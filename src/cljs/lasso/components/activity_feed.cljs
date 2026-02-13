(ns lasso.components.activity-feed
  "Activity feed and session status display components."
  (:require [re-frame.core :as rf]))

(defn session-status-display
  "Display current session status information."
  []
  (fn []
    (let [state @(rf/subscribe [:session/state])
          target-username @(rf/subscribe [:session/target-username])
          scrobble-count @(rf/subscribe [:session/scrobble-count])
          last-poll @(rf/subscribe [:session/last-poll])
          polling? @(rf/subscribe [:ui/polling?])]
      [:div.bg-blue-50.border.border-blue-200.rounded-lg.p-4.mb-4
       [:div.flex.items-center.justify-between.mb-3
        [:div.flex.items-center.gap-2
         [:span.text-sm.font-medium.text-gray-700 "Status:"]
         (case state
           :active [:span.px-2.py-1.bg-green-100.text-green-800.rounded-full.text-xs.font-semibold
                    "Active"]
           :paused [:span.px-2.py-1.bg-yellow-100.text-yellow-800.rounded-full.text-xs.font-semibold
                    "Paused"]
           [:span.px-2.py-1.bg-gray-100.text-gray-800.rounded-full.text-xs.font-semibold
            "Unknown"])]
        (when polling?
          [:span.text-xs.text-blue-600.animate-pulse "● Polling"])]

       [:div.space-y-2.text-sm
        [:div.flex.justify-between
         [:span.text-gray-600 "Following:"]
         [:span.font-semibold.text-gray-900 target-username]]
        [:div.flex.justify-between
         [:span.text-gray-600 "Scrobbles tracked:"]
         [:span.font-semibold.text-gray-900 scrobble-count]]
        (when last-poll
          [:div.flex.justify-between
           [:span.text-gray-600 "Last update:"]
           [:span.text-gray-700
            (let [date (js/Date. last-poll)]
              (.toLocaleTimeString date "en-US"
                                  #js {:hour "2-digit"
                                       :minute "2-digit"
                                       :second "2-digit"}))]])]])))

(defn scrobble-item
  "Single scrobble item display."
  [scrobble]
  [:div.border-b.border-gray-200.py-3.last:border-b-0
   [:div.flex.justify-between.items-start
    [:div.flex-1
     [:p.font-medium.text-gray-900 (:track scrobble)]
     [:p.text-sm.text-gray-600 (:artist scrobble)]
     (when (:album scrobble)
       [:p.text-xs.text-gray-500 (:album scrobble)])]
    [:span.text-xs.text-gray-500.whitespace-nowrap.ml-4
     (:formatted-time scrobble)]]])

(defn scrobble-list
  "Scrollable list of recent scrobbles."
  []
  (fn []
    (let [scrobbles @(rf/subscribe [:session/recent-scrobbles-formatted])]
      (if (empty? scrobbles)
        [:div.text-center.py-8.text-gray-500
         [:p "No scrobbles yet."]
         [:p.text-sm.mt-1 "Scrobbles will appear here when the target user starts listening."]]
        [:div.max-h-96.overflow-y-auto.border.border-gray-200.rounded-md.bg-white.divide-y.divide-gray-200
         (for [scrobble scrobbles]
           ^{:key (str (:artist scrobble) "-" (:track scrobble) "-" (:timestamp scrobble))}
           [scrobble-item scrobble])]))))

(defn activity-feed
  "Main activity feed container."
  []
  (fn []
    (let [is-active? @(rf/subscribe [:session/is-active?])]
      (when is-active?
        [:div.bg-white.rounded-lg.shadow.p-6
         [:h2.text-xl.font-semibold.text-gray-900.mb-4
          "Activity Feed"]
         [session-status-display]
         [:div.mt-4
          [:h3.text-sm.font-medium.text-gray-700.mb-2
           "Recent Scrobbles"]
          [scrobble-list]]]))))
