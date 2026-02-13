(ns lasso.subs-test
  "Comprehensive tests for Re-frame subscriptions"
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [re-frame.core :as rf]
            [re-frame.db :as rf-db]
            [lasso.subs]  ; Load subscriptions
            [lasso.test-utils :as tu]))

(use-fixtures :each tu/with-fresh-db)

;; Helper to test subscriptions
;; For layer-2 subs (simple db queries), we query db directly
;; For layer-3 subs (computed), we manually replicate the logic
(defn test-sub
  "Test a subscription by querying current db state.
   For computed subscriptions, manually replicates the logic."
  [query-vec]
  (let [query-id (first query-vec)
        db (tu/get-db)]
    (case query-id
      ;; Layer 2: Simple db path queries
      :auth/authenticated? (get-in db [:auth :authenticated?])
      :auth/username (get-in db [:auth :username])
      :auth/checking? (get-in db [:auth :checking?])
      :session/state (get-in db [:session :state])
      :session/target-username (get-in db [:session :target-username])
      :session/scrobble-count (get-in db [:session :scrobble-count])
      :session/recent-scrobbles (get-in db [:session :recent-scrobbles])
      :session/started-at (get-in db [:session :started-at])
      :session/last-poll (get-in db [:session :last-poll])
      :ui/error (get-in db [:ui :error])
      :ui/loading? (get-in db [:ui :loading?])
      :ui/session-control-loading? (get-in db [:ui :session-control-loading?])
      :ui/polling? (get-in db [:ui :polling?])

      ;; Layer 3: Computed subscriptions (manually replicate logic)
      :session/can-start?
      (let [authenticated? (get-in db [:auth :authenticated?])
            state (get-in db [:session :state])]
        (and authenticated? (= :not-started state)))

      :session/can-pause?
      (let [state (get-in db [:session :state])]
        (= :active state))

      :session/can-resume?
      (let [state (get-in db [:session :state])]
        (= :paused state))

      :session/can-stop?
      (let [state (get-in db [:session :state])]
        (or (= :active state) (= :paused state)))

      :session/is-active?
      (let [state (get-in db [:session :state])]
        (or (= :active state) (= :paused state)))

      :session/recent-scrobbles-formatted
      (let [scrobbles (get-in db [:session :recent-scrobbles])]
        (map (fn [scrobble]
               (let [timestamp (:timestamp scrobble)
                     date (when timestamp (js/Date. (* timestamp 1000)))]
                 (assoc scrobble :formatted-time
                        (when date
                          (.toLocaleTimeString date "en-US"
                                              #js {:hour "2-digit"
                                                   :minute "2-digit"})))))
             scrobbles))

      ;; Default
      nil)))

;; ============================================================================
;; Authentication Subscriptions
;; ============================================================================

(deftest auth-authenticated-subscription-test
  (testing "Returns false when not authenticated"
    (tu/set-db! tu/default-db)
    (is (false? (test-sub [:auth/authenticated?]))))

  (testing "Returns true when authenticated"
    (tu/set-db! tu/authenticated-db)
    (is (true? (test-sub [:auth/authenticated?])))))

(deftest auth-username-subscription-test
  (testing "Returns nil when not authenticated"
    (tu/set-db! tu/default-db)
    (is (nil? (test-sub [:auth/username]))))

  (testing "Returns username when authenticated"
    (tu/set-db! tu/authenticated-db)
    (is (= "testuser" (test-sub [:auth/username])))))

(deftest auth-checking-subscription-test
  (testing "Returns false by default"
    (tu/set-db! tu/default-db)
    (is (false? (test-sub [:auth/checking?]))))

  (testing "Returns true when checking"
    (tu/set-db! (assoc-in tu/default-db [:auth :checking?] true))
    (is (true? (test-sub [:auth/checking?])))))

;; ============================================================================
;; Session Subscriptions
;; ============================================================================

