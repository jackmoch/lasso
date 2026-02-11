(ns lasso.server
  "Pedestal server lifecycle management for Lasso application."
  (:require [io.pedestal.http :as http]
            [lasso.config :as config]
            [lasso.routes :as routes]
            [taoensso.timbre :as log])
  (:gen-class))

(defonce server-instance (atom nil))

(defn create-server
  "Create a Pedestal server configuration."
  []
  (let [{:keys [host port]} (:server config/config)]
    (http/create-server
     {::http/routes routes/routes
      ::http/type :jetty
      ::http/host host
      ::http/port port
      ::http/join? false
      ::http/resource-path "public"
      ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})))

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
