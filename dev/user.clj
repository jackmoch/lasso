(ns user
  "REPL utilities for development."
  (:require [lasso.server :as server]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.shell :as shell]
            [logging :as log]))

;; Configure clean logging for development
(log/configure-dev-logging!)

;; Shadow-cljs process state
(defonce shadow-process (atom nil))

(defn start-frontend
  "Start shadow-cljs frontend watch as separate process."
  []
  (log/section "Frontend")
  (println "  • Starting shadow-cljs watch...")
  (let [process (future
                  (shell/sh "npx" "shadow-cljs" "watch" "app"
                           :dir (System/getProperty "user.dir")))]
    (reset! shadow-process process)
    (Thread/sleep 2000)  ; Give it time to start
    (println "  ✓ Hot reload enabled")
    (println "  → Edit files in src/cljs/ to see live updates")
    (println "  → Check browser console for reload messages")))

(defn stop-frontend
  "Stop shadow-cljs frontend watch."
  []
  (when @shadow-process
    (future-cancel @shadow-process)
    (reset! shadow-process nil)
    ;; Kill any shadow-cljs processes
    (shell/sh "pkill" "-f" "shadow-cljs")
    (println "  ✓ Frontend watch stopped")))

(defn start-backend
  "Start the Pedestal backend server."
  []
  (log/section "Backend")
  (server/start)
  (println "  ✓ Server started")
  (println "  → http://localhost:8080"))

(defn stop-backend
  "Stop the Pedestal backend server."
  []
  (server/stop)
  (println "  ✓ Backend stopped"))

(defn start
  "Start the full development environment (backend + frontend)."
  []
  (println "\n🚀 Starting Lasso development environment...")
  (println "   Please wait while services initialize...\n")
  (start-frontend)
  (start-backend)
  (log/banner "✅ Lasso Ready")
  (println "  🌐 Open http://localhost:8080 in your browser")
  (println "  🔍 Watch browser console for hot reload messages")
  (println)
  (println "  📝 REPL Commands:")
  (println "     (stop)      - Stop all services")
  (println "     (restart)   - Restart all services")
  (println "     (reset)     - Reload namespaces + restart")
  (println "     (cljs-repl) - Connect to browser REPL")
  (println))

(defn stop
  "Stop the full development environment (backend + frontend)."
  []
  (log/section "Stopping")
  (stop-backend)
  (stop-frontend)
  (println "  ✓ All services stopped")
  (println))

(defn restart
  "Restart the full development environment."
  []
  (log/section "Restarting")
  (stop)
  (start))

(defn reset
  "Stop server, reload namespaces, and restart server."
  []
  (log/section "Reloading")
  (stop)
  (println "  Refreshing namespaces...")
  (refresh :after 'user/start))

(defn go
  "Alias for start - common in Clojure dev workflows."
  []
  (start))

(defn cljs-repl
  "Start a ClojureScript REPL connected to the browser."
  []
  (println "To connect to ClojureScript REPL, run in a separate terminal:")
  (println "  npx shadow-cljs cljs-repl app")
  (println "\nOr use the browser console for debugging."))

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
