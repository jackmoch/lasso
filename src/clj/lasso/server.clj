(ns lasso.server
  "Pedestal server lifecycle management for Lasso application."
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :refer [interceptor]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [lasso.config :as config]
            [lasso.routes :as routes]
            [lasso.middleware.security :as security]
            [taoensso.timbre :as log])
  (:gen-class))

(defonce server-instance (atom nil))

(def spa-not-found-interceptor
  "Serve SPA HTML for non-API routes when no route matches.
   API routes return 404 JSON."
  (interceptor
   {:name ::spa-not-found
    :leave (fn [context]
             (if (nil? (get-in context [:response :status]))
               (let [uri (get-in context [:request :uri])]
                 (if (str/starts-with? uri "/api/")
                   (assoc context :response
                          {:status 404
                           :headers {"Content-Type" "application/json"}
                           :body "{\"error\": \"API endpoint not found\"}"})
                   (assoc context :response
                          {:status 200
                           :headers {"Content-Type" "text/html"}
                           :body (slurp (io/resource "public/index.html"))})))
               context))}))


(defn create-server
  "Create a Pedestal server configuration with security middleware."
  []
  (let [{:keys [host port]} (:server config/config)
        environment (:environment config/config)]
    (-> {::http/routes routes/routes
         ::http/type :jetty
         ::http/host host
         ::http/port port
         ::http/join? false
         ::http/resource-path "public"
         ;; Disable default secure headers (we use our own)
         ::http/secure-headers nil
         ;; Serve SPA HTML for unknown non-API routes
         ::http/not-found-interceptor spa-not-found-interceptor
         ;; Enable session support
         ::http/enable-session {:cookie-name "lasso-session"
                               :cookie-attrs {:http-only true
                                             :secure (= environment :production)
                                             :same-site :lax}}}
        ;; Add common interceptors that apply to all routes
        (http/default-interceptors)
        (update ::http/interceptors concat
                [security/cors-interceptor
                 security/security-headers-interceptor
                 security/rate-limit-interceptor
                 security/request-logging-interceptor
                 (body-params/body-params)]))))

(defn start
  "Start the Pedestal server."
  []
  (when-not @server-instance
    (let [server (-> (create-server)
                     http/create-server
                     http/start)]
      (reset! server-instance server)
      (log/info "Server started on port" (get-in config/config [:server :port]))
      server)))

(defn stop
  "Stop the Pedestal server."
  []
  (when @server-instance
    (http/stop @server-instance)
    (reset! server-instance nil)
    (log/info "Server stopped")))

(defn restart
  "Restart the Pedestal server."
  []
  (stop)
  (start))

(defn -main
  "Application entry point."
  [& _args]
  (log/info "Starting Lasso application...")
  (start))