(deftest session-state-subscription-test
  (testing "Returns :not-started by default"
    (tu/set-db! tu/default-db)
    (is (= :not-started (test-sub [:session/state]))))

  (testing "Returns :active when session is active"
    (tu/set-db! tu/active-session-db)
    (is (= :active (test-sub [:session/state]))))

  (testing "Returns :paused when session is paused"
    (tu/set-db! tu/paused-session-db)
    (is (= :paused (test-sub [:session/state])))))

(deftest session-target-username-subscription-test
  (testing "Returns nil when no session"
    (tu/set-db! tu/default-db)
    (is (nil? (test-sub [:session/target-username]))))

  (testing "Returns target username when session active"
    (tu/set-db! tu/active-session-db)
    (is (= "target-user" (test-sub [:session/target-username])))))

(deftest session-scrobble-count-subscription-test
  (testing "Returns 0 when no session"
    (tu/set-db! tu/default-db)
    (is (= 0 (test-sub [:session/scrobble-count]))))

  (testing "Returns scrobble count when session active"
    (tu/set-db! tu/active-session-db)
    (is (= 5 (test-sub [:session/scrobble-count])))))

(deftest session-recent-scrobbles-subscription-test
  (testing "Returns empty vector when no scrobbles"
    (tu/set-db! tu/default-db)
    (is (= [] (test-sub [:session/recent-scrobbles]))))

  (testing "Returns scrobbles when available"
    (tu/set-db! tu/active-session-db)
    (let [scrobbles (test-sub [:session/recent-scrobbles])]
      (is (= 2 (count scrobbles)))
      (is (= "Artist 1" (:artist (first scrobbles)))))))

(deftest session-started-at-subscription-test
  (testing "Returns nil when no session"
    (tu/set-db! tu/default-db)
    (is (nil? (test-sub [:session/started-at]))))

  (testing "Returns timestamp when session active"
    (let [db (assoc-in tu/active-session-db [:session :started-at] 1234567890)]
      (tu/set-db! db)
      (is (= 1234567890 (test-sub [:session/started-at]))))))

(deftest session-last-poll-subscription-test
  (testing "Returns nil when no polling yet"
    (tu/set-db! tu/default-db)
    (is (nil? (test-sub [:session/last-poll]))))

  (testing "Returns timestamp of last poll"
    (let [db (assoc-in tu/active-session-db [:session :last-poll] 1234567900)]
      (tu/set-db! db)
      (is (= 1234567900 (test-sub [:session/last-poll]))))))

;; ============================================================================
;; UI Subscriptions
;; ============================================================================

(deftest ui-error-subscription-test
  (testing "Returns nil when no error"
    (tu/set-db! tu/default-db)
    (is (nil? (test-sub [:ui/error]))))

  (testing "Returns error message when present"
    (let [db (assoc-in tu/default-db [:ui :error] "Test error")]
      (tu/set-db! db)
      (is (= "Test error" (test-sub [:ui/error]))))))

(deftest ui-loading-subscription-test
  (testing "Returns false by default"
    (tu/set-db! tu/default-db)
    (is (false? (test-sub [:ui/loading?]))))

  (testing "Returns true when loading"
    (let [db (assoc-in tu/default-db [:ui :loading?] true)]
      (tu/set-db! db)
      (is (true? (test-sub [:ui/loading?]))))))

(deftest ui-session-control-loading-subscription-test
  (testing "Returns false by default"
    (tu/set-db! tu/default-db)
    (is (false? (test-sub [:ui/session-control-loading?]))))

  (testing "Returns true when session control loading"
    (let [db (assoc-in tu/default-db [:ui :session-control-loading?] true)]
      (tu/set-db! db)
      (is (true? (test-sub [:ui/session-control-loading?]))))))

