(ns lasso.admin.views
  "Admin dashboard UI components: login page and session console."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

;; =============================================================================
;; Helpers
;; =============================================================================

(defn- format-age
  "Convert milliseconds to a human-readable elapsed time string."
  [age-ms]
  (let [total-secs (quot age-ms 1000)
        hours (quot total-secs 3600)
        mins (quot (rem total-secs 3600) 60)]
    (cond
      (< total-secs 60) "just now"
      (< total-secs 3600) (str mins "m")
      :else (str hours "h " mins "m"))))

(defn- format-time
  "Format a js/Date to HH:MM:SS."
  [date]
  (when date
    (.toLocaleTimeString date "en-US" #js {:hour "2-digit"
                                           :minute "2-digit"
                                           :second "2-digit"})))

(defn- state-badge
  "Render a colored state indicator."
  [state]
  (let [[dot color] (case state
                      "active" ["●" "text-green-600"]
                      "paused" ["◐" "text-yellow-600"]
                      ["○" "text-gray-400"])]
    [:span {:class (str color " mr-1 font-bold")} dot]
    ))

;; =============================================================================
;; Login Page
;; =============================================================================

(defn login-page
  "Admin login form."
  []
  (let [username (r/atom "")
        password (r/atom "")]
    (fn []
      (let [loading? @(rf/subscribe [:admin/loading?])
            error @(rf/subscribe [:admin/error])]
        [:div.min-h-screen.bg-gray-100.flex.items-center.justify-center
         [:div.bg-white.rounded-lg.shadow-md.p-8.w-full.max-w-sm
          [:h1.text-2xl.font-bold.text-gray-900.text-center.mb-6
           "Lasso Admin"]

          [:form
           {:on-submit (fn [e]
                         (.preventDefault e)
                         (rf/dispatch [:admin/login
                                       {:username @username
                                        :password @password}]))}

           [:div.mb-4
            [:label.block.text-sm.font-medium.text-gray-700.mb-1
             {:for "admin-username"} "Username"]
            [:input.w-full.border.border-gray-300.rounded-md.px-3.py-2.text-sm.focus:outline-none.focus:ring-2.focus:ring-red-500
             {:id "admin-username"
              :type "text"
              :value @username
              :auto-focus true
              :on-change #(reset! username (.. % -target -value))
              :disabled loading?
              :placeholder "Username"}]]

           [:div.mb-6
            [:label.block.text-sm.font-medium.text-gray-700.mb-1
             {:for "admin-password"} "Password"]
            [:input.w-full.border.border-gray-300.rounded-md.px-3.py-2.text-sm.focus:outline-none.focus:ring-2.focus:ring-red-500
             {:id "admin-password"
              :type "password"
              :value @password
              :on-change #(reset! password (.. % -target -value))
              :disabled loading?
              :placeholder "Password"}]]

           [:button.w-full.bg-red-600.hover:bg-red-700.text-white.font-medium.py-2.px-4.rounded-md.transition-colors
            {:type "submit"
             :disabled loading?}
            (if loading? "Signing in…" "Sign In")]

           (when error
             [:div.mt-4.flex.items-center.text-sm.text-red-600
              [:span.mr-1 "⚠"]
              error])]]]))))

;; =============================================================================
;; Dashboard Components
;; =============================================================================

(defn metric-card
  "A single metric count card."
  [label value]
  [:div.bg-white.rounded-lg.shadow.p-4.text-center
   [:p.text-sm.text-gray-500.mb-1 label]
   [:p.text-3xl.font-bold.text-gray-900 (or value "—")]])

(defn metrics-bar
  "Row of metric count cards."
  []
  (fn []
    (let [metrics @(rf/subscribe [:admin/metrics])]
      [:div.grid.grid-cols-3.gap-4.mb-4
       [metric-card "Total Sessions" (:total_sessions metrics)]
       [metric-card "Active Sessions" (:active_sessions metrics)]
       [metric-card "Paused" (:paused_sessions metrics)]])))

