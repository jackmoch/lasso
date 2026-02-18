(ns lasso.routes
  "HTTP routes for Lasso application."
  (:require [io.pedestal.http.route :as route]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [lasso.auth.handlers :as auth-handlers]
            [lasso.session.handlers :as session-handlers]
            [lasso.admin.handlers :as admin-handlers]
            [lasso.middleware :as mw]
            [lasso.middleware.security :as security]
            [lasso.middleware.admin :as admin-mw]))

(defn home-page
  "Serve the main application page."
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "public/index.html"))})

(defn serve-css
  "Serve CSS files from classpath resources."
  [request]
  (let [path (get-in request [:path-params :path])
        resource-path (str "public/css/" path)]
    (if-let [resource (io/resource resource-path)]
      {:status 200
       :headers {"Content-Type" "text/css"}
       :body (slurp resource)}
      {:status 404
       :body "Not found"})))

(defn serve-js
  "Serve JavaScript files from classpath resources."
  [request]
  (let [path (get-in request [:path-params :path])
        resource-path (str "public/js/" path)]
    (if-let [resource (io/resource resource-path)]
      {:status 200
       :headers {"Content-Type" "application/javascript"}
       :body (slurp resource)}
      {:status 404
       :body "Not found"})))

(defn health-check
  "Health check endpoint for container orchestration."
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:status "ok"})})

(def routes
  "Application route definitions.
   Security interceptors are applied globally in server configuration."
  (route/expand-routes
   #{["/" :get home-page :route-name :home]
     ["/health" :get health-check :route-name :health]

     ;; Static assets
     ["/css/*path" :get serve-css :route-name :serve-css]
     ["/js/*path" :get serve-js :route-name :serve-js]

     ;; Authentication routes — auth-rate-limit-interceptor is applied here (not
     ;; globally) because these endpoints make upstream Last.fm API calls and need
     ;; a tighter ceiling than the global 100/min. See AUTH_RATE_LIMIT_MAX_REQUESTS.
     ["/api/auth/init" :post [security/auth-rate-limit-interceptor auth-handlers/auth-init-handler] :route-name :auth-init]
     ["/api/auth/callback" :get [security/auth-rate-limit-interceptor auth-handlers/auth-callback-handler] :route-name :auth-callback]
     ["/api/auth/logout" :post [mw/require-auth auth-handlers/logout-handler] :route-name :auth-logout]

     ;; Session management routes (all require authentication)
     ["/api/session/start" :post [mw/require-auth session-handlers/start-session-handler] :route-name :session-start]
     ["/api/session/pause" :post [mw/require-auth session-handlers/pause-session-handler] :route-name :session-pause]
     ["/api/session/resume" :post [mw/require-auth session-handlers/resume-session-handler] :route-name :session-resume]
     ["/api/session/stop" :post [mw/require-auth session-handlers/stop-session-handler] :route-name :session-stop]
     ["/api/session/status" :get [mw/require-auth session-handlers/status-handler] :route-name :session-status]

     ;; Admin routes — use separate admin-session cookie, not user session
     ["/api/admin/login" :post admin-handlers/login-handler :route-name :admin-login]
     ["/api/admin/logout" :post [admin-mw/require-admin-auth admin-handlers/logout-handler] :route-name :admin-logout]
     ["/api/admin/status" :get [admin-mw/require-admin-auth admin-handlers/status-handler] :route-name :admin-status]
     ["/api/admin/sessions/:session-id" :delete [admin-mw/require-admin-auth admin-handlers/force-stop-handler] :route-name :admin-force-stop]}))
