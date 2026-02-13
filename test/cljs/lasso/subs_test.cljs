(ns lasso.subs-test
  "Tests for Re-frame subscriptions"
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [lasso.subs]  ; Load subscriptions
            [lasso.test-utils :as tu]))

;; Test fixtures - reset Re-frame before each test
(use-fixtures :each tu/with-fresh-db)

;; ============================================================================
;; Layer 2: Simple Subscriptions
;; ============================================================================

(deftest auth-subscriptions-test
  (testing "auth/authenticated? subscription"
    (tu/set-db! tu/authenticated-db)
    (is (true? (tu/subscribe-value [:auth/authenticated?])))

    (tu/set-db! tu/empty-db)
    (is (nil? (tu/subscribe-value [:auth/authenticated?]))))

  (testing "auth/username subscription"
    (tu/set-db! tu/authenticated-db)
    (is (= "testuser" (tu/subscribe-value [:auth/username])))

    (tu/set-db! tu/empty-db)
    (is (nil? (tu/subscribe-value [:auth/username]))))

  (testing "auth/checking? subscription"
    (tu/set-db! (assoc-in tu/empty-db [:auth :checking?] true))
    (is (true? (tu/subscribe-value [:auth/checking?])))

    (tu/set-db! tu/empty-db)
    (is (nil? (tu/subscribe-value [:auth/checking?])))))

(deftest session-subscriptions-test
  (testing "session/state subscription"
    (tu/set-db! tu/authenticated-db)
    (is (= :not-started (tu/subscribe-value [:session/state])))

    (tu/set-db! tu/active-session-db)
    (is (= :active (tu/subscribe-value [:session/state])))

    (tu/set-db! tu/paused-session-db)
    (is (= :paused (tu/subscribe-value [:session/state]))))

  (testing "session/target-username subscription"
    (tu/set-db! tu/active-session-db)
    (is (= "target-user" (tu/subscribe-value [:session/target-username])))

    (tu/set-db! tu/authenticated-db)
    (is (nil? (tu/subscribe-value [:session/target-username]))))

  (testing "session/scrobble-count subscription"
    (tu/set-db! tu/active-session-db)
    (is (= 5 (tu/subscribe-value [:session/scrobble-count])))

    (tu/set-db! tu/authenticated-db)
    (is (= 0 (tu/subscribe-value [:session/scrobble-count]))))

  (testing "session/recent-scrobbles subscription"
    (tu/set-db! tu/active-session-db)
    (let [scrobbles (tu/subscribe-value [:session/recent-scrobbles])]
      (is (= 2 (count scrobbles)))
      (is (= "Artist 1" (:artist (first scrobbles)))))

    (tu/set-db! tu/authenticated-db)
    (is (empty? (tu/subscribe-value [:session/recent-scrobbles]))))

  (testing "session/started-at subscription"
    (tu/set-db! (assoc-in tu/active-session-db [:session :started-at] 1234567890))
    (is (= 1234567890 (tu/subscribe-value [:session/started-at]))))

  (testing "session/last-poll subscription"
    (tu/set-db! (assoc-in tu/active-session-db [:session :last-poll] 1234567900))
    (is (= 1234567900 (tu/subscribe-value [:session/last-poll])))))

(deftest ui-subscriptions-test
  (testing "ui/error subscription"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :error] "Test error"))
    (is (= "Test error" (tu/subscribe-value [:ui/error])))

    (tu/set-db! tu/authenticated-db)
    (is (nil? (tu/subscribe-value [:ui/error]))))

  (testing "ui/loading? subscription"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :loading?] true))
    (is (true? (tu/subscribe-value [:ui/loading?])))

    (tu/set-db! tu/authenticated-db)
    (is (false? (tu/subscribe-value [:ui/loading?]))))

  (testing "ui/session-control-loading? subscription"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :session-control-loading?] true))
    (is (true? (tu/subscribe-value [:ui/session-control-loading?])))

    (tu/set-db! tu/authenticated-db)
    (is (nil? (tu/subscribe-value [:ui/session-control-loading?]))))

  (testing "ui/polling? subscription"
    (tu/set-db! (assoc-in tu/authenticated-db [:ui :polling?] true))
    (is (true? (tu/subscribe-value [:ui/polling?])))

    (tu/set-db! tu/authenticated-db)
    (is (nil? (tu/subscribe-value [:ui/polling?])))))

