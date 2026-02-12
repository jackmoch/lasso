(ns lasso.session.manager-test
  "Tests for session lifecycle management."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.session.manager :as manager]
            [lasso.session.store :as store]
            [lasso.auth.session :as auth-session]
            [lasso.lastfm.client :as lastfm]))

;; Test fixtures
(defn reset-sessions-fixture [f]
  "Reset session store before each test."
  (store/clear-all-sessions!)
  (f))

(use-fixtures :each reset-sessions-fixture)

;; Tests for validate-target-user
(deftest validate-target-user-test
  (testing "successful user validation"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "testuser"}})]
      (let [result (manager/validate-target-user "testuser")]
        (is (:valid? result))
        (is (= "testuser" (:username result))))))

  (testing "user not found"
    (with-redefs [lastfm/api-request (fn [_] {:error "User not found"})]
      (let [result (manager/validate-target-user "nonexistent")]
        (is (not (:valid? result)))
        (is (string? (:error result))))))

  (testing "API error"
    (with-redefs [lastfm/api-request (fn [_] (throw (Exception. "Network error")))]
      (let [result (manager/validate-target-user "someuser")]
        (is (not (:valid? result)))
        (is (= "Failed to validate user" (:error result)))))))

;; Tests for start-session
(deftest start-session-test
  (testing "successfully start new session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            result (manager/start-session session-id "targetuser")]
        (is (:success result))
        (let [following (get-in result [:session :following-session])]
          (is (= :active (:state following)))
          (is (= "targetuser" (:target-username following)))
          (is (= 0 (:scrobble-count following)))
          (is (set? (:scrobble-cache following)))
          (is (number? (:started-at following)))))))

  (testing "fail to start when target user invalid"
    (with-redefs [lastfm/api-request (fn [_] {:error "User not found"})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            result (manager/start-session session-id "invalid")]
        (is (not (:success result)))
        (is (string? (:error result))))))

  (testing "don't overwrite existing session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            ;; Start first session
            _ (manager/start-session session-id "targetuser")
            ;; Try to start another
            result (manager/start-session session-id "anotheruser")]
        ;; Should still have first session
        (is (:success result))
        (is (= "targetuser" (get-in result [:session :following-session :target-username]))))))

  (testing "session not found"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [result (manager/start-session "nonexistent-session-id" "targetuser")]
        (is (not (:success result)))
        (is (= "Session not found" (:error result)))))))

;; Tests for pause-session
(deftest pause-session-test
  (testing "successfully pause active session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            result (manager/pause-session session-id)]
        (is (:success result))
        (is (= :paused (get-in result [:session :following-session :state]))))))

  (testing "can't pause when no session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          result (manager/pause-session session-id)]
      (is (not (:success result)))
      (is (= "Session is not active" (:error result)))))

  (testing "can't pause when already paused"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            _ (manager/pause-session session-id)
            result (manager/pause-session session-id)]
        (is (not (:success result)))
        (is (= "Session is not active" (:error result))))))

  (testing "session not found"
    (let [result (manager/pause-session "nonexistent-id")]
      (is (not (:success result)))
      (is (= "Session not found" (:error result))))))

;; Tests for resume-session
(deftest resume-session-test
  (testing "successfully resume paused session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            _ (manager/pause-session session-id)
            result (manager/resume-session session-id)]
        (is (:success result))
        (is (= :active (get-in result [:session :following-session :state]))))))

  (testing "can't resume when not paused"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            result (manager/resume-session session-id)]
        (is (not (:success result)))
        (is (= "Session is not paused" (:error result))))))

  (testing "can't resume when no session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          result (manager/resume-session session-id)]
      (is (not (:success result)))
      (is (= "Session is not paused" (:error result)))))

  (testing "session not found"
    (let [result (manager/resume-session "nonexistent-id")]
      (is (not (:success result)))
      (is (= "Session not found" (:error result))))))

;; Tests for stop-session
(deftest stop-session-test
  (testing "successfully stop active session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            result (manager/stop-session session-id)]
        (is (:success result))
        (is (nil? (get-in result [:session :following-session]))))))

  (testing "stop when no session (no-op)"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          result (manager/stop-session session-id)]
      (is (:success result))))

  (testing "session not found"
    (let [result (manager/stop-session "nonexistent-id")]
      (is (not (:success result)))
      (is (= "Session not found" (:error result))))))

;; Tests for get-session-status
(deftest get-session-status-test
  (testing "status with active session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            status (manager/get-session-status session-id)]
        (is (:authenticated status))
        (is (= "testuser" (:username status)))
        (is (= :active (get-in status [:session :state])))
        (is (= "targetuser" (get-in status [:session :target_username])))
        (is (= 0 (get-in status [:session :scrobble_count]))))))

  (testing "status without following session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          status (manager/get-session-status session-id)]
      (is (:authenticated status))
      (is (= "testuser" (:username status)))
      (is (= :not-started (get-in status [:session :state])))
      (is (nil? (get-in status [:session :target_username])))))

  (testing "status when not authenticated"
    (let [status (manager/get-session-status "nonexistent-id")]
      (is (not (:authenticated status)))
      (is (nil? (:username status)))
      (is (nil? (:session status))))))

;; Tests for can-start-session?
(deftest can-start-session-test
  (testing "can start when no session exists"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")]
      (is (manager/can-start-session? session-id))))

  (testing "cannot start when session exists"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")]
        (is (not (manager/can-start-session? session-id))))))

  (testing "cannot start for nonexistent session-id"
    (is (not (manager/can-start-session? "nonexistent-id")))))
