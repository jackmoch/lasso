(ns lasso.user.subs
  "Re-frame subscriptions for user profile."
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :user/loading?
 (fn [db _]
   (get-in db [:user :loading?])))

(rf/reg-sub
 :user/error
 (fn [db _]
   (get-in db [:user :error])))

(rf/reg-sub
 :user/profile
 (fn [db _]
   (get-in db [:user :profile])))

(rf/reg-sub
 :user/sessions
 (fn [db _]
   (get-in db [:user :sessions])))

(rf/reg-sub
 :user/total-scrobbles
 :<- [:user/profile]
 (fn [profile _]
   (or (:total_scrobbles profile) 0)))

(rf/reg-sub
 :user/distinct-targets
 :<- [:user/sessions]
 (fn [sessions _]
   (->> sessions
        (map :target_username)
        (remove nil?)
        set
        count)))
