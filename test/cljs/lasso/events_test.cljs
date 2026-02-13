(ns lasso.events-test
  "Tests for Re-frame event handlers"
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [re-frame.core :as rf]
            [lasso.events]  ; Load event handlers
            [lasso.db :as db]
            [lasso.test-utils :as tu]))

;; Test fixtures - reset Re-frame before each test
(use-fixtures :each tu/with-fresh-db)

;; ============================================================================
;; Initialization Events
;; ============================================================================

(deftest initialize-db-test
  (testing "Initializes db with default state"
    (let [result (tu/dispatch-sync [:initialize-db])]
      (is (= db/default-db result))
      (is (false? (get-in result [:auth :authenticated?])))
      (is (= :not-started (get-in result [:session :state]))))))

(deftest check-auth-test
  (testing "Sets checking flag and makes API call"
    (tu/set-db! db/default-db)
    (rf/dispatch-sync [:check-auth])
    (is (true? (get-in (tu/test-db) [:auth :checking?])))
    ;; Note: HTTP effect would be tested with mock in integration tests
    ))

(deftest check-auth-success-test
  (testing "Updates auth state when authenticated"
    (tu/set-db! db/default-db)
    (let [response {:authenticated true
                    :username "testuser"
                    :session nil}
          result (tu/dispatch-sync [:check-auth-success response])]
      (is (false? (get-in result [:auth :checking?])))
      (is (true? (get-in result [:auth :authenticated?])))
      (is (= "testuser" (get-in result [:auth :username])))))

  (testing "Updates session state when session exists"
    (tu/set-db! db/default-db)
    (let [response {:authenticated true
                    :username "testuser"
                    :session {:state "active"
                              :target_username "targetuser"
                              :scrobble_count 5
                              :recent_scrobbles [{:artist "Artist 1" :track "Track 1"}]
                              :started_at 1234567890
                              :last_poll 1234567900}}
          result (tu/dispatch-sync [:check-auth-success response])]
      (is (= :active (get-in result [:session :state])))
      (is (= "targetuser" (get-in result [:session :target-username])))
      (is (= 5 (get-in result [:session :scrobble-count])))
      (is (= 1 (count (get-in result [:session :recent-scrobbles]))))))

  (testing "Does not update when not authenticated"
    (tu/set-db! db/default-db)
    (let [response {:authenticated false}
          result (tu/dispatch-sync [:check-auth-success response])]
      (is (false? (get-in result [:auth :authenticated?])))
      (is (nil? (get-in result [:auth :username]))))))

(deftest check-auth-failure-test
  (testing "Clears checking flag on failure"
    (tu/set-db! (assoc-in db/default-db [:auth :checking?] true))
    (let [result (tu/dispatch-sync [:check-auth-failure])]
      (is (false? (get-in result [:auth :checking?]))))))

;; ============================================================================
;; Authentication Events
;; ============================================================================

(deftest auth-init-failure-test
  (testing "Sets error message on auth init failure"
    (tu/set-db! db/default-db)
    (let [response {:response {:error_code "OAUTH_TOKEN_FAILED"
                              :message "Failed to get token"}}
          result (tu/dispatch-sync [:auth/init-failure response])]
      (is (= "Last.fm authentication failed. Please try again."
             (get-in result [:ui :error]))))))

(deftest auth-logout-test
  (testing "Sets loading state on logout"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:auth/logout])
    (is (true? (get-in (tu/test-db) [:ui :loading?])))))

(deftest auth-logout-success-test
  (testing "Resets db to default on logout success"
    (tu/set-db! tu/authenticated-db)
    (let [result (tu/dispatch-sync [:auth/logout-success])]
      (is (= db/default-db result)))))

(deftest auth-logout-failure-test
  (testing "Clears loading and sets error on logout failure"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :loading?] true))
    (let [response {:response {:message "Logout failed"}}
          result (tu/dispatch-sync [:auth/logout-failure response])]
      (is (false? (get-in result [:ui :loading?])))
      (is (= "Logout failed" (get-in result [:ui :error]))))))

;; ============================================================================
;; Session Management Events
;; ============================================================================

(deftest session-start-test
  (testing "Sets loading state on session start"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:session/start "targetuser"])
    (is (true? (get-in (tu/test-db) [:ui :loading?])))))

(deftest session-start-success-test
  (testing "Updates session state on start success"
    (tu/set-db! tu/authenticated-db)
    (let [response {:session {:state "active"
                              :target_username "targetuser"
                              :scrobble_count 0
                              :recent_scrobbles []
                              :started_at 1234567890}}
          result (tu/dispatch-sync [:session/start-success response])]
      (is (false? (get-in result [:ui :loading?])))
      (is (nil? (get-in result [:ui :error])))
      (is (= :active (get-in result [:session :state])))
      (is (= "targetuser" (get-in result [:session :target-username])))
      (is (= 0 (get-in result [:session :scrobble-count]))))))

