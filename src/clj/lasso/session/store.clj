(ns lasso.session.store
  "In-memory session storage with atomic operations."
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [lasso.config :as config]
            [taoensso.timbre :as log]))

(defonce sessions
  ;; Atom storing all user sessions. Map of session-id -> session data.
  (atom {}))

(defn create-session
  "Create a new session and store it.
   Returns the session data."
  [session-id username session-key]
  (let [now (System/currentTimeMillis)
        session {:session-id session-id
                 :username username
                 :session-key session-key
                 :created-at now
                 :last-activity now
                 :following-session nil}]
    (swap! sessions assoc session-id session)
    session))

(defn session-expired?
  "Check whether a session has exceeded its TTL based on last-activity.
   Uses SESSION_TTL_MS config (default: 86400000ms = 24h)."
  [session]
  (let [ttl-ms (get-in config/config [:session :ttl-ms])
        last-activity (:last-activity session)]
    (> (System/currentTimeMillis) (+ last-activity ttl-ms))))

(defn get-session
  "Retrieve a session by ID. Returns nil if not found or expired.
   Expired sessions are lazily removed from the store on access."
  [session-id]
  (when-let [session (get @sessions session-id)]
    (if (session-expired? session)
      (do
        (swap! sessions dissoc session-id)
        (log/info "Removed expired session on access" {:session-id session-id})
        nil)
      session)))

(defn update-session
  "Update a session atomically using a function.
   The function receives the current session data and returns updated data.
   Returns the updated session."
  [session-id update-fn]
  (let [result (atom nil)]
    (swap! sessions
           (fn [sessions-map]
             (if-let [session (get sessions-map session-id)]
               (let [updated (update-fn session)]
                 (reset! result updated)
                 (assoc sessions-map session-id updated))
               (do
                 (reset! result nil)
                 sessions-map))))
    @result))

(defn delete-session
  "Remove a session from storage."
  [session-id]
  (swap! sessions dissoc session-id)
  nil)

(defn touch-session
  "Update the last-activity timestamp for a session."
  [session-id]
  (update-session session-id
                  (fn [session]
                    (assoc session :last-activity (System/currentTimeMillis)))))

(defn get-active-following-sessions
  "Query all sessions that have an active following session.
   Returns a sequence of session maps."
  []
  (->> @sessions
       vals
       (filter (fn [session]
                 (and (:following-session session)
                      (= :active (get-in session [:following-session :state])))))))

(defn count-sessions
  "Return the total number of active sessions."
  []
  (count @sessions))

(defn cleanup-expired-sessions!
  "Sweep the store and remove all expired sessions.
   Returns the number of sessions removed."
  []
  (let [before (count @sessions)]
    (swap! sessions
           (fn [sessions-map]
             (into {} (remove (fn [[_ session]] (session-expired? session))
                              sessions-map))))
    (let [removed (- before (count @sessions))]
      (when (pos? removed)
        (log/info "Cleaned up expired sessions" {:removed removed}))
      removed)))

(defn clear-all-sessions!
  "Clear all sessions. Used for testing and development."
  []
  (reset! sessions {}))

;; =============================================================================
;; Background Cleanup Scheduler
;; =============================================================================

(defonce cleanup-control-chan (atom nil))

(defn start-cleanup-scheduler!
  "Start a background job that periodically removes expired sessions.
   Interval controlled by SESSION_CLEANUP_INTERVAL_MS (default: 1h).
   Safe to call multiple times — no-ops if already running."
  []
  (when-not @cleanup-control-chan
    (let [interval-ms (get-in config/config [:session :cleanup-interval-ms])
          control-chan (async/chan)]
      (reset! cleanup-control-chan control-chan)
      (log/info "Starting session cleanup scheduler" {:interval-ms interval-ms})
      (async/go-loop []
        (let [timeout-chan (async/timeout interval-ms)
              [_ port] (async/alts! [control-chan timeout-chan])]
          (when (= port timeout-chan)
            (cleanup-expired-sessions!)
            (recur)))))))

(defn stop-cleanup-scheduler!
  "Stop the background session cleanup job."
  []
  (when-let [chan @cleanup-control-chan]
    (async/close! chan)
    (reset! cleanup-control-chan nil)
    (log/info "Session cleanup scheduler stopped")))
