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
  (rf/clear-subscription-cache!)
  (rdom/render root [views/main-panel]))

(defn ^:export init
  "Initialize the application."
  []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:check-auth])
  (mount-root))
