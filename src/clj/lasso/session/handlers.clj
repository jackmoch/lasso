(ns lasso.session.handlers
  "HTTP handlers for session management endpoints."
  (:require [lasso.session.manager :as manager]
            [lasso.middleware :as mw]
            [lasso.util.http :as http]
            [lasso.validation.schemas :as schemas]
            [clojure.data.json :as json]
            [taoensso.timbre :as log]))

(defn start-session-handler
  "POST /api/session/start
   Start following a target Last.fm user.
   Requires: {:target_username 'username'}
   Returns: {:state 'active', :target_username '...', :scrobble_count 0}"
  [request]
  (try
    (let [_ (log/info "DEBUG start-session-handler request keys:" (keys request))
          _ (log/info "DEBUG request :session:" (:session request))
          session-id (mw/get-session-id request)
          _ (log/info "DEBUG session-id:" session-id)
          body (:body request)
          request-data (cond
                         ;; Already parsed by body-params interceptor
                         (:json-params request) (:json-params request)
                         (:body-params request) (:body-params request)
                         ;; Body is a string
                         (string? body) (json/read-str body :key-fn keyword)
                         ;; Body is an InputStream
                         (instance? java.io.InputStream body)
                         (json/read-str (slurp body) :key-fn keyword)
                         ;; Already a map
                         (map? body) body
                         ;; Default empty map
                         :else {})
          target-username (:target_username request-data)]

      (if-not target-username
        (http/error-response "Missing target_username"
                            :status 400
                            :error-code "MISSING_TARGET_USERNAME")

        ;; Validate request
        (let [validation (schemas/validate schemas/StartSessionRequest request-data)]
          (if-not (:valid? validation)
            (http/error-response "Invalid request"
                                :status 400
                                :error-code "INVALID_REQUEST"
                                :details (:errors validation))

            ;; Start session
            (let [result (manager/start-session session-id target-username)]
              (if (:success result)
                (let [following (get-in result [:session :following-session])]
                  (log/info "Following session started" {:session-id session-id
                                                         :target-username target-username})
                  (http/json-response
                   {:state (name (:state following))
                    :target_username (:target-username following)
                    :scrobble_count (:scrobble-count following)}))
                (http/error-response (:error result)
                                    :status 400
                                    :error-code "START_SESSION_FAILED")))))))
    (catch Exception e
      (log/error e "Error in start-session-handler")
      (http/error-response "Failed to start session"
                           :status 500
                           :error-code "START_SESSION_ERROR"))))

(defn pause-session-handler
  "POST /api/session/pause
   Pause the active following session.
   Returns: {:state 'paused', :target_username '...', :scrobble_count N}"
  [request]
  (try
    (let [session-id (mw/get-session-id request)
          result (manager/pause-session session-id)]

      (if (:success result)
        (let [following (get-in result [:session :following-session])]
          (log/info "Following session paused" {:session-id session-id})
          (http/json-response
           {:state (name (:state following))
            :target_username (:target-username following)
            :scrobble_count (:scrobble-count following)}))
        (http/error-response (:error result)
                            :status 400
                            :error-code "PAUSE_SESSION_FAILED")))
    (catch Exception e
      (log/error e "Error in pause-session-handler")
      (http/error-response "Failed to pause session"
                           :status 500
                           :error-code "PAUSE_SESSION_ERROR"))))

(defn resume-session-handler
  "POST /api/session/resume
   Resume a paused following session.
   Returns: {:state 'active', :target_username '...', :scrobble_count N}"
  [request]
  (try
    (let [session-id (mw/get-session-id request)
          result (manager/resume-session session-id)]

      (if (:success result)
        (let [following (get-in result [:session :following-session])]
          (log/info "Following session resumed" {:session-id session-id})
          (http/json-response
           {:state (name (:state following))
            :target_username (:target-username following)
            :scrobble_count (:scrobble-count following)}))
        (http/error-response (:error result)
                            :status 400
                            :error-code "RESUME_SESSION_FAILED")))
    (catch Exception e
      (log/error e "Error in resume-session-handler")
      (http/error-response "Failed to resume session"
                           :status 500
                           :error-code "RESUME_SESSION_ERROR"))))

(defn stop-session-handler
  "POST /api/session/stop
   Stop and clear the following session.
   Returns: {:success true}"
  [request]
  (try
    (let [session-id (mw/get-session-id request)
          result (manager/stop-session session-id)]

      (if (:success result)
        (do
          (log/info "Following session stopped" {:session-id session-id})
          (http/json-response {:success true}))
        (http/error-response (:error result)
                            :status 400
                            :error-code "STOP_SESSION_FAILED")))
    (catch Exception e
      (log/error e "Error in stop-session-handler")
      (http/error-response "Failed to stop session"
                           :status 500
                           :error-code "STOP_SESSION_ERROR"))))

(defn status-handler
  "GET /api/session/status
   Get current session status and recent activity.
   Returns: {:authenticated true/false, :username '...', :session {...}}"
  [request]
  (try
    (let [session-id (mw/get-session-id request)
          status (manager/get-session-status session-id)
          ;; Convert :state keyword to string for JSON
          status-with-strings (if-let [state (get-in status [:session :state])]
                                (assoc-in status [:session :state] (name state))
                                status)]

      (http/json-response status-with-strings))
    (catch Exception e
      (log/error e "Error in status-handler")
      (http/error-response "Failed to get session status"
                           :status 500
                           :error-code "STATUS_ERROR"))))
