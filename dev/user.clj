(ns user
  "REPL utilities for development with integrated shadow-cljs."
  (:require [lasso.server :as server]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]
            [clojure.tools.namespace.repl :refer [refresh]]))

;; Shadow-cljs server state
(defonce shadow-server-running? (atom false))

(defn start-shadow-server
  "Start shadow-cljs server if not already running."
  []
  (when-not @shadow-server-running?
    (shadow-server/start!)
    (reset! shadow-server-running? true)
    (println "✓ shadow-cljs server started")))

(defn start-frontend
  "Start shadow-cljs frontend watch."
  []
  (start-shadow-server)
  (shadow/watch :app)
  (println "✓ Frontend watch started (shadow-cljs)"))

(defn stop-frontend
  "Stop shadow-cljs frontend watch."
  []
  (shadow/stop-worker :app)
  (println "✓ Frontend watch stopped"))

(defn start-backend
  "Start the Pedestal backend server."
  []
  (server/start)
  (println "✓ Backend server started on http://localhost:8080"))

(defn stop-backend
  "Stop the Pedestal backend server."
  []
  (server/stop)
  (println "✓ Backend server stopped"))

(defn start
  "Start the full development environment (backend + frontend)."
  []
  (println "\n🚀 Starting Lasso development environment...\n")
  (start-frontend)
  (start-backend)
  (println "\n✅ Ready! Open http://localhost:8080\n")
  (println "Hot reload enabled for:")
  (println "  • Frontend: shadow-cljs watching src/cljs/")
  (println "  • Backend: Use (reset) to reload namespaces\n"))

(defn stop
  "Stop the full development environment (backend + frontend)."
  []
  (println "\n🛑 Stopping Lasso development environment...\n")
  (stop-backend)
  (stop-frontend)
  (println "✅ Stopped\n"))

(defn restart
  "Restart the full development environment."
  []
  (stop)
  (start))

(defn reset
  "Stop server, reload namespaces, and restart server."
  []
  (println "\n♻️  Reloading namespaces...\n")
  (stop)
  (refresh :after 'user/start))

(defn go
  "Alias for start - common in Clojure dev workflows."
  []
  (start))

(defn cljs-repl
  "Start a ClojureScript REPL connected to the browser."
  []
  (shadow/repl :app))

(comment
  ;; Development workflow commands
  (start)   ; or (go) - Start everything
  (stop)    ; Stop everything
  (restart) ; Restart everything
  (reset)   ; Reload namespaces + restart

  ;; Individual control
  (start-backend)
  (stop-backend)
  (start-frontend)
  (stop-frontend)

  ;; ClojureScript REPL
  (cljs-repl)
  )
