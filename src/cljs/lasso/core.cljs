(ns lasso.core
  "Main entry point for Lasso frontend application."
  (:require [reagent.dom.client :as rdom]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]  ; Register :http-xhrio effect handler
            [lasso.db]
            [lasso.events]
            [lasso.subs]
            [lasso.admin.subs]
            [lasso.admin.events]
            [lasso.routes :as routes]
            [lasso.views :as views]))

;; -- Application Initialization --

(defonce root (rdom/create-root (.getElementById js/document "app")))

(defn mount-root
  "Mount the root component to the DOM."
  []
  (rf/clear-subscription-cache!)
  (rdom/render root [views/main-panel]))

(defn ^:export init
  "Initialize the application."
  []
  (js/console.log "🚀 Lasso initializing...")
  (rf/dispatch-sync [:initialize-db])
  (routes/init-routes!)
  (rf/dispatch [:check-auth])
  (mount-root)
  (js/console.log "✅ Lasso ready"))

;; -- Hot Reload Hooks --

(defn before-reload
  "Called before hot reload. Clean up state."
  []
  (js/console.log "%c🔄 Hot reload starting...", "color: orange; font-weight: bold"))

(defn after-reload
  "Called after hot reload. Re-mount UI."
  []
  (js/console.log "%c🔄 Reloading UI...", "color: blue; font-weight: bold")
  (mount-root)
  (js/console.log "%c✅ Hot reload complete! Changes applied.", "color: green; font-weight: bold"))
