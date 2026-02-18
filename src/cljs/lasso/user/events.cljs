(ns lasso.user.events
  "Re-frame event handlers for user profile."
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]))

(rf/reg-event-fx
 :user/fetch-profile
 (fn [{:keys [db]} _]
   {:db         (-> db
                    (assoc-in [:user :loading?] true)
                    (assoc-in [:user :error] nil))
    :http-xhrio {:method          :get
                 :uri             "/api/user/profile"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :with-credentials true
                 :on-success      [:user/fetch-profile-success]
                 :on-failure      [:user/fetch-profile-failure]}}))

(rf/reg-event-db
 :user/fetch-profile-success
 (fn [db [_ response]]
   (-> db
       (assoc-in [:user :loading?] false)
       (assoc-in [:user :error] nil)
       (assoc-in [:user :profile] (dissoc response :sessions))
       (assoc-in [:user :sessions] (or (:sessions response) [])))))

(rf/reg-event-db
 :user/fetch-profile-failure
 (fn [db [_ response]]
   (let [status (:status response)
         error-code (get-in response [:response :error_code])]
     (-> db
         (assoc-in [:user :loading?] false)
         (assoc-in [:user :error]
                   (cond
                     (= error-code "PERSISTENCE_UNAVAILABLE")
                     "Profile history is not available yet."

                     (= 401 status)
                     "Please log in to view your profile."

                     :else
                     "Failed to load profile. Please try again."))))))
