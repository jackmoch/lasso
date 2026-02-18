(ns lasso.middleware.admin
  "Admin authentication interceptor for Pedestal."
  (:require [io.pedestal.interceptor :as interceptor]
            [lasso.admin.session :as admin-session]
            [lasso.util.http :as http]))

(def require-admin-auth
  "Interceptor that requires a valid admin-session cookie.
   Attaches admin session data to [:request :admin-session] on success.
   Returns 401 if cookie is missing or session is invalid/expired."
  (interceptor/interceptor
   {:name ::require-admin-auth
    :enter (fn [context]
             (let [request (:request context)
                   session-id (http/parse-cookie request "admin-session")]
               (if session-id
                 (if-let [session (admin-session/get-admin-session session-id)]
                   (do
                     (admin-session/touch-admin-session session-id)
                     (assoc-in context [:request :admin-session] session))
                   (assoc context :response
                          (http/error-response "Admin session not found or expired"
                                               :status 401
                                               :error-code "ADMIN_SESSION_EXPIRED")))
                 (assoc context :response
                        (http/error-response "Admin authentication required"
                                             :status 401
                                             :error-code "ADMIN_AUTH_REQUIRED")))))}))
