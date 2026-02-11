(ns lasso.views
  "Main UI views for Lasso application."
  (:require [re-frame.core :as rf]))

(defn main-panel
  "Main application panel component."
  []
  (let [app-name @(rf/subscribe [:app-name])
        message @(rf/subscribe [:message])]
    [:div.min-h-screen.flex.items-center.justify-center.bg-gray-50
     [:div.max-w-2xl.mx-auto.text-center.p-8
      [:h1.text-5xl.font-bold.text-gray-900.mb-4
       app-name]
      [:p.text-xl.text-gray-600.mb-8
       message]
      [:button.btn-primary
       {:on-click #(js/alert "Coming soon! Sprint 3-4 will implement authentication.")}
       "Get Started"]]]))
