(ns lasso.components.session-controls
  "Session control UI components."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn target-username-form
  "Input form for entering target Last.fm username."
  []
  (let [username-input (r/atom "")
        can-start? @(rf/subscribe [:session/can-start?])
        loading? @(rf/subscribe [:ui/session-control-loading?])]
    (fn []
      [:div.space-y-3
       [:label.block
        [:span.text-sm.font-medium.text-gray-700.mb-1.block
         "Enter Last.fm username to follow:"]
        [:input.w-full.px-4.py-2.border.border-gray-300.rounded-md.focus:ring-2.focus:ring-red-500.focus:border-transparent
         {:type "text"
          :placeholder "e.g., johndoe"
          :value @username-input
          :disabled (not can-start?)
          :on-change #(reset! username-input (-> % .-target .-value))
          :on-key-press #(when (and (= "Enter" (.-key %))
                                    (not (empty? @username-input)))
                           (rf/dispatch [:session/start @username-input]))}]]
       [:button.btn-primary.w-full
        {:on-click #(when-not (empty? @username-input)
                      (rf/dispatch [:session/start @username-input]))
         :disabled (or (not can-start?) loading? (empty? @username-input))}
        (if loading? "Starting..." "Start Following")]])))

(defn control-buttons
  "Session control buttons (pause/resume/stop)."
  []
  (let [show-confirm? (r/atom false)]
    (fn []
      (let [can-pause? @(rf/subscribe [:session/can-pause?])
            can-resume? @(rf/subscribe [:session/can-resume?])
            can-stop? @(rf/subscribe [:session/can-stop?])
            loading? @(rf/subscribe [:ui/session-control-loading?])]
        [:div.space-y-3
         ;; Pause button
         (when can-pause?
           [:button.btn-secondary.w-full
            {:on-click #(rf/dispatch [:session/pause])
             :disabled loading?}
            (if loading? "Pausing..." "Pause Session")])

         ;; Resume button
         (when can-resume?
           [:button.btn-primary.w-full
            {:on-click #(rf/dispatch [:session/resume])
             :disabled loading?}
            (if loading? "Resuming..." "Resume Session")])

         ;; Stop button with confirmation
         (when can-stop?
           (if @show-confirm?
             [:div.bg-yellow-50.border.border-yellow-200.rounded-md.p-4.space-y-3
              [:p.text-sm.text-yellow-800.font-medium
               "Are you sure you want to stop this session?"]
              [:p.text-xs.text-yellow-700
               "This will clear all session data and stop tracking scrobbles."]
              [:div.flex.gap-2
               [:button.flex-1.px-4.py-2.bg-red-600.text-white.rounded-md.hover:bg-red-700.transition-colors
                {:on-click #(do
                              (rf/dispatch [:session/stop])
                              (reset! show-confirm? false))
                 :disabled loading?}
                (if loading? "Stopping..." "Yes, Stop")]
               [:button.flex-1.px-4.py-2.bg-gray-200.text-gray-800.rounded-md.hover:bg-gray-300.transition-colors
                {:on-click #(reset! show-confirm? false)
                 :disabled loading?}
                "Cancel"]]]
             [:button.px-4.py-2.w-full.bg-red-600.text-white.rounded-md.hover:bg-red-700.transition-colors
              {:on-click #(reset! show-confirm? true)
               :disabled loading?}
              "Stop Session"]))]))))

(defn session-controls
  "Main session controls container."
  []
  (fn []
    (let [authenticated? @(rf/subscribe [:auth/authenticated?])
          can-start? @(rf/subscribe [:session/can-start?])
          is-active? @(rf/subscribe [:session/is-active?])]
      (when authenticated?
        [:div.bg-white.rounded-lg.shadow.p-6.mb-6
         [:h2.text-xl.font-semibold.text-gray-900.mb-4
          "Session Controls"]
         (if can-start?
           [target-username-form]
           [control-buttons])]))))
