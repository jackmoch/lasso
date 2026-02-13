(ns lasso.subs
  "Re-frame subscriptions for Lasso application."
  (:require [re-frame.core :as rf]))

;; -- Layer 2: Simple Subscriptions --

(rf/reg-sub
 :auth/authenticated?
 (fn [db _]
   (get-in db [:auth :authenticated?])))

(rf/reg-sub
 :auth/username
 (fn [db _]
   (get-in db [:auth :username])))

(rf/reg-sub
 :auth/checking?
 (fn [db _]
   (get-in db [:auth :checking?])))

(rf/reg-sub
 :session/state
 (fn [db _]
   (get-in db [:session :state])))

(rf/reg-sub
 :session/target-username
 (fn [db _]
   (get-in db [:session :target-username])))

(rf/reg-sub
 :session/scrobble-count
 (fn [db _]
   (get-in db [:session :scrobble-count])))

(rf/reg-sub
 :session/recent-scrobbles
 (fn [db _]
   (get-in db [:session :recent-scrobbles])))

(rf/reg-sub
 :session/started-at
 (fn [db _]
   (get-in db [:session :started-at])))

(rf/reg-sub
 :session/last-poll
 (fn [db _]
   (get-in db [:session :last-poll])))

(rf/reg-sub
 :ui/error
 (fn [db _]
   (get-in db [:ui :error])))

(rf/reg-sub
 :ui/loading?
 (fn [db _]
   (get-in db [:ui :loading?])))

(rf/reg-sub
 :ui/session-control-loading?
 (fn [db _]
   (get-in db [:ui :session-control-loading?])))

(rf/reg-sub
 :ui/polling?
 (fn [db _]
   (get-in db [:ui :polling?])))

;; -- Layer 3: Computed Subscriptions --

(rf/reg-sub
 :session/can-start?
 :<- [:auth/authenticated?]
 :<- [:session/state]
 (fn [[authenticated? state] _]
   (and authenticated? (= :not-started state))))

(rf/reg-sub
 :session/can-pause?
 :<- [:session/state]
 (fn [state _]
   (= :active state)))

(rf/reg-sub
 :session/can-resume?
 :<- [:session/state]
 (fn [state _]
   (= :paused state)))

(rf/reg-sub
 :session/can-stop?
 :<- [:session/state]
 (fn [state _]
   (or (= :active state)
       (= :paused state))))

(rf/reg-sub
 :session/is-active?
 :<- [:session/state]
 (fn [state _]
   (or (= :active state)
       (= :paused state))))

(rf/reg-sub
 :session/recent-scrobbles-formatted
 :<- [:session/recent-scrobbles]
 (fn [scrobbles _]
   (map (fn [scrobble]
          (let [timestamp (:timestamp scrobble)
                date (when timestamp (js/Date. (* timestamp 1000)))]
            (assoc scrobble :formatted-time
                   (when date
                     (.toLocaleTimeString date "en-US"
                                         #js {:hour "2-digit"
                                              :minute "2-digit"})))))
        scrobbles)))
