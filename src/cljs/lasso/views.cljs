(ns lasso.views
  "Main UI views for Lasso application."
  (:require [re-frame.core :as rf]
            [lasso.components.auth :as auth]
            [lasso.components.session-controls :as session-controls]
            [lasso.components.activity-feed :as activity-feed]
            [lasso.components.error :as error]))

(defn navbar
  "Application navbar with title and tagline."
  []
  [:div.bg-white.border-b.border-gray-200.mb-8
   [:div.max-w-4xl.mx-auto.px-4.py-6
    [:h1.text-3xl.font-bold.text-gray-900.mb-1
     "Lasso"]
    [:p.text-sm.text-gray-600
     "Track your Spotify Jam listening on Last.fm"]]])

(defn loading-spinner
  "Loading spinner component."
  []
  [:div.min-h-screen.flex.items-center.justify-center.bg-gray-50
   [:div.text-center
    [:div.inline-block.animate-spin.rounded-full.h-12.w-12.border-b-2.border-red-500]
    [:p.mt-4.text-gray-600 "Loading..."]]])

(defn main-panel
  "Main application panel component."
  []
  (fn []
    (let [checking? @(rf/subscribe [:auth/checking?])]
      (if checking?
        [loading-spinner]
        [:div.min-h-screen.bg-gray-50
         [navbar]
         [:div.max-w-4xl.mx-auto.px-4.pb-8
          [error/error-display]
          [auth/auth-component]
          [session-controls/session-controls]
          [activity-feed/activity-feed]]]))))
