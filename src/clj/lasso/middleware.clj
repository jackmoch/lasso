(ns lasso.middleware
  "Pedestal interceptors for authentication and request processing."
  (:require [io.pedestal.interceptor :as interceptor]
            [lasso.auth.session :as auth-session]
            [lasso.util.http :as http]
            [lasso.session.store :as store]))

(def require-auth
  "Interceptor that requires a valid session cookie.
   Validates the session exists and is active, then attaches session data to context.
   Returns 401 Unauthorized if:
   - No session cookie present
   - Session ID is invalid or expired
   - Session not found in store"
  (interceptor/interceptor
   {:name ::require-auth
    :enter (fn [context]
             (let [request (:request context)
                   session-id (http/parse-cookie request "session-id")]
               (if session-id
                 (if-let [session (store/get-session session-id)]
                   ;; Session found - attach to context and update last activity
                   (do
                     (auth-session/touch session-id)
                     (assoc context :session session))
                   ;; Session not found
                   (assoc context :response
                          (http/error-response "Session not found or expired"
                                               :status 401
                                               :error-code "SESSION_EXPIRED")))
                 ;; No session cookie
                 (assoc context :response
                        (http/error-response "Authentication required"
                                             :status 401
                                             :error-code "AUTH_REQUIRED")))))}))

(defn get-session
  "Extract session data from context (attached by require-auth interceptor).
   Returns nil if no session present."
  [context]
  (:session context))

(defn get-session-id
  "Extract session ID from context session data.
   Returns nil if no session present."
  [context]
  (when-let [session (get-session context)]
    (:session-id session)))