(ns lasso.events
  "Re-frame event handlers for Lasso application."
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [lasso.db :as db]
            [lasso.api :as api]))

;; -- Helper Functions --

(defn error-message
  "Map backend error codes to user-friendly messages."
  [response]
  (let [error-code (get-in response [:response :error_code])
        message (get-in response [:response :message])]
    (case error-code
      "MISSING_TARGET_USERNAME" "Please enter a Last.fm username."
      "INVALID_TARGET_USERNAME" "The username you entered doesn't exist on Last.fm."
      "SESSION_NOT_FOUND" "Your session has expired. Please log in again."
      "NO_ACTIVE_SESSION" "No active session found. Please start a session first."
      "SESSION_ALREADY_ACTIVE" "A session is already active."
      "OAUTH_TOKEN_FAILED" "Last.fm authentication failed. Please try again."
      "OAUTH_SESSION_FAILED" "Failed to establish session with Last.fm."
      "LASTFM_API_ERROR" "Last.fm is currently unavailable. Please try again later."
      "SCROBBLE_FAILED" "Failed to submit scrobble to Last.fm."
      ;; Fallback to backend message or generic error
      (or message "An error occurred. Please try again."))))

;; -- Initialization Events --

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 :check-auth
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :checking?] true)
    :http-xhrio (api/check-session-status
                 [:check-auth-success]
                 [:check-auth-failure])}))

(rf/reg-event-db
 :check-auth-success
 (fn [db [_ response]]
   (let [authenticated? (:authenticated response)
         username (:username response)
         session (:session response)]
     (cond-> db
       true (assoc-in [:auth :checking?] false)
       authenticated? (assoc-in [:auth :authenticated?] true)
       authenticated? (assoc-in [:auth :username] username)
       (and authenticated? session)
       (-> (assoc-in [:session :state] (keyword (:state session)))
           (assoc-in [:session :target-username] (:target_username session))
           (assoc-in [:session :scrobble-count] (:scrobble_count session))
           (assoc-in [:session :started-at] (:started_at session))
           (assoc-in [:session :last-poll] (:last_poll session)))))))

(rf/reg-event-fx
 :check-auth-failure
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :checking?] false)}))

;; -- Authentication Events --

(rf/reg-event-fx
 :auth/login
 (fn [_ _]
   {:http-xhrio (api/init-auth
                 [:auth/init-success]
                 [:auth/init-failure])}))

(rf/reg-event-fx
 :auth/init-success
 (fn [_ [_ response]]
   (let [auth-url (:auth_url response)]
     ;; Redirect to Last.fm authorization page
     (set! (.-location js/window) auth-url)
     {})))

(rf/reg-event-fx
 :auth/init-failure
 (fn [{:keys [db]} [_ response]]
   {:db (assoc-in db [:ui :error] (error-message response))}))

(rf/reg-event-fx
 :auth/logout
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:ui :loading?] true)
    :http-xhrio (api/logout
                 [:auth/logout-success]
                 [:auth/logout-failure])}))

(rf/reg-event-fx
 :auth/logout-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:ui :loading?] false)
            (assoc-in [:auth :authenticated?] false)
            (assoc-in [:auth :username] nil)
            (assoc-in [:session] (:session db/default-db))
            (assoc-in [:ui :polling?] false))
    :dispatch [:session/stop-polling]}))

(rf/reg-event-fx
 :auth/logout-failure
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:ui :loading?] false)
            (assoc-in [:ui :error] (error-message response)))}))

;; -- Session Control Events --

(rf/reg-event-fx
 :session/start
 (fn [{:keys [db]} [_ username]]
   (if (empty? username)
     {:db (assoc-in db [:ui :error] "Please enter a Last.fm username.")}
     {:db (assoc-in db [:ui :session-control-loading?] true)
      :http-xhrio (api/start-session
                   username
                   [:session/start-success]
                   [:session/start-failure])})))

(rf/reg-event-fx
 :session/start-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:ui :session-control-loading?] false)
            (assoc-in [:session :state] :active)
            (assoc-in [:session :target-username] (:target_username response))
            (assoc-in [:session :scrobble-count] 0)
            (assoc-in [:session :recent-scrobbles] [])
            (assoc-in [:session :started-at] (:started_at response)))
    :dispatch [:session/start-polling]}))

(rf/reg-event-db
 :session/start-failure
 (fn [db [_ response]]
   (-> db
       (assoc-in [:ui :session-control-loading?] false)
       (assoc-in [:ui :error] (error-message response)))))

