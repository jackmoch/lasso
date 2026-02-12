(ns lasso.auth.session-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.auth.session :as auth-session]
            [lasso.session.store :as store]))

(use-fixtures :each
  (fn [f]
    (store/clear-all-sessions!)
    (f)
    (store/clear-all-sessions!)))

(deftest create-session-test
  (testing "Create session with encryption"
    (let [result (auth-session/create-session "testuser" "my-lastfm-key")
          session-id (:session-id result)
          session-data (:session-data result)]
      (is (string? session-id))
      (is (= "testuser" (:username session-data)))
      (is (not= "my-lastfm-key" (:session-key session-data)) "Key should be encrypted")
      (is (string? (:session-key session-data))))))

(deftest destroy-session-test
  (testing "Destroy removes session"
    (let [result (auth-session/create-session "testuser" "my-lastfm-key")
          session-id (:session-id result)]
      (is (some? (store/get-session session-id)))
      (auth-session/destroy-session session-id)
      (is (nil? (store/get-session session-id))))))

(deftest get-session-id-test
  (testing "Extract session ID from cookie header"
    (let [request {:headers {"cookie" "session-id=abc123; other=value"}}]
      (is (= "abc123" (auth-session/get-session-id request)))))

  (testing "Returns nil when cookie not present"
    (let [request {:headers {}}]
      (is (nil? (auth-session/get-session-id request))))))

(deftest encrypt-decrypt-session-key-test
  (testing "Encrypt and decrypt roundtrip"
    (let [original "my-lastfm-session-key"
          encrypted (auth-session/encrypt-session-key original)
          decrypted (auth-session/decrypt-session-key encrypted)]
      (is (not= original encrypted) "Should be encrypted")
      (is (= original decrypted) "Should decrypt to original"))))

(deftest get-decrypted-session-key-test
  (testing "Get decrypted key from session"
    (let [result (auth-session/create-session "testuser" "my-lastfm-key")
          session-id (:session-id result)
          decrypted (auth-session/get-decrypted-session-key session-id)]
      (is (= "my-lastfm-key" decrypted))))

  (testing "Returns nil for non-existent session"
    (is (nil? (auth-session/get-decrypted-session-key "nonexistent")))))

(deftest update-session-test
  (testing "Update session through auth module"
    (let [result (auth-session/create-session "testuser" "my-lastfm-key")
          session-id (:session-id result)
          updated (auth-session/update-session session-id
                                              (fn [session]
                                                (assoc session :custom-field "value")))]
      (is (= "value" (:custom-field updated)))
      (is (= "value" (:custom-field (store/get-session session-id)))))))

(deftest touch-test
  (testing "Touch updates last activity"
    (let [result (auth-session/create-session "testuser" "my-lastfm-key")
          session-id (:session-id result)
          original-time (:last-activity (:session-data result))]
      (Thread/sleep 10)
      (auth-session/touch session-id)
      (let [session (store/get-session session-id)]
        (is (> (:last-activity session) original-time))))))
