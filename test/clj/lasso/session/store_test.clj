(ns lasso.session.store-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.session.store :as store]))

(use-fixtures :each
  (fn [f]
    (store/clear-all-sessions!)
    (f)
    (store/clear-all-sessions!)))

(deftest create-session-test
  (testing "Create a new session"
    (let [session (store/create-session "test-uuid" "testuser" "encrypted-key")]
      (is (= "test-uuid" (:session-id session)))
      (is (= "testuser" (:username session)))
      (is (= "encrypted-key" (:session-key session)))
      (is (number? (:created-at session)))
      (is (number? (:last-activity session)))
      (is (nil? (:following-session session))))))

(deftest get-session-test
  (testing "Get existing session"
    (store/create-session "test-uuid" "testuser" "encrypted-key")
    (let [session (store/get-session "test-uuid")]
      (is (some? session))
      (is (= "testuser" (:username session)))))

  (testing "Get non-existent session returns nil"
    (is (nil? (store/get-session "nonexistent")))))

(deftest update-session-test
  (testing "Update session data"
    (store/create-session "test-uuid" "testuser" "encrypted-key")
    (let [updated (store/update-session "test-uuid"
                                       (fn [session]
                                         (assoc session :username "newuser")))]
      (is (= "newuser" (:username updated)))
      (is (= "newuser" (:username (store/get-session "test-uuid"))))))

  (testing "Update non-existent session returns nil"
    (let [result (store/update-session "nonexistent"
                                      (fn [session]
                                        (assoc session :username "newuser")))]
      (is (nil? result)))))

(deftest delete-session-test
  (testing "Delete existing session"
    (store/create-session "test-uuid" "testuser" "encrypted-key")
    (is (some? (store/get-session "test-uuid")))
    (store/delete-session "test-uuid")
    (is (nil? (store/get-session "test-uuid")))))

(deftest touch-session-test
  (testing "Update last activity timestamp"
    (let [original (store/create-session "test-uuid" "testuser" "encrypted-key")
          original-time (:last-activity original)]
      (Thread/sleep 10)
      (store/touch-session "test-uuid")
      (let [touched (store/get-session "test-uuid")]
        (is (> (:last-activity touched) original-time))))))

(deftest get-active-following-sessions-test
  (testing "Returns empty list when no sessions"
    (is (empty? (store/get-active-following-sessions))))

  (testing "Returns empty list when no active following sessions"
    (store/create-session "test-uuid" "testuser" "encrypted-key")
    (is (empty? (store/get-active-following-sessions))))

  (testing "Returns active following sessions"
    (store/create-session "uuid-1" "user1" "key1")
    (store/update-session "uuid-1"
                         (fn [session]
                           (assoc session :following-session
                                  {:target-username "target1"
                                   :state :active
                                   :started-at (System/currentTimeMillis)})))
    (store/create-session "uuid-2" "user2" "key2")
    (store/update-session "uuid-2"
                         (fn [session]
                           (assoc session :following-session
                                  {:target-username "target2"
                                   :state :paused
                                   :started-at (System/currentTimeMillis)})))
    (let [active (store/get-active-following-sessions)]
      (is (= 1 (count active)))
      (is (= "user1" (:username (first active))))))

  (testing "Returns multiple active sessions"
    (store/create-session "uuid-1" "user1" "key1")
    (store/update-session "uuid-1"
                         (fn [session]
                           (assoc session :following-session
                                  {:target-username "target1"
                                   :state :active
                                   :started-at (System/currentTimeMillis)})))
    (store/create-session "uuid-2" "user2" "key2")
    (store/update-session "uuid-2"
                         (fn [session]
                           (assoc session :following-session
                                  {:target-username "target2"
                                   :state :active
                                   :started-at (System/currentTimeMillis)})))
    (let [active (store/get-active-following-sessions)]
      (is (= 2 (count active))))))

(deftest count-sessions-test
  (testing "Count starts at zero"
    (is (= 0 (store/count-sessions))))

  (testing "Count increases with sessions"
    (store/create-session "uuid-1" "user1" "key1")
    (is (= 1 (store/count-sessions)))
    (store/create-session "uuid-2" "user2" "key2")
    (is (= 2 (store/count-sessions))))

  (testing "Count decreases when session deleted"
    (store/create-session "uuid-1" "user1" "key1")
    (store/create-session "uuid-2" "user2" "key2")
    (is (= 2 (store/count-sessions)))
    (store/delete-session "uuid-1")
    (is (= 1 (store/count-sessions)))))

(deftest concurrent-access-test
  (testing "Concurrent session creation"
    (let [futures (doall (map (fn [i]
                               (future
                                 (store/create-session (str "uuid-" i)
                                                      (str "user-" i)
                                                      (str "key-" i))))
                             (range 10)))]
      (doseq [f futures] @f)
      (is (= 10 (store/count-sessions)))))

  (testing "Concurrent session updates"
    (store/create-session "test-uuid" "testuser" "encrypted-key")
    (let [futures (doall (map (fn [i]
                               (future
                                 (store/update-session "test-uuid"
                                                      (fn [session]
                                                        (update session :last-activity
                                                               (fnil inc 0))))))
                             (range 100)))]
      (doseq [f futures] @f)
      ;; All updates should complete without errors
      (is (some? (store/get-session "test-uuid"))))))
