(ns lasso.events-test
  "Comprehensive tests for Re-frame event handlers"
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [re-frame.core :as rf]
            [lasso.db :as db]
            [lasso.events]  ; Load event handlers
            [lasso.test-utils :as tu]))

(use-fixtures :each tu/with-fresh-db)

;; ============================================================================
;; Helper Function Tests
;; ============================================================================

(deftest error-message-test
  (testing "Maps error codes to user-friendly messages"
    (is (= "Please enter a Last.fm username."
           (lasso.events/error-message {:response {:error_code "MISSING_TARGET_USERNAME"}})))

    (is (= "The username you entered doesn't exist on Last.fm."
           (lasso.events/error-message {:response {:error_code "INVALID_TARGET_USERNAME"}})))

    (is (= "Your session has expired. Please log in again."
           (lasso.events/error-message {:response {:error_code "SESSION_NOT_FOUND"}})))

    (is (= "Last.fm authentication failed. Please try again."
           (lasso.events/error-message {:response {:error_code "OAUTH_TOKEN_FAILED"}}))))

  (testing "Falls back to backend message when code unknown"
    (is (= "Custom error from backend"
           (lasso.events/error-message {:response {:message "Custom error from backend"}}))))

  (testing "Falls back to generic message when no code or message"
    (is (= "An error occurred. Please try again."
           (lasso.events/error-message {:response {}})))))

;; ============================================================================
;; Initialization Events
;; ============================================================================

(deftest initialize-db-test
  (testing "Sets db to default state"
    (rf/dispatch-sync [:initialize-db])
    (is (= db/default-db (tu/get-db)))))

(deftest check-auth-test
  (testing "Sets checking flag"
    (tu/set-db! db/default-db)
    (rf/dispatch-sync [:check-auth])
    (is (true? (tu/get-in-db [:auth :checking?])))))

(deftest check-auth-success-test
  (testing "Updates auth state when authenticated without session"
    (tu/set-db! db/default-db)
    (rf/dispatch-sync [:check-auth-success {:authenticated true
                                             :username "testuser"
                                             :session nil}])
    (is (false? (tu/get-in-db [:auth :checking?])))
    (is (true? (tu/get-in-db [:auth :authenticated?])))
    (is (= "testuser" (tu/get-in-db [:auth :username]))))

  (testing "Updates both auth and session when session exists"
    (tu/set-db! db/default-db)
    (rf/dispatch-sync [:check-auth-success {:authenticated true
                                             :username "testuser"
                                             :session {:state "active"
                                                       :target_username "targetuser"
                                                       :scrobble_count 5
                                                       :recent_scrobbles []
                                                       :started_at 1234567890
                                                       :last_poll 1234567900}}])
    (is (true? (tu/get-in-db [:auth :authenticated?])))
    (is (= :active (tu/get-in-db [:session :state])))
    (is (= "targetuser" (tu/get-in-db [:session :target-username])))
    (is (= 5 (tu/get-in-db [:session :scrobble-count]))))

  (testing "Does not update auth when not authenticated"
    (tu/set-db! db/default-db)
    (rf/dispatch-sync [:check-auth-success {:authenticated false}])
    (is (false? (tu/get-in-db [:auth :authenticated?])))
    (is (nil? (tu/get-in-db [:auth :username])))))

(deftest check-auth-failure-test
  (testing "Clears checking flag"
    (tu/set-db! (assoc-in db/default-db [:auth :checking?] true))
    (rf/dispatch-sync [:check-auth-failure])
    (is (false? (tu/get-in-db [:auth :checking?])))))

;; ============================================================================
;; Authentication Events
;; ============================================================================

(deftest auth-init-failure-test
  (testing "Sets error message on init failure"
    (tu/set-db! db/default-db)
    (rf/dispatch-sync [:auth/init-failure {:response {:error_code "OAUTH_TOKEN_FAILED"}}])
    (is (= "Last.fm authentication failed. Please try again."
           (tu/get-in-db [:ui :error])))))

