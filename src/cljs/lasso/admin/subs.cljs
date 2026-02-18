(ns lasso.admin.subs
  "Re-frame subscriptions for the admin dashboard."
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :admin/authenticated?
 (fn [db _]
   (get-in db [:admin :authenticated?])))

(rf/reg-sub
 :admin/loading?
 (fn [db _]
   (get-in db [:admin :loading?])))

(rf/reg-sub
 :admin/error
 (fn [db _]
   (get-in db [:admin :error])))

(rf/reg-sub
 :admin/metrics
 (fn [db _]
   (get-in db [:admin :metrics])))

(rf/reg-sub
 :admin/sessions
 (fn [db _]
   (get-in db [:admin :sessions])))

(rf/reg-sub
 :admin/last-refreshed
 (fn [db _]
   (get-in db [:admin :last-refreshed])))

;; Layer 3: derived subscriptions

(rf/reg-sub
 :admin/active-sessions
 :<- [:admin/sessions]
 (fn [sessions _]
   (filter #(= "active" (:state %)) sessions)))

(rf/reg-sub
 :admin/paused-sessions
 :<- [:admin/sessions]
 (fn [sessions _]
   (filter #(= "paused" (:state %)) sessions)))

(rf/reg-sub
 :admin/idle-sessions
 :<- [:admin/sessions]
 (fn [sessions _]
   (filter #(= "idle" (:state %)) sessions)))

(rf/reg-sub
 :admin/stopping?
 (fn [db [_ session-id]]
   (contains? (get-in db [:admin :stopping]) session-id)))

;; Current client-side route
(rf/reg-sub
 :current-route
 (fn [db _]
   (:route db)))
