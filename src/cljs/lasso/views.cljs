(ns lasso.views
  "Main UI views for Lasso application."
  (:require [re-frame.core :as rf]
            [lasso.components.auth :as auth]
            [lasso.components.session-controls :as session-controls]
            [lasso.components.activity-feed :as activity-feed]
            [lasso.components.error :as error]
            [lasso.components.how-it-works :as how-it-works]
            [lasso.admin.views :as admin-views]))

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

(defn home-panel
  "Main home page panel."
  []
  (fn []
    (js/console.log "🎨 MAIN-PANEL RENDER")
    (let [checking? @(rf/subscribe [:auth/checking?])]
      (js/console.log "🎨 main-panel checking?:" checking?)
      (if checking?
        [loading-spinner]
        [:div.min-h-screen.bg-gray-50
         [navbar]
         [:div.max-w-4xl.mx-auto.px-4.pb-8
          [error/error-display]
          [auth/auth-component]
          (when-not @(rf/subscribe [:auth/authenticated?])
            [how-it-works/how-it-works])
          [session-controls/session-controls]
          [activity-feed/activity-feed]]]))))

(defn main-panel
  "Root component — delegates to the correct view based on current route."
  []
  (fn []
    (let [route @(rf/subscribe [:current-route])
          route-name (get-in route [:data :name])]
      (case route-name
        :admin/login [admin-views/login-page]
        :admin       [admin-views/dashboard]
        [home-panel]))))
