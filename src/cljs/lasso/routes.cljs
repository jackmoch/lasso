(ns lasso.routes
  "Client-side routing using reitit-frontend."
  (:require [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [re-frame.core :as rfx]))

(def routes
  [["/" {:name :home}]
   ["/admin/login" {:name :admin/login}]
   ["/admin" {:name :admin}]])

(defn- on-navigate
  [match _history]
  (rfx/dispatch [:navigated match]))

(defn init-routes!
  "Start reitit-frontend routing with HTML5 pushState.
   Dispatches :navigated on every navigation event."
  []
  (rfe/start!
   (rf/router routes)
   on-navigate
   {:use-fragment false}))
