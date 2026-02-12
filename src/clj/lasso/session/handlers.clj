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
  [context]
  (try
    (let [session-id (mw/get-session-id context)
          body (get-in context [:request :body])
          request-data (if (string? body)
                        (json/read-str body :key-fn keyword)
                        body)
          target-username (:target_username request-data)]

      (if-not target-username
        {:response (http/error-response "Missing target_username"
                                        :status 400
                                        :error-code "MISSING_TARGET_USERNAME")}

        ;; Validate request
        (let [validation (schemas/validate schemas/StartSessionRequest request-data)]
          (if-not (:valid? validation)
            {:response (http/error-response "Invalid request"
                                            :status 400
                                            :error-code "INVALID_REQUEST"
                                            :details (:errors validation))}

            ;; Start session
            (let [result (manager/start-session session-id target-username)]
              (if (:success result)
                (let [following (get-in result [:session :following-session])]
                  (log/info "Following session started" {:session-id session-id
                                                         :target-username target-username})
                  {:response (http/json-response
                              {:state (name (:state following))
                               :target_username (:target-username following)
                               :scrobble_count (:scrobble-count following)})})
                {:response (http/error-response (:error result)
                                                :status 400
                                                :error-code "START_SESSION_FAILED")}))))))
    (catch Exception e
      (log/error e "Error in start-session-handler")
      {:response (http/error-response "Failed to start session"
                                      :status 500
                                      :error-code "START_SESSION_ERROR")})))

(defn pause-session-handler
  "POST /api/session/pause
   Pause the active following session.
   Returns: {:state 'paused', :target_username '...', :scrobble_count N}"
  [context]
  (try
    (let [session-id (mw/get-session-id context)
          result (manager/pause-session session-id)]

      (if (:success result)
        (let [following (get-in result [:session :following-session])]
          (log/info "Following session paused" {:session-id session-id})
          {:response (http/json-response
                      {:state (name (:state following))
                       :target_username (:target-username following)
                       :scrobble_count (:scrobble-count following)})})
        {:response (http/error-response (:error result)
                                        :status 400
                                        :error-code "PAUSE_SESSION_FAILED")}))
    (catch Exception e
      (log/error e "Error in pause-session-handler")
      {:response (http/error-response "Failed to pause session"
                                      :status 500
                                      :error-code "PAUSE_SESSION_ERROR")})))

(defn resume-session-handler
  "POST /api/session/resume
   Resume a paused following session.
   Returns: {:state 'active', :target_username '...', :scrobble_count N}"
  [context]
  (try
    (let [session-id (mw/get-session-id context)
          result (manager/resume-session session-id)]

      (if (:success result)
        (let [following (get-in result [:session :following-session])]
          (log/info "Following session resumed" {:session-id session-id})
          {:response (http/json-response
                      {:state (name (:state following))
                       :target_username (:target-username following)
                       :scrobble_count (:scrobble-count following)})})
        {:response (http/error-response (:error result)
                                        :status 400
                                        :error-code "RESUME_SESSION_FAILED")}))
    (catch Exception e
      (log/error e "Error in resume-session-handler")
      {:response (http/error-response "Failed to resume session"
                                      :status 500
                                      :error-code "RESUME_SESSION_ERROR")})))

(defn stop-session-handler
  "POST /api/session/stop
   Stop and clear the following session.
   Returns: {:success true}"
  [context]
  (try
    (let [session-id (mw/get-session-id context)
          result (manager/stop-session session-id)]

      (if (:success result)
        (do
          (log/info "Following session stopped" {:session-id session-id})
          {:response (http/json-response {:success true})})
        {:response (http/error-response (:error result)
                                        :status 400
                                        :error-code "STOP_SESSION_FAILED")}))
    (catch Exception e
      (log/error e "Error in stop-session-handler")
      {:response (http/error-response "Failed to stop session"
                                      :status 500
                                      :error-code "STOP_SESSION_ERROR")})))

(defn status-handler
  "GET /api/session/status
   Get current session status and recent activity.
   Returns: {:authenticated true/false, :username '...', :session {...}}"
  [context]
  (try
    (let [session-id (mw/get-session-id context)
          status (manager/get-session-status session-id)
          ;; Convert :state keyword to string for JSON
          status-with-strings (if-let [state (get-in status [:session :state])]
                                (assoc-in status [:session :state] (name state))
                                status)]

      {:response (http/json-response status-with-strings)})
    (catch Exception e
      (log/error e "Error in status-handler")
      {:response (http/error-response "Failed to get session status"
                                      :status 500
                                      :error-code "STATUS_ERROR")})))
