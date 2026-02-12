(ns lasso.auth.handlers
  "HTTP handlers for OAuth authentication flow."
  (:require [lasso.lastfm.oauth :as oauth]
            [lasso.auth.session :as auth-session]
            [lasso.util.http :as http]
            [taoensso.timbre :as log]))

(defn auth-init-handler
  "POST /api/auth/init
   Initiates the Last.fm OAuth flow by generating an auth URL.
   Returns: {:auth_url 'https://last.fm/...'}"
  [_request]
  (try
    (let [token-result (oauth/get-token)]
      (if (:token token-result)
        (let [auth-url (oauth/generate-auth-url (:token token-result))]
          (http/json-response {:auth_url auth-url}))
        (do
          (log/error "Failed to get OAuth token" token-result)
          (http/error-response "Failed to initiate authentication"
                               :status 500
                               :error-code "OAUTH_TOKEN_FAILED"
                               :details (:error token-result)))))
    (catch Exception e
      (log/error e "Error in auth-init-handler")
      (http/error-response "Authentication initialization failed"
                           :status 500
                           :error-code "OAUTH_INIT_ERROR"))))

(defn auth-callback-handler
  "GET /api/auth/callback?token=xxx
   Completes the OAuth flow by exchanging the authorized token for a session key.
   Creates a server-side session and returns a session cookie.
   Returns: {:username 'user123'} with Set-Cookie header"
  [request]
  (try
    (let [token (get-in request [:params :token])]
      (if-not token
        (http/error-response "Missing token parameter"
                            :status 400
                            :error-code "MISSING_TOKEN")
        (let [session-result (oauth/get-session-key token)]
          (if-let [session-data (:session session-result)]
            (let [username (:name session-data)
                  session-key (:key session-data)
                  {:keys [session-id]} (auth-session/create-session username session-key)]
              (log/info "User authenticated successfully" {:username username})
              (http/json-response {:username username}
                                 :cookies {"session-id" session-id}))
            (do
              (log/error "Failed to get session key" session-result)
              (http/error-response "Authentication failed"
                                  :status 401
                                  :error-code "OAUTH_SESSION_FAILED"
                                  :details (:error session-result)))))))
    (catch Exception e
      (log/error e "Error in auth-callback-handler")
      (http/error-response "Authentication callback failed"
                           :status 500
                           :error-code "OAUTH_CALLBACK_ERROR"))))

(defn logout-handler
  "POST /api/auth/logout
   Destroys the user's session and clears the session cookie.
   Requires authentication (session-id cookie).
   Returns: {:success true}"
  [request]
  (try
    (let [session-id (get-in request [:session :session-id])]
      (auth-session/destroy-session session-id)
      (log/info "User logged out" {:session-id session-id})
      ;; Clear cookie by setting max-age to 0
      {:status 200
       :headers {"Content-Type" "application/json"
                 "Set-Cookie" (http/cookie-string "session-id" "" :max-age 0)}
       :body "{\"success\":true}"})
    (catch Exception e
      (log/error e "Error in logout-handler")
      (http/error-response "Logout failed"
                           :status 500
                           :error-code "LOGOUT_ERROR"))))