(defn session-row
  "A single row in the sessions table. Handles inline stop confirmation."
  [_session]
  (let [confirming? (r/atom false)]
    (fn [session]
      (let [session-id (:session_id session)
            stopping? @(rf/subscribe [:admin/stopping? session-id])
            age-str (format-age (:session_age_ms session))]
        [:tr.border-b.border-gray-100.hover:bg-gray-50
         ;; User
         [:td.py-3.px-4.text-sm.font-medium.text-gray-900
          (:username session)]
         ;; Target
         [:td.py-3.px-4.text-sm.text-gray-600
          (or (:target_username session) "—")]
         ;; State
         [:td.py-3.px-4.text-sm
          [state-badge (:state session)]
          [:span {:class (case (:state session)
                           "active" "text-green-700"
                           "paused" "text-yellow-700"
                           "text-gray-500")}
           (:state session)]]
         ;; Scrobbles
         [:td.py-3.px-4.text-sm.text-gray-600.text-right
          (or (:scrobble_count session) "—")]
         ;; Age
         [:td.py-3.px-4.text-sm.text-gray-500
          age-str]
         ;; Action
         [:td.py-3.px-4.text-sm
          (if @confirming?
            [:span.inline-flex.items-center.gap-2
             [:span.text-xs.text-gray-600 (str "Stop " (:username session) "?")]
             [:button.text-xs.bg-red-600.text-white.px-2.py-1.rounded.hover:bg-red-700
              {:on-click (fn []
                           (reset! confirming? false)
                           (rf/dispatch [:admin/stop-session session-id]))
               :disabled stopping?}
              "Confirm"]
             [:button.text-xs.text-gray-500.hover:text-gray-700.px-2.py-1
              {:on-click #(reset! confirming? false)} "Cancel"]]
            [:button.text-sm.text-red-600.hover:text-red-800.font-medium
             {:on-click #(reset! confirming? true)
              :disabled stopping?}
             (if stopping? "Stopping…" "Stop")])]]))))

(defn sessions-table
  "Table of sessions with show-all toggle."
  []
  (let [show-idle? (r/atom false)]
    (fn []
      (let [all-sessions @(rf/subscribe [:admin/sessions])
            idle-sessions @(rf/subscribe [:admin/idle-sessions])
            visible-sessions (if @show-idle?
                               all-sessions
                               (remove #(= "idle" (:state %)) all-sessions))
            idle-count (count idle-sessions)]
        [:div
         [:h2.text-lg.font-semibold.text-gray-900.mb-3 "Sessions"]
         [:div.bg-white.rounded-lg.shadow.overflow-hidden
          [:table.w-full
           [:thead
            [:tr.bg-gray-50.border-b.border-gray-200
             [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "User"]
             [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Target"]
             [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "State"]
             [:th.py-3.px-4.text-right.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Scrobbles"]
             [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Age"]
             [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Action"]]]
           [:tbody
            (if (empty? visible-sessions)
              [:tr [:td.py-6.px-4.text-center.text-sm.text-gray-400
                    {:col-span 6} "No sessions"]]
              (for [s visible-sessions]
                ^{:key (:session_id s)} [session-row s]))]]]

         (when (and (pos? idle-count) (not @show-idle?))
           [:div.mt-2.flex.items-center.justify-between.text-sm.text-gray-500
            [:span (str idle-count " idle session" (when (> idle-count 1) "s") " hidden")]
            [:button.text-red-600.hover:text-red-800.font-medium
             {:on-click #(reset! show-idle? true)}
             (str "Show all " (count all-sessions) " sessions")]])

         (when @show-idle?
           [:div.mt-2.text-right
            [:button.text-sm.text-gray-500.hover:text-gray-700
             {:on-click #(reset! show-idle? false)}
             "Collapse idle sessions"]])]))))

;; =============================================================================
;; Dashboard Page
;; =============================================================================

(defn dashboard
  "Admin dashboard: metrics + session table."
  []
  (fn []
    (let [loading? @(rf/subscribe [:admin/loading?])
          error @(rf/subscribe [:admin/error])
          last-refreshed @(rf/subscribe [:admin/last-refreshed])]
      [:div.min-h-screen.bg-gray-100
       ;; Header
       [:div.bg-white.border-b.border-gray-200.mb-6
        [:div.max-w-5xl.mx-auto.px-4.py-4.flex.items-center.justify-between
         [:h1.text-xl.font-bold.text-gray-900 "Lasso Admin Console"]
         [:div.flex.items-center.gap-3
          [:button.flex.items-center.gap-1.px-3.py-2.text-sm.text-gray-600.hover:text-gray-900.bg-white.border.border-gray-300.rounded-md.hover:bg-gray-50.transition-colors
           {:on-click #(rf/dispatch [:admin/fetch-status])
            :disabled loading?}
           [:span (if loading? "↻ Refreshing…" "↻ Refresh")]]
          [:button.px-3.py-2.text-sm.text-gray-600.hover:text-gray-900.hover:bg-gray-100.rounded-md.transition-colors
           {:on-click #(rf/dispatch [:admin/logout])}
           "Sign Out"]]]]

       [:div.max-w-5xl.mx-auto.px-4

        ;; Error banner
        (when error
          [:div.mb-4.p-3.bg-red-50.border.border-red-200.rounded-md.text-sm.text-red-700
           error])

        ;; Metric cards
        [metrics-bar]

        ;; Last refreshed
        (when last-refreshed
          [:p.text-xs.text-gray-400.mb-4
           (str "Last refreshed: " (format-time last-refreshed))])

        ;; Sessions table
        [sessions-table]]])))
