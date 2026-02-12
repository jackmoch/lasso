(ns lasso.routes
  "HTTP routes for Lasso application."
  (:require [io.pedestal.http.route :as route]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [lasso.auth.handlers :as auth-handlers]
            [lasso.middleware :as mw]))

(defn home-page
  "Serve the main application page."
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "public/index.html"))})

(defn health-check
  "Health check endpoint for container orchestration."
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:status "ok"})})

(def routes
  "Application route definitions."
  (route/expand-routes
   #{["/" :get home-page :route-name :home]
     ["/health" :get health-check :route-name :health]

     ;; Authentication routes
     ["/api/auth/init" :post auth-handlers/auth-init-handler :route-name :auth-init]
     ["/api/auth/callback" :get auth-handlers/auth-callback-handler :route-name :auth-callback]
     ["/api/auth/logout" :post [mw/require-auth auth-handlers/logout-handler] :route-name :auth-logout]}))
