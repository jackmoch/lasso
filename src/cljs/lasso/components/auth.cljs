(ns lasso.components.auth
  "Authentication UI components."
  (:require [re-frame.core :as rf]))

(defn login-button
  "Login with Last.fm button."
  []
  [:button.btn-primary
   {:on-click #(rf/dispatch [:auth/login])}
   "Login with Last.fm"])

(defn user-info
  "Display authenticated user info with logout button."
  []
  (let [username @(rf/subscribe [:auth/username])
        loading? @(rf/subscribe [:ui/loading?])]
    [:div.flex.items-center.justify-between.bg-white.rounded-lg.shadow.p-4.mb-6
     [:div.flex.items-center.gap-3
      [:div.w-10.h-10.bg-red-500.rounded-full.flex.items-center.justify-center.text-white.font-bold.text-lg
       (when username
         (-> username (.toUpperCase) (.charAt 0)))]
      [:div
       [:p.text-sm.text-gray-600 "Logged in as"]
       [:p.font-semibold.text-gray-900 username]]]
     [:button.px-4.py-2.text-sm.text-gray-700.hover:text-gray-900.hover:bg-gray-100.rounded-md.transition-colors
      {:on-click #(rf/dispatch [:auth/logout])
       :disabled loading?}
      (if loading? "Logging out..." "Logout")]]))

(defn auth-component
  "Main authentication component - shows login or user info."
  []
  (let [authenticated? @(rf/subscribe [:auth/authenticated?])]
    (if authenticated?
      [user-info]
      [:div.text-center.mb-8
       [:p.text-gray-600.mb-4
        "Login with your Last.fm account to start tracking scrobbles."]
       [login-button]])))
