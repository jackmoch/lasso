(ns lasso.core
  "Main entry point for Lasso frontend application."
  (:require [reagent.dom.client :as rdom]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]  ; Register :http-xhrio effect handler
            [lasso.db]
            [lasso.events]
            [lasso.subs]
            [lasso.views :as views]))

;; -- Application Initialization --

(defonce root (rdom/create-root (.getElementById js/document "app")))

(defn mount-root
  "Mount the root component to the DOM."
  []
  (js/console.log "🔧 mount-root called")
  (rf/clear-subscription-cache!)
  (rdom/render root [views/main-panel])
  (js/console.log "✅ Root mounted"))

(defn ^:export init
  "Initialize the application."
  []
  (js/console.log "🚀 APP INITIALIZING...")
  (rf/dispatch-sync [:initialize-db])
  (js/console.log "✅ DB initialized")
  (rf/dispatch [:check-auth])
  (js/console.log "✅ Auth check dispatched")
  (mount-root)
  (js/console.log "✅ APP INITIALIZED"))
