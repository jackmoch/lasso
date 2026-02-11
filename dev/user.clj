(ns user
  "REPL utilities for development."
  (:require [lasso.server :as server]
            [clojure.tools.namespace.repl :refer [refresh]]))

(defn start
  "Start the development server."
  []
  (server/start)
  (println "Server started on http://localhost:8080"))

(defn stop
  "Stop the development server."
  []
  (server/stop)
  (println "Server stopped"))

(defn restart
  "Restart the development server."
  []
  (server/restart)
  (println "Server restarted"))

(defn reset
  "Stop server, reload namespaces, and restart server."
  []
  (stop)
  (refresh :after 'user/start))

(comment
  ;; Development workflow commands
  (start)
  (stop)
  (restart)
  (reset)
  )