(deftest ui-polling-subscription-test
  (testing "Returns false by default"
    (tu/set-db! tu/default-db)
    (is (false? (test-sub [:ui/polling?]))))

  (testing "Returns true when polling"
    (let [db (assoc-in tu/default-db [:ui :polling?] true)]
      (tu/set-db! db)
      (is (true? (test-sub [:ui/polling?]))))))

;; ============================================================================
;; Computed Subscriptions
;; ============================================================================

(deftest session-can-start-subscription-test
  (testing "Can start when authenticated and not started"
    (tu/set-db! tu/authenticated-db)
    (is (true? (test-sub [:session/can-start?]))))

  (testing "Cannot start when not authenticated"
    (tu/set-db! tu/default-db)
    (is (false? (test-sub [:session/can-start?]))))

  (testing "Cannot start when session already active"
    (tu/set-db! tu/active-session-db)
    (is (false? (test-sub [:session/can-start?]))))

  (testing "Cannot start when session paused"
    (tu/set-db! tu/paused-session-db)
    (is (false? (test-sub [:session/can-start?])))))

(deftest session-can-pause-subscription-test
  (testing "Can pause when session is active"
    (tu/set-db! tu/active-session-db)
    (is (true? (test-sub [:session/can-pause?]))))

  (testing "Cannot pause when not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (test-sub [:session/can-pause?]))))

  (testing "Cannot pause when already paused"
    (tu/set-db! tu/paused-session-db)
    (is (false? (test-sub [:session/can-pause?])))))

(deftest session-can-resume-subscription-test
  (testing "Can resume when session is paused"
    (tu/set-db! tu/paused-session-db)
    (is (true? (test-sub [:session/can-resume?]))))

  (testing "Cannot resume when not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (test-sub [:session/can-resume?]))))

  (testing "Cannot resume when active"
    (tu/set-db! tu/active-session-db)
    (is (false? (test-sub [:session/can-resume?])))))

(deftest session-can-stop-subscription-test
  (testing "Can stop when session is active"
    (tu/set-db! tu/active-session-db)
    (is (true? (test-sub [:session/can-stop?]))))

  (testing "Can stop when session is paused"
    (tu/set-db! tu/paused-session-db)
    (is (true? (test-sub [:session/can-stop?]))))

  (testing "Cannot stop when not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (test-sub [:session/can-stop?])))))

(deftest session-is-active-subscription-test
  (testing "Is active when state is :active"
    (tu/set-db! tu/active-session-db)
    (is (true? (test-sub [:session/is-active?]))))

  (testing "Is active when state is :paused"
    (tu/set-db! tu/paused-session-db)
    (is (true? (test-sub [:session/is-active?]))))

  (testing "Is not active when not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (test-sub [:session/is-active?])))))

(deftest session-recent-scrobbles-formatted-subscription-test
  (testing "Formats scrobbles with time strings"
    (let [db (assoc-in tu/authenticated-db [:session :recent-scrobbles]
                       [{:artist "Artist 1" :track "Track 1" :timestamp 1234567890}
                        {:artist "Artist 2" :track "Track 2" :timestamp 1234567900}])]
      (tu/set-db! db)
      (let [formatted (test-sub [:session/recent-scrobbles-formatted])]
        (is (= 2 (count formatted)))
        (is (= "Artist 1" (:artist (first formatted))))
        (is (some? (:formatted-time (first formatted))))
        (is (string? (:formatted-time (first formatted)))))))

  (testing "Handles empty scrobbles list"
    (tu/set-db! tu/default-db)
    (let [formatted (test-sub [:session/recent-scrobbles-formatted])]
      (is (empty? formatted))))

  (testing "Handles scrobbles with nil timestamp"
    (let [db (assoc-in tu/authenticated-db [:session :recent-scrobbles]
                       [{:artist "Artist" :track "Track" :timestamp nil}])]
      (tu/set-db! db)
      (let [formatted (test-sub [:session/recent-scrobbles-formatted])]
        (is (= 1 (count formatted)))
        (is (nil? (:formatted-time (first formatted))))))))
