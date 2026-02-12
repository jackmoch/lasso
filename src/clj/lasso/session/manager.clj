(ns lasso.session.manager
  "Session lifecycle management and state transitions."
  (:require [lasso.session.store :as store]
            [lasso.lastfm.client :as lastfm]
            [lasso.validation.schemas :as schemas]
            [lasso.polling.scheduler :as scheduler]
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
         :error (or (:error result) "User not found")}))
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
   Returns {:success true :session ...} or {:success false :error ...}"
  [session-id target-username]
  (log/info "Starting following session" {:session-id session-id
                                          :target-username target-username})

  ;; Validate target user exists
  (let [validation (validate-target-user target-username)]
    (if (:valid? validation)
      (let [now (System/currentTimeMillis)
            following-session {:target-username (:username validation)
                              :state :active
                              :started-at now
                              :scrobble-count 0
                              :scrobble-cache #{}}
            updated (store/update-session
                     session-id
                     (fn [session]
                       (if (:following-session session)
                         ;; Session already exists - don't overwrite
                         session
                         (assoc session :following-session following-session))))]
        (if updated
          (do
            ;; Start polling for this session
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
          ;; Stop polling when paused
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
          ;; Resume polling when resumed
          (scheduler/handle-session-state-change session-id :active)
          {:success true :session updated})))
    {:success false :error "Session not found"}))

(defn stop-session
  "Stop and clear a following session.
   Returns {:success true :session ...} or {:success false :error ...}"
  [session-id]
  (log/info "Stopping following session" {:session-id session-id})

  (let [updated (store/update-session
                 session-id
                 (fn [session]
                   (if (:following-session session)
                     (assoc session :following-session nil)
                     ;; No following session to stop
                     session)))]
    (if updated
      (do
        ;; Stop polling when session stopped
        (scheduler/handle-session-state-change session-id :stopped)
        {:success true :session updated})
      {:success false :error "Session not found"})))

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
                   :recent_scrobbles [] ; TODO: Implement when polling is added
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

(defn can-start-session?
  "Check if a new following session can be started.
   Returns true if no active session exists."
  [session-id]
  (if-let [session (store/get-session session-id)]
    (nil? (:following-session session))
    false))
