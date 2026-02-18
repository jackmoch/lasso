(ns lasso.admin.session
  "Admin session store — separate from user OAuth sessions."
  (:require [lasso.config :as config]
            [lasso.util.crypto :as crypto]
            [taoensso.timbre :as log]))

(defonce admin-sessions
  ;; Atom storing admin sessions. Map of session-id -> session data.
  ;; Intentionally separate from user session store.
  (atom {}))

(defn admin-session-expired?
  "Check whether an admin session has exceeded its TTL based on last-activity."
  [session]
  (let [ttl-ms (get-in config/config [:admin :session-ttl-ms])
        last-activity (:last-activity session)]
    (> (System/currentTimeMillis) (+ last-activity ttl-ms))))

(defn create-admin-session
  "Create a new admin session. Returns the session map including :session-id."
  []
  (let [session-id (crypto/generate-uuid)
        now (System/currentTimeMillis)
        session {:session-id session-id
                 :created-at now
                 :last-activity now}]
    (swap! admin-sessions assoc session-id session)
    (log/info "Admin session created" {:session-id session-id})
    session))

(defn get-admin-session
  "Retrieve an admin session by ID. Returns nil if not found or expired.
   Expired sessions are lazily removed on access."
  [session-id]
  (when-let [session (get @admin-sessions session-id)]
    (if (admin-session-expired? session)
      (do
        (swap! admin-sessions dissoc session-id)
        (log/info "Removed expired admin session" {:session-id session-id})
        nil)
      session)))

(defn touch-admin-session
  "Update last-activity timestamp for an admin session."
  [session-id]
  (swap! admin-sessions update session-id
         (fn [session]
           (when session
             (assoc session :last-activity (System/currentTimeMillis))))))

(defn destroy-admin-session
  "Remove an admin session from storage."
  [session-id]
  (swap! admin-sessions dissoc session-id)
  (log/info "Admin session destroyed" {:session-id session-id})
  nil)

(defn clear-all-admin-sessions!
  "Clear all admin sessions. Used for testing."
  []
  (reset! admin-sessions {}))
