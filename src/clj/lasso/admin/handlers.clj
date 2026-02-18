(ns lasso.admin.handlers
  "HTTP handlers for admin API endpoints."
  (:require [lasso.admin.session :as admin-session]
            [lasso.config :as config]
            [lasso.session.store :as store]
            [lasso.polling.scheduler :as scheduler]
            [lasso.util.http :as http]
            [clojure.data.json :as json]
            [taoensso.timbre :as log])
  (:import [java.security MessageDigest]))

;; =============================================================================
;; Helpers
;; =============================================================================

(defn- constant-time-equals?
  "Compare two strings in constant time to prevent timing attacks."
  [a b]
  (MessageDigest/isEqual
   (.getBytes (str a) "UTF-8")
   (.getBytes (str b) "UTF-8")))

(defn- parse-body
  "Parse JSON request body from various formats (InputStream, String, or map)."
  [request]
  (let [body (:body request)]
    (cond
      (:json-params request) (:json-params request)
      (:body-params request) (:body-params request)
      (instance? java.io.InputStream body) (json/read-str (slurp body) :key-fn keyword)
      (string? body) (json/read-str body :key-fn keyword)
      (map? body) body
      :else {})))

(defn- session->api-map
  "Convert internal session map to safe API response shape.
   Excludes session-key and other sensitive fields."
  [session]
  (let [now (System/currentTimeMillis)
        following (:following-session session)
        state (if following
                (name (:state following))
                "idle")]
    (cond-> {:session_id (:session-id session)
             :username (:username session)
             :state state
             :session_age_ms (- now (:created-at session))
             :created_at (:created-at session)}
      following (assoc :target_username (:target-username following)
                       :scrobble_count (:scrobble-count following)
                       :started_at (:started-at following)
                       :last_poll (:last-poll following)))))

(defn- compute-metrics
  "Derive metrics map from current sessions atom and active pollers."
  [all-sessions active-pollers-count]
  (let [total (count all-sessions)
        active (count (filter #(= "active" (:state %)) all-sessions))
        paused (count (filter #(= "paused" (:state %)) all-sessions))
        idle (count (filter #(= "idle" (:state %)) all-sessions))]
    {:total_sessions total
     :active_sessions active
     :paused_sessions paused
     :idle_sessions idle
     :active_pollers active-pollers-count}))

;; =============================================================================
;; Handlers
;; =============================================================================

(defn login-handler
  "POST /api/admin/login
   Validates credentials and creates an admin session.
   Returns 200 with {success: true} and Set-Cookie on success.
   Returns 401 with error on invalid credentials."
  [request]
  (try
    (let [data (parse-body request)
          username (:username data)
          password (:password data)
          admin-username (get-in config/config [:admin :username])
          admin-password (get-in config/config [:admin :password])
          ttl-ms (get-in config/config [:admin :session-ttl-ms])
          is-production? (= :production (:environment config/config))]

      (if (and username password
               admin-username admin-password
               (constant-time-equals? username admin-username)
               (constant-time-equals? password admin-password))
        ;; Valid credentials — create session and set cookie
        (let [{:keys [session-id]} (admin-session/create-admin-session)]
          (log/info "Admin login successful")
          {:status 200
           :headers {"Content-Type" "application/json"
                     "Set-Cookie" (http/cookie-string "admin-session" session-id
                                                      :max-age (quot ttl-ms 1000)
                                                      :path "/"
                                                      :http-only true
                                                      :secure is-production?
                                                      :same-site "Strict")}
           :body (json/write-str {:success true})})

        ;; Invalid credentials
        (do
          (log/warn "Admin login failed — invalid credentials")
          (http/error-response "Invalid credentials"
                               :status 401
                               :error-code "ADMIN_AUTH_FAILED"))))
    (catch Exception e
      (log/error e "Error in admin login-handler")
      (http/error-response "Login failed"
                           :status 500
                           :error-code "ADMIN_LOGIN_ERROR"))))

(defn logout-handler
  "POST /api/admin/logout
   Destroys the admin session and clears the cookie."
  [request]
  (try
    (let [session-id (get-in request [:admin-session :session-id])]
      (when session-id
        (admin-session/destroy-admin-session session-id))
      (log/info "Admin logged out")
      {:status 200
       :headers {"Content-Type" "application/json"
                 "Set-Cookie" (http/cookie-string "admin-session" ""
                                                  :max-age 0
                                                  :path "/")}
       :body (json/write-str {:success true})})
    (catch Exception e
      (log/error e "Error in admin logout-handler")
      (http/error-response "Logout failed"
                           :status 500
                           :error-code "ADMIN_LOGOUT_ERROR"))))

(defn status-handler
  "GET /api/admin/status
   Returns system snapshot: metrics and all sessions (no session keys)."
  [_request]
  (try
    (let [all-sessions (->> (vals @store/sessions)
                            (map session->api-map)
                            (sort-by :created_at >)
                            vec)
          active-pollers-count (count @scheduler/active-pollers)
          metrics (compute-metrics all-sessions active-pollers-count)]
      (http/json-response {:metrics metrics
                           :sessions all-sessions}))
    (catch Exception e
      (log/error e "Error in admin status-handler")
      (http/error-response "Failed to retrieve status"
                           :status 500
                           :error-code "ADMIN_STATUS_ERROR"))))

(defn force-stop-handler
  "DELETE /api/admin/sessions/:session-id
   Force-stops a user session: stops the polling loop then deletes the session."
  [request]
  (try
    (let [target-session-id (get-in request [:path-params :session-id])]
      (if (store/get-session target-session-id)
        (do
          ;; Stop poller first, then delete session
          (scheduler/stop-poller target-session-id)
          (store/delete-session target-session-id)
          (log/info "Admin force-stopped session" {:session-id target-session-id})
          (http/json-response {:success true
                               :session_id target-session-id}))
        (http/error-response "Session not found"
                             :status 404
                             :error-code "SESSION_NOT_FOUND")))
    (catch Exception e
      (log/error e "Error in admin force-stop-handler")
      (http/error-response "Failed to stop session"
                           :status 500
                           :error-code "ADMIN_STOP_ERROR"))))
