(ns lasso.routes
  "HTTP routes for Lasso application."
  (:require [io.pedestal.http.route :as route]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

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
     ["/health" :get health-check :route-name :health]}))