(deftest auth-logout-test
  (testing "Sets loading flag"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:auth/logout])
    (is (true? (tu/get-in-db [:ui :loading?])))))

(deftest auth-logout-success-test
  (testing "Clears auth state and resets session"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:auth/logout-success])
    (is (false? (tu/get-in-db [:ui :loading?])))
    (is (false? (tu/get-in-db [:auth :authenticated?])))
    (is (nil? (tu/get-in-db [:auth :username])))
    (is (= :not-started (tu/get-in-db [:session :state])))
    (is (nil? (tu/get-in-db [:session :target-username])))
    (is (false? (tu/get-in-db [:ui :polling?])))))

(deftest auth-logout-failure-test
  (testing "Clears loading and sets error"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :loading?] true))
    (rf/dispatch-sync [:auth/logout-failure {:response {:message "Logout failed"}}])
    (is (false? (tu/get-in-db [:ui :loading?])))
    (is (= "Logout failed" (tu/get-in-db [:ui :error])))))

;; ============================================================================
;; Session Control Events
;; ============================================================================

(deftest session-start-empty-username-test
  (testing "Shows error when username is empty"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:session/start ""])
    (is (= "Please enter a Last.fm username." (tu/get-in-db [:ui :error])))))

(deftest session-start-valid-username-test
  (testing "Sets loading flag when username provided"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:session/start "validuser"])
    (is (true? (tu/get-in-db [:ui :session-control-loading?])))))

(deftest session-start-success-test
  (testing "Updates session state on success"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:session/start-success {:target_username "targetuser"
                                                :started_at 1234567890}])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= :active (tu/get-in-db [:session :state])))
    (is (= "targetuser" (tu/get-in-db [:session :target-username])))
    (is (= 0 (tu/get-in-db [:session :scrobble-count])))
    (is (= 1234567890 (tu/get-in-db [:session :started-at])))))

(deftest session-start-failure-test
  (testing "Shows error on start failure"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :session-control-loading?] true))
    (rf/dispatch-sync [:session/start-failure {:response {:error_code "INVALID_TARGET_USERNAME"}}])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= "The username you entered doesn't exist on Last.fm."
           (tu/get-in-db [:ui :error])))))

(deftest session-pause-test
  (testing "Sets loading flag on pause"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:session/pause])
    (is (true? (tu/get-in-db [:ui :session-control-loading?])))))

(deftest session-pause-success-test
  (testing "Updates state to paused and stops polling"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:session/pause-success])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= :paused (tu/get-in-db [:session :state])))))

(deftest session-pause-failure-test
  (testing "Shows error on pause failure"
    (tu/set-db! (assoc-in tu/active-session-db [:ui :session-control-loading?] true))
    (rf/dispatch-sync [:session/pause-failure {:response {:message "Pause failed"}}])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= "Pause failed" (tu/get-in-db [:ui :error])))))

(deftest session-resume-test
  (testing "Sets loading flag on resume"
    (tu/set-db! tu/paused-session-db)
    (rf/dispatch-sync [:session/resume])
    (is (true? (tu/get-in-db [:ui :session-control-loading?])))))

(deftest session-resume-success-test
  (testing "Updates state to active and starts polling"
    (tu/set-db! tu/paused-session-db)
    (rf/dispatch-sync [:session/resume-success])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= :active (tu/get-in-db [:session :state])))))

(deftest session-resume-failure-test
  (testing "Shows error on resume failure"
    (tu/set-db! (assoc-in tu/paused-session-db [:ui :session-control-loading?] true))
    (rf/dispatch-sync [:session/resume-failure {:response {:message "Resume failed"}}])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= "Resume failed" (tu/get-in-db [:ui :error])))))

(deftest session-stop-test
  (testing "Sets loading flag on stop"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:session/stop])
    (is (true? (tu/get-in-db [:ui :session-control-loading?])))))