;; ============================================================================
;; Layer 3: Computed Subscriptions
;; ============================================================================

(deftest session-can-start-test
  (testing "Can start when authenticated and not started"
    (tu/set-db! tu/authenticated-db)
    (is (true? (tu/subscribe-value [:session/can-start?]))))

  (testing "Cannot start when not authenticated"
    (tu/set-db! (assoc-in tu/authenticated-db [:auth :authenticated?] false))
    (is (false? (tu/subscribe-value [:session/can-start?]))))

  (testing "Cannot start when session is active"
    (tu/set-db! tu/active-session-db)
    (is (false? (tu/subscribe-value [:session/can-start?]))))

  (testing "Cannot start when session is paused"
    (tu/set-db! tu/paused-session-db)
    (is (false? (tu/subscribe-value [:session/can-start?])))))

(deftest session-can-pause-test
  (testing "Can pause when session is active"
    (tu/set-db! tu/active-session-db)
    (is (true? (tu/subscribe-value [:session/can-pause?]))))

  (testing "Cannot pause when session is not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (tu/subscribe-value [:session/can-pause?]))))

  (testing "Cannot pause when session is already paused"
    (tu/set-db! tu/paused-session-db)
    (is (false? (tu/subscribe-value [:session/can-pause?])))))

(deftest session-can-resume-test
  (testing "Can resume when session is paused"
    (tu/set-db! tu/paused-session-db)
    (is (true? (tu/subscribe-value [:session/can-resume?]))))

  (testing "Cannot resume when session is not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (tu/subscribe-value [:session/can-resume?]))))

  (testing "Cannot resume when session is active"
    (tu/set-db! tu/active-session-db)
    (is (false? (tu/subscribe-value [:session/can-resume?])))))

(deftest session-can-stop-test
  (testing "Can stop when session is active"
    (tu/set-db! tu/active-session-db)
    (is (true? (tu/subscribe-value [:session/can-stop?]))))

  (testing "Can stop when session is paused"
    (tu/set-db! tu/paused-session-db)
    (is (true? (tu/subscribe-value [:session/can-stop?]))))

  (testing "Cannot stop when session is not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (tu/subscribe-value [:session/can-stop?])))))

(deftest session-is-active-test
  (testing "Is active when session state is :active"
    (tu/set-db! tu/active-session-db)
    (is (true? (tu/subscribe-value [:session/is-active?]))))

  (testing "Is active when session state is :paused"
    (tu/set-db! tu/paused-session-db)
    (is (true? (tu/subscribe-value [:session/is-active?]))))

  (testing "Is not active when session is not started"
    (tu/set-db! tu/authenticated-db)
    (is (false? (tu/subscribe-value [:session/is-active?])))))

(deftest session-recent-scrobbles-formatted-test
  (testing "Formats scrobbles with time strings"
    (let [scrobbles [{:artist "Artist 1"
                      :track "Track 1"
                      :timestamp 1234567890}
                     {:artist "Artist 2"
                      :track "Track 2"
                      :timestamp 1234567900}]
          db (assoc-in tu/authenticated-db [:session :recent-scrobbles] scrobbles)]
      (tu/set-db! db)
      (let [formatted (tu/subscribe-value [:session/recent-scrobbles-formatted])]
        (is (= 2 (count formatted)))
        (is (= "Artist 1" (:artist (first formatted))))
        (is (some? (:formatted-time (first formatted))))
        ;; Formatted time should be a string like "05:31 PM"
        (is (string? (:formatted-time (first formatted)))))))

  (testing "Handles empty scrobbles list"
    (tu/set-db! tu/authenticated-db)
    (let [formatted (tu/subscribe-value [:session/recent-scrobbles-formatted])]
      (is (empty? formatted))))

  (testing "Handles scrobbles with nil timestamp"
    (let [scrobbles [{:artist "Artist 1"
                      :track "Track 1"
                      :timestamp nil}]
          db (assoc-in tu/authenticated-db [:session :recent-scrobbles] scrobbles)]
      (tu/set-db! db)
      (let [formatted (tu/subscribe-value [:session/recent-scrobbles-formatted])]
        (is (= 1 (count formatted)))
        (is (nil? (:formatted-time (first formatted))))))))
