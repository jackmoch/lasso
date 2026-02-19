(ns lasso.user.views
  "Profile page UI components."
  (:require [re-frame.core :as rf]))

;; =============================================================================
;; Helpers
;; =============================================================================

(defn- format-date
  "Format a millisecond timestamp to a readable date string."
  [ms]
  (when ms
    (.toLocaleDateString (js/Date. ms) "en-US"
                         #js {:year "numeric" :month "short" :day "numeric"})))

(defn- format-duration
  "Return a human-readable duration string from started_at and ended_at (ms)."
  [started-at ended-at]
  (when (and started-at ended-at)
    (let [diff-ms   (- ended-at started-at)
          total-min (quot diff-ms 60000)
          hours     (quot total-min 60)
          mins      (rem total-min 60)]
      (cond
        (zero? total-min) "< 1m"
        (zero? hours)     (str mins "m")
        (zero? mins)      (str hours "h")
        :else             (str hours "h " mins "m")))))

(defn- stat-card
  "A single statistics card."
  [label value]
  [:div.bg-white.rounded-lg.shadow.p-4.text-center
   [:p.text-sm.text-gray-500.mb-1 label]
   [:p.text-3xl.font-bold.text-gray-900 (or value "—")]])

;; =============================================================================
;; Session history table
;; =============================================================================

(defn- session-row
  "A single row in the session history table."
  [session]
  [:tr.border-b.border-gray-100.hover:bg-gray-50
   [:td.py-3.px-4.text-sm.font-medium.text-gray-900
    (:target_username session)]
   [:td.py-3.px-4.text-sm.text-right.text-gray-700
    (or (:scrobble_count session) 0)]
   [:td.py-3.px-4.text-sm.text-gray-500
    (format-duration (:started_at session) (:ended_at session))]
   [:td.py-3.px-4.text-sm.text-gray-400
    (format-date (:started_at session))]
   [:td.py-3.px-4.text-sm
    [:span {:class (case (:state session)
                     "active"    "text-green-700 font-medium"
                     "completed" "text-gray-500"
                     "stopped"   "text-gray-400"
                     "text-gray-400")}
     (:state session)]]])

(defn- sessions-table
  "Session history table component."
  [sessions]
  (if (empty? sessions)
    [:p.text-sm.text-gray-400.py-4 "No sessions recorded yet."]
    [:div.bg-white.rounded-lg.shadow.overflow-hidden
     [:table.w-full
      [:thead
       [:tr.bg-gray-50.border-b.border-gray-200
        [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Target"]
        [:th.py-3.px-4.text-right.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Scrobbles"]
        [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Duration"]
        [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Date"]
        [:th.py-3.px-4.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Status"]]]
      [:tbody
       (for [s sessions]
         ^{:key (or (:id s) (:started_at s))} [session-row s])]]]))

;; =============================================================================
;; Profile page
;; =============================================================================

(defn profile-page
  "User profile page — shows stats and session history."
  []
  (fn []
    (let [loading?        @(rf/subscribe [:user/loading?])
          error           @(rf/subscribe [:user/error])
          profile         @(rf/subscribe [:user/profile])
          sessions        @(rf/subscribe [:user/sessions])
          total-scrobbles @(rf/subscribe [:user/total-scrobbles])
          distinct-count  @(rf/subscribe [:user/distinct-targets])
          username        @(rf/subscribe [:auth/username])]
      [:div.min-h-screen.bg-gray-50
       ;; Navbar
       [:div.bg-white.border-b.border-gray-200.mb-8
        [:div.max-w-4xl.mx-auto.px-4.py-6.flex.items-center.justify-between
         [:div
          [:h1.text-3xl.font-bold.text-gray-900.mb-1 "Lasso"]
          [:p.text-sm.text-gray-600 "Track your Spotify Jam listening on Last.fm"]]
         [:div.flex.items-center.gap-4
          [:a.text-sm.text-gray-600.hover:text-gray-900
           {:href "/"} "Home"]
          [:button.text-sm.text-gray-600.hover:text-gray-900
           {:on-click #(rf/dispatch [:auth/logout])}
           "Sign Out"]]]]

       [:div.max-w-4xl.mx-auto.px-4.pb-8
        ;; Username heading
        [:h2.text-2xl.font-bold.text-gray-900.mb-1
         (or username "Your Profile")]
        (when (:first_seen profile)
          [:p.text-sm.text-gray-500.mb-6
           (str "Member since " (format-date (:first_seen profile)))])

        ;; Loading
        (when loading?
          [:div.text-center.py-8
           [:div.inline-block.animate-spin.rounded-full.h-8.w-8.border-b-2.border-red-500]])

        ;; Error
        (when error
          [:div.mb-6.p-4.bg-amber-50.border.border-amber-200.rounded-md.text-sm.text-amber-700
           error])

        ;; Stats cards
        (when-not loading?
          [:div.grid.grid-cols-3.gap-4.mb-8
           [stat-card "Total Scrobbles" total-scrobbles]
           [stat-card "Sessions" (count sessions)]
           [stat-card "Artists Followed" distinct-count]])

        ;; Session history
        (when-not loading?
          [:div
           [:h3.text-lg.font-semibold.text-gray-900.mb-3 "Session History"]
           [sessions-table sessions]])]])))