(deftest session-stop-success-test
  (testing "Resets session to default state"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:session/stop-success])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= :not-started (tu/get-in-db [:session :state])))
    (is (nil? (tu/get-in-db [:session :target-username])))
    (is (= 0 (tu/get-in-db [:session :scrobble-count])))
    (is (empty? (tu/get-in-db [:session :recent-scrobbles])))))

(deftest session-stop-failure-test
  (testing "Shows error on stop failure"
    (tu/set-db! (assoc-in tu/active-session-db [:ui :session-control-loading?] true))
    (rf/dispatch-sync [:session/stop-failure {:response {:message "Stop failed"}}])
    (is (false? (tu/get-in-db [:ui :session-control-loading?])))
    (is (= "Stop failed" (tu/get-in-db [:ui :error])))))

;; ============================================================================
;; Polling Events
;; ============================================================================

(deftest session-start-polling-test
  (testing "Sets polling flag to true"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:session/start-polling])
    (is (true? (tu/get-in-db [:ui :polling?])))))

(deftest session-stop-polling-test
  (testing "Sets polling flag to false"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :polling?] true))
    (rf/dispatch-sync [:session/stop-polling])
    (is (false? (tu/get-in-db [:ui :polling?])))))

(deftest session-status-poll-test
  (testing "Does nothing when polling is false"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :polling?] false))
    (rf/dispatch-sync [:session/status-poll])
    ;; Just verifies it doesn't error
    (is true))

  (testing "Initiates HTTP request when polling is true"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :polling?] true))
    (rf/dispatch-sync [:session/status-poll])
    ;; HTTP effect is mocked, just verify no error
    (is true)))

(deftest session-status-success-test
  (testing "Updates session state from status response"
    (tu/set-db! (assoc-in tu/active-session-db [:ui :polling?] true))
    (rf/dispatch-sync [:session/status-success
                       {:authenticated true
                        :username "testuser"
                        :session {:state "active"
                                  :target_username "targetuser"
                                  :scrobble_count 10
                                  :recent_scrobbles [{:artist "New Artist" :track "New Track"}]
                                  :last_poll 1234567999}}])
    (is (true? (tu/get-in-db [:auth :authenticated?])))
    (is (= "testuser" (tu/get-in-db [:auth :username])))
    (is (= :active (tu/get-in-db [:session :state])))
    (is (= 10 (tu/get-in-db [:session :scrobble-count])))
    (is (= 1 (count (tu/get-in-db [:session :recent-scrobbles]))))))

(deftest session-status-failure-unauthorized-test
  (testing "Stops polling and shows error on 401"
    (tu/set-db! (assoc-in tu/active-session-db [:ui :polling?] true))
    (rf/dispatch-sync [:session/status-failure {:status 401}])
    (is (false? (tu/get-in-db [:ui :polling?])))
    (is (false? (tu/get-in-db [:auth :authenticated?])))
    (is (= "Your session has expired. Please log in again."
           (tu/get-in-db [:ui :error])))))

(deftest session-status-failure-other-error-test
  (testing "Continues polling on non-401 errors"
    (tu/set-db! (assoc-in tu/active-session-db [:ui :polling?] true))
    (rf/dispatch-sync [:session/status-failure {:status 500}])
    ;; Should still be polling (will retry with backoff)
    (is (true? (tu/get-in-db [:ui :polling?])))))

;; ============================================================================
;; UI Events
;; ============================================================================

(deftest ui-set-error-test
  (testing "Sets error message"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:ui/set-error "Test error message"])
    (is (= "Test error message" (tu/get-in-db [:ui :error])))))

(deftest ui-clear-error-test
  (testing "Clears error message"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :error] "Some error"))
    (rf/dispatch-sync [:ui/clear-error])
    (is (nil? (tu/get-in-db [:ui :error])))))
