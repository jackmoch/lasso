(ns lasso.session.manager
  "Session lifecycle management and state transitions."
  (:require [lasso.session.store :as store]
            [lasso.lastfm.client :as lastfm]
            [lasso.polling.scheduler :as scheduler]
            [lasso.polling.engine :as engine]
            [lasso.user.store :as user-store]
            [lasso.util.crypto :as crypto]
            [lasso.config :as config]
            [taoensso.timbre :as log]))

(defn validate-target-user
  "Verify that the target username exists and is accessible on Last.fm.
   Returns {:valid? true :username ...} or {:valid? false :error ...}"
  [target-username]
  (try
    (let [result (lastfm/api-request {:method "user.getInfo"
                                      :params {:user target-username}})]
      (if-let [user-info (:user result)]
        {:valid? true
         :username (:name user-info)}
        {:valid? false
         :error (or (:message result) (str (:error result)) "User not found")}))
    (catch Exception e
      (log/error e "Error validating target user" {:username target-username})
      {:valid? false
       :error "Failed to validate user"})))

(defn can-start-session?
  "Check if a new following session can be started.
   Returns true if no active session exists."
  [session-id]
  (if-let [session (store/get-session session-id)]
    (nil? (:following-session session))
    false))

(defn start-session
  "Start a new following session for the given session-id.
   Validates the target user exists before creating the session.
   Writes a session record to Firestore (no-op if unavailable).
   Returns {:success true :session ...} or {:success false :error ...}"
  [session-id target-username]
  (log/info "Starting following session" {:session-id session-id
                                          :target-username target-username})

  (let [validation (validate-target-user target-username)]
    (if (:valid? validation)
      (let [now              (System/currentTimeMillis)
            fs-session-id    (crypto/generate-uuid)
            following-session {:target-username (:username validation)
                               :state           :active
                               :started-at      now
                               :scrobble-count  0
                               :scrobble-cache  #{}
                               :recent-scrobbles []
                               :fs-session-id   fs-session-id}
            updated (store/update-session
                     session-id
                     (fn [session]
                       (if (:following-session session)
                         session
                         (assoc session :following-session following-session))))]
        (if updated
          (let [username (:username updated)]
            ;; Log session start in Firestore
            (user-store/add-session-record username fs-session-id
                                           (:username validation) now)
            (user-store/update-last-target username (:username validation))
            ;; Start polling
            (scheduler/handle-session-state-change session-id :active)
            {:success true :session updated})
          {:success false :error "Session not found"}))
      {:success false :error (:error validation)})))

(defn pause-session
  "Pause an active following session.
   Returns {:success true :session ...} or {:success false :error ...}"
  [session-id]
  (log/info "Pausing following session" {:session-id session-id})

  (if-let [current-session (store/get-session session-id)]
    (let [following (:following-session current-session)]
      (cond
        (nil? following)
        {:success false :error "Session is not active"}

        (not= :active (:state following))
        {:success false :error "Session is not active"}

        :else
        (let [updated (store/update-session
                       session-id
                       (fn [session]
                         (assoc-in session [:following-session :state] :paused)))]
          (scheduler/handle-session-state-change session-id :paused)
          {:success true :session updated})))
    {:success false :error "Session not found"}))

(defn resume-session
  "Resume a paused following session.
   Returns {:success true :session ...} or {:success false :error ...}"
  [session-id]
  (log/info "Resuming following session" {:session-id session-id})

  (if-let [current-session (store/get-session session-id)]
    (let [following (:following-session current-session)]
      (cond
        (nil? following)
        {:success false :error "Session is not paused"}

        (not= :paused (:state following))
        {:success false :error "Session is not paused"}

        :else
        (let [updated (store/update-session
                       session-id
                       (fn [session]
                         (assoc-in session [:following-session :state] :active)))]
          (scheduler/handle-session-state-change session-id :active)
          {:success true :session updated})))
    {:success false :error "Session not found"}))

(defn stop-session
  "Stop and clear a following session.
   Writes the final session record to Firestore (no-op if unavailable).
   Returns {:success true :session ...} or {:success false :error ...}"
  [session-id]
  (log/info "Stopping following session" {:session-id session-id})

  ;; Capture following-session data before clearing it
  (let [current (store/get-session session-id)
        following (when current (:following-session current))
        updated (store/update-session
                 session-id
                 (fn [session]
                   (if (:following-session session)
                     (assoc session :following-session nil)
                     session)))]
    (if updated
      (do
        ;; Finish Firestore record with final scrobble count
        (when following
          (user-store/finish-session-record
           (:username current)
           (:fs-session-id following)
           (or (:scrobble-count following) 0)
           "stopped"))
        (scheduler/handle-session-state-change session-id :stopped)
        {:success true :session updated})
      {:success false :error "Session not found"})))

(defn- restore-following-session!
  "Rebuild in-memory following session state from a Firestore session record and
   restart the polling loop. Called by maybe-restore-session! when a valid active
   session is found in Firestore after server restart."
  [session-id username _session-key fs-session]
  (let [target (:target_username fs-session)
        cache  (engine/rebuild-scrobble-cache target (config/get-env "LASTFM_API_KEY"))]
    (store/update-session
     session-id
     (fn [session]
       (assoc session :following-session
              {:target-username  (:target_username fs-session)
               :state            :active
               :started-at       (:started_at fs-session)
               :last-poll        nil
               :scrobble-count   (or (:scrobble_count fs-session) 0)
               :scrobble-cache   cache
               :recent-scrobbles []
               :fs-session-id    (:id fs-session)})))
    (scheduler/start-poller session-id)
    (log/info "Restored following session" {:username username :target target})))

(defn maybe-restore-session!
  "Check Firestore for an active following session and restore it if one exists
   that is less than 24 hours old and the in-memory session is not already active.
   No-op when Firestore is unavailable, session is already active, or no valid
   record exists. Safe to call on every login."
  [session-id username session-key]
  (let [mem-state (get-in (store/get-session session-id) [:following-session :state])]
    (when (not= :active mem-state)
      (when-let [fs-session (user-store/get-latest-active-session username)]
        (let [age-ms (- (System/currentTimeMillis) (or (:started_at fs-session) 0))]
          (when (< age-ms (* 24 60 60 1000))
            (restore-following-session! session-id username session-key fs-session)))))))

(defn get-session-status
  "Get the current status of a session.
   Returns status map with session state and recent activity."
  [session-id]
  (if-let [session (store/get-session session-id)]
    (let [following (:following-session session)]
      {:authenticated true
       :username (:username session)
       :session (if following
                  {:state (:state following)
                   :target_username (:target-username following)
                   :scrobble_count (or (:scrobble-count following) 0)
                   :recent_scrobbles (or (:recent-scrobbles following) [])
                   :started_at (:started-at following)
                   :last_poll (:last-poll following)}
                  {:state :not-started
                   :target_username nil
                   :scrobble_count 0
                   :recent_scrobbles []
                   :started_at nil
                   :last_poll nil})})
    {:authenticated false
     :username nil
     :session nil}))
