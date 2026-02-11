(ns lasso.core
  "Main entry point for Lasso frontend application."
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [lasso.views :as views]))

;; -- Re-frame Event Handlers --

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   {:app-name "Lasso"
    :message "Welcome to Lasso! Track your Spotify Jam listening on Last.fm."
    :auth {:authenticated? false
           :username nil}
    :session {:state :not-started
              :target-username nil
              :scrobble-count 0
              :recent-scrobbles []
              :last-poll nil}
    :ui {:loading? false
         :error nil}}))

;; -- Re-frame Subscriptions --

(rf/reg-sub
 :app-name
 (fn [db _]
   (:app-name db)))

(rf/reg-sub
 :message
 (fn [db _]
   (:message db)))

;; -- Application Initialization --

(defn mount-root
  "Mount the root component to the DOM."
  []
  (rf/clear-subscription-cache!)
  (rdom/render [views/main-panel]
               (.getElementById js/document "app")))

(defn ^:export init
  "Initialize the application."
  []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))