(deftest session-start-failure-test
  (testing "Shows error on session start failure"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :loading?] true))
    (let [response {:response {:error_code "INVALID_TARGET_USERNAME"}}
          result (tu/dispatch-sync [:session/start-failure response])]
      (is (false? (get-in result [:ui :loading?])))
      (is (= "The username you entered doesn't exist on Last.fm."
             (get-in result [:ui :error]))))))

(deftest session-pause-test
  (testing "Sets loading state on pause"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:session/pause])
    (is (true? (get-in (tu/test-db) [:ui :loading?])))))

(deftest session-pause-success-test
  (testing "Updates state to paused on success"
    (tu/set-db! tu/active-session-db)
    (let [response {:session {:state "paused"
                              :target_username "targetuser"
                              :scrobble_count 5}}
          result (tu/dispatch-sync [:session/pause-success response])]
      (is (false? (get-in result [:ui :loading?])))
      (is (= :paused (get-in result [:session :state]))))))

(deftest session-resume-test
  (testing "Sets loading state on resume"
    (tu/set-db! tu/paused-session-db)
    (rf/dispatch-sync [:session/resume])
    (is (true? (get-in (tu/test-db) [:ui :loading?])))))

(deftest session-resume-success-test
  (testing "Updates state to active on resume success"
    (tu/set-db! tu/paused-session-db)
    (let [response {:session {:state "active"
                              :target_username "targetuser"
                              :scrobble_count 5}}
          result (tu/dispatch-sync [:session/resume-success response])]
      (is (false? (get-in result [:ui :loading?])))
      (is (= :active (get-in result [:session :state]))))))

(deftest session-stop-test
  (testing "Sets loading state on stop"
    (tu/set-db! tu/active-session-db)
    (rf/dispatch-sync [:session/stop])
    (is (true? (get-in (tu/test-db) [:ui :loading?])))))

(deftest session-stop-success-test
  (testing "Resets session state on stop success"
    (tu/set-db! tu/active-session-db)
    (let [result (tu/dispatch-sync [:session/stop-success])]
      (is (false? (get-in result [:ui :loading?])))
      (is (= :not-started (get-in result [:session :state])))
      (is (nil? (get-in result [:session :target-username])))
      (is (= 0 (get-in result [:session :scrobble-count])))
      (is (empty? (get-in result [:session :recent-scrobbles]))))))

;; ============================================================================
;; Session Status Polling Events
;; ============================================================================

(deftest session-status-update-test
  (testing "Updates session data from polling response"
    (tu/set-db! tu/active-session-db)
    (let [new-scrobbles [{:artist "New Artist"
                         :track "New Track"
                         :timestamp 1234567999}]
          response {:session {:state "active"
                              :target_username "targetuser"
                              :scrobble_count 6
                              :recent_scrobbles new-scrobbles
                              :last_poll 1234567999}}
          result (tu/dispatch-sync [:session/status-update response])]
      (is (= 6 (get-in result [:session :scrobble-count])))
      (is (= 1 (count (get-in result [:session :recent-scrobbles]))))
      (is (= "New Artist" (get-in result [:session :recent-scrobbles 0 :artist])))))

  (testing "Handles no new scrobbles"
    (tu/set-db! tu/active-session-db)
    (let [response {:session {:state "active"
                              :target_username "targetuser"
                              :scrobble_count 5
                              :recent_scrobbles []}}
          result (tu/dispatch-sync [:session/status-update response])]
      (is (= 5 (get-in result [:session :scrobble-count])))
      (is (empty? (get-in result [:session :recent-scrobbles]))))))

;; ============================================================================
;; UI Events
;; ============================================================================

(deftest clear-error-test
  (testing "Clears error message"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :error] "Some error"))
    (let [result (tu/dispatch-sync [:ui/clear-error])]
      (is (nil? (get-in result [:ui :error]))))))

;; ============================================================================
;; Error Message Helper Tests
;; ============================================================================

(deftest error-message-test
  (testing "Maps error codes to user-friendly messages"
    (is (= "Please enter a Last.fm username."
           (lasso.events/error-message
            {:response {:error_code "MISSING_TARGET_USERNAME"}})))

    (is (= "The username you entered doesn't exist on Last.fm."
           (lasso.events/error-message
            {:response {:error_code "INVALID_TARGET_USERNAME"}})))

    (is (= "Your session has expired. Please log in again."
           (lasso.events/error-message
            {:response {:error_code "SESSION_NOT_FOUND"}}))))

  (testing "Falls back to backend message when available"
    (is (= "Custom error message"
           (lasso.events/error-message
            {:response {:error_code "UNKNOWN_ERROR"
                        :message "Custom error message"}}))))

  (testing "Falls back to generic message when no code or message"
    (is (= "An error occurred. Please try again."
           (lasso.events/error-message
            {:response {}})))))
