(ns lasso.admin.events
  "Re-frame event handlers for the admin dashboard."
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [lasso.db :as db]))

(def admin-api-base "/api/admin")

(defn- admin-request
  "Build an http-xhrio effect map for admin API calls."
  [method endpoint {:keys [params on-success on-failure]}]
  {:method method
   :uri (str admin-api-base endpoint)
   :params (or params {})
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :with-credentials true
   :on-success on-success
   :on-failure on-failure})

;; =============================================================================
;; Navigation
;; =============================================================================

(rf/reg-event-fx
 :navigated
 (fn [{:keys [db]} [_ match]]
   (let [route-name (get-in match [:data :name])]
     (cond-> {:db (assoc db :route match)}
       ;; Auto-fetch status when navigating to the admin dashboard
       (= :admin route-name) (assoc :dispatch [:admin/fetch-status])
       ;; Fetch profile data when navigating to /profile
       (= :profile route-name) (assoc :dispatch [:user/fetch-profile])))))

;; =============================================================================
;; Admin Login
;; =============================================================================

(rf/reg-event-fx
 :admin/login
 (fn [{:keys [db]} [_ {:keys [username password]}]]
   {:db (-> db
            (assoc-in [:admin :loading?] true)
            (assoc-in [:admin :error] nil))
    :http-xhrio (admin-request
                 :post "/login"
                 {:params {:username username :password password}
                  :on-success [:admin/login-success]
                  :on-failure [:admin/login-failure]})}))

(rf/reg-event-fx
 :admin/login-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:admin :loading?] false)
            (assoc-in [:admin :authenticated?] true)
            (assoc-in [:admin :error] nil))
    :admin/navigate "/admin"}))

(rf/reg-event-db
 :admin/login-failure
 (fn [db [_ response]]
   (let [error-code (get-in response [:response :error_code])]
     (-> db
         (assoc-in [:admin :loading?] false)
         (assoc-in [:admin :error]
                   (if (= error-code "ADMIN_AUTH_FAILED")
                     "Invalid credentials. Please try again."
                     "Login failed. Please try again."))))))

;; =============================================================================
;; Admin Logout
;; =============================================================================

(rf/reg-event-fx
 :admin/logout
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:admin :loading?] true)
    :http-xhrio (admin-request
                 :post "/logout"
                 {:on-success [:admin/logout-success]
                  :on-failure [:admin/logout-success]})}))  ; navigate away even on failure

(rf/reg-event-fx
 :admin/logout-success
 (fn [{:keys [db]} _]
   {:db (assoc db :admin (:admin db/default-db))
    :admin/navigate "/admin/login"}))

;; =============================================================================
;; Status Fetching
;; =============================================================================

(rf/reg-event-fx
 :admin/fetch-status
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:admin :loading?] true)
    :http-xhrio (admin-request
                 :get "/status"
                 {:on-success [:admin/fetch-status-success]
                  :on-failure [:admin/fetch-status-failure]})}))

(rf/reg-event-db
 :admin/fetch-status-success
 (fn [db [_ response]]
   (-> db
       (assoc-in [:admin :loading?] false)
       (assoc-in [:admin :authenticated?] true)
       (assoc-in [:admin :error] nil)
       (assoc-in [:admin :metrics] (:metrics response))
       (assoc-in [:admin :sessions] (:sessions response))
       (assoc-in [:admin :last-refreshed] (js/Date.)))))

(rf/reg-event-fx
 :admin/fetch-status-failure
 (fn [{:keys [db]} [_ response]]
   (let [status (:status response)]
     (if (= 401 status)
       ;; Not authenticated — redirect to login
       {:db (-> db
                (assoc-in [:admin :loading?] false)
                (assoc-in [:admin :authenticated?] false))
        :admin/navigate "/admin/login"}
       {:db (-> db
                (assoc-in [:admin :loading?] false)
                (assoc-in [:admin :error] "Failed to load status. Please refresh."))}))))

;; =============================================================================
;; Force Stop Session
;; =============================================================================

(rf/reg-event-fx
 :admin/stop-session
 (fn [{:keys [db]} [_ session-id]]
   {:db (update-in db [:admin :stopping] conj session-id)
    :http-xhrio {:method :delete
                 :uri (str admin-api-base "/sessions/" session-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :with-credentials true
                 :on-success [:admin/stop-session-success session-id]
                 :on-failure [:admin/stop-session-failure session-id]}}))

(rf/reg-event-fx
 :admin/stop-session-success
 (fn [{:keys [db]} [_ session-id _response]]
   {:db (-> db
            (update-in [:admin :stopping] disj session-id)
            (update-in [:admin :sessions]
                       (fn [sessions]
                         (vec (remove #(= session-id (:session_id %)) sessions)))))
    :dispatch [:admin/fetch-status]}))

(rf/reg-event-db
 :admin/stop-session-failure
 (fn [db [_ session-id _response]]
   (-> db
       (update-in [:admin :stopping] disj session-id)
       (assoc-in [:admin :error] (str "Failed to stop session " session-id)))))

;; =============================================================================
;; UI
;; =============================================================================

(rf/reg-event-db
 :admin/set-error
 (fn [db [_ message]]
   (assoc-in db [:admin :error] message)))

(rf/reg-event-db
 :admin/clear-error
 (fn [db _]
   (assoc-in db [:admin :error] nil)))

;; =============================================================================
;; Navigation Effect Handler
;; =============================================================================

(rf/reg-fx
 :admin/navigate
 (fn [path]
   (set! (.-href (.-location js/window)) path)))
