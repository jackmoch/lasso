(ns lasso.components.error
  "Error display UI component."
  (:require [re-frame.core :as rf]))

(defn error-display
  "Dismissable error banner."
  []
  (fn []
    (let [error @(rf/subscribe [:ui/error])]
      (when error
        [:div.bg-red-50.border.border-red-200.rounded-lg.p-4.mb-6.flex.items-start.justify-between
         [:div.flex.items-start.gap-3
          [:div.flex-shrink-0
           [:svg.w-5.h-5.text-red-600
            {:fill "currentColor" :viewBox "0 0 20 20"}
            [:path {:fill-rule "evenodd"
                    :d "M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    :clip-rule "evenodd"}]]]
          [:div.flex-1
           [:p.text-sm.font-medium.text-red-800 "Error"]
           [:p.text-sm.text-red-700.mt-1 error]]]
         [:button.flex-shrink-0.text-red-600.hover:text-red-800.transition-colors
          {:on-click #(rf/dispatch [:ui/clear-error])
           :aria-label "Dismiss error"}
          [:svg.w-5.h-5
           {:fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
           [:path {:stroke-linecap "round"
                   :stroke-linejoin "round"
                   :stroke-width "2"
                   :d "M6 18L18 6M6 6l12 12"}]]]]))))