(rf/reg-event-fx
 :session/pause
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:ui :session-control-loading?] true)
    :http-xhrio (api/pause-session
                 [:session/pause-success]
                 [:session/pause-failure])}))

(rf/reg-event-fx
 :session/pause-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:ui :session-control-loading?] false)
            (assoc-in [:session :state] :paused))
    :dispatch [:session/stop-polling]}))

(rf/reg-event-db
 :session/pause-failure
 (fn [db [_ response]]
   (-> db
       (assoc-in [:ui :session-control-loading?] false)
       (assoc-in [:ui :error] (error-message response)))))

(rf/reg-event-fx
 :session/resume
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:ui :session-control-loading?] true)
    :http-xhrio (api/resume-session
                 [:session/resume-success]
                 [:session/resume-failure])}))

(rf/reg-event-fx
 :session/resume-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:ui :session-control-loading?] false)
            (assoc-in [:session :state] :active))
    :dispatch [:session/start-polling]}))

(rf/reg-event-db
 :session/resume-failure
 (fn [db [_ response]]
   (-> db
       (assoc-in [:ui :session-control-loading?] false)
       (assoc-in [:ui :error] (error-message response)))))

(rf/reg-event-fx
 :session/stop
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:ui :session-control-loading?] true)
    :http-xhrio (api/stop-session
                 [:session/stop-success]
                 [:session/stop-failure])}))

(rf/reg-event-fx
 :session/stop-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:ui :session-control-loading?] false)
            (assoc-in [:session] (:session db/default-db)))
    :dispatch [:session/stop-polling]}))

(rf/reg-event-db
 :session/stop-failure
 (fn [db [_ response]]
   (-> db
       (assoc-in [:ui :session-control-loading?] false)
       (assoc-in [:ui :error] (error-message response)))))

;; -- Polling Events --

(rf/reg-event-fx
 :session/start-polling
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:ui :polling?] true)
    :dispatch [:session/status-poll]}))

(rf/reg-event-db
 :session/stop-polling
 (fn [db _]
   (assoc-in db [:ui :polling?] false)))

(rf/reg-event-fx
 :session/status-poll
 (fn [{:keys [db]} _]
   (let [polling? (get-in db [:ui :polling?])]
     (if polling?
       {:http-xhrio (api/check-session-status
                     [:session/status-success]
                     [:session/status-failure])}
       {}))))

(rf/reg-event-fx
 :session/status-success
 (fn [{:keys [db]} [_ response]]
   (let [session (:session response)
         authenticated? (:authenticated response)
         polling? (get-in db [:ui :polling?])
         session-active? (= :active (keyword (:state session)))]
     {:db (cond-> db
            ;; Update auth state
            true (assoc-in [:auth :authenticated?] authenticated?)
            authenticated? (assoc-in [:auth :username] (:username response))

            ;; Update session state
            session (assoc-in [:session :state] (keyword (:state session)))
            session (assoc-in [:session :target-username] (:target_username session))
            session (assoc-in [:session :scrobble-count] (:scrobble_count session))
            session (assoc-in [:session :recent-scrobbles] (:recent_scrobbles session))
            session (assoc-in [:session :last-poll] (:last_poll session)))

      ;; Continue polling if still active and polling flag is true
      :dispatch-later (when (and polling? session-active?)
                        [{:ms 5000 :dispatch [:session/status-poll]}])})))

(rf/reg-event-fx
 :session/status-failure
 (fn [{:keys [db]} [_ response]]
   (let [polling? (get-in db [:ui :polling?])
         status (:status response)]
     ;; Don't show errors for polling failures, just retry
     ;; Stop polling on 401 (unauthorized)
     (if (= 401 status)
       {:db (-> db
                (assoc-in [:ui :polling?] false)
                (assoc-in [:auth :authenticated?] false)
                (assoc-in [:ui :error] "Your session has expired. Please log in again."))}
       ;; Retry with exponential backoff
       {:dispatch-later (when polling?
                          [{:ms 10000 :dispatch [:session/status-poll]}])}))))

;; -- UI Events --

(rf/reg-event-db
 :ui/set-error
 (fn [db [_ error-message]]
   (assoc-in db [:ui :error] error-message)))

(rf/reg-event-db
 :ui/clear-error
 (fn [db _]
   (assoc-in db [:ui :error] nil)))
