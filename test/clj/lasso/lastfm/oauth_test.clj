(ns lasso.lastfm.oauth-test
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.lastfm.oauth :as oauth]
            [lasso.lastfm.client :as client]))

(deftest get-token-test
  (testing "Get token calls correct API method"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:token "test-token-123"})]
        (let [result (oauth/get-token)]
          (is (= 1 (count @requests)))
          (let [req (first @requests)]
            (is (= "auth.getToken" (:method req)))
            (is (true? (:signed req))))
          (is (= "test-token-123" (:token result))))))))

(deftest generate-auth-url-test
  (testing "Generate auth URL with token"
    (with-redefs [lasso.config/config {:lastfm {:api-key "test-api-key"}}]
      (let [url (oauth/generate-auth-url "my-token")]
        (is (string? url))
        (is (.startsWith url "https://www.last.fm/api/auth/"))
        (is (.contains url "api_key=test-api-key"))
        (is (.contains url "token=my-token"))))))

(deftest get-session-key-test
  (testing "Get session key calls correct API method"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:session {:name "testuser"
                                :key "session-key-123"}})]
        (let [result (oauth/get-session-key "authorized-token")]
          (is (= 1 (count @requests)))
          (let [req (first @requests)]
            (is (= "auth.getSession" (:method req)))
            (is (= "authorized-token" (get-in req [:params :token])))
            (is (true? (:signed req))))
          (is (= "testuser" (get-in result [:session :name])))
          (is (= "session-key-123" (get-in result [:session :key])))))))

  (testing "Get session key handles errors"
    (with-redefs [client/api-request
                  (fn [req]
                    {:error 14
                     :message "Unauthorized Token"})]
      (let [result (oauth/get-session-key "bad-token")]
        (is (= 14 (:error result)))
        (is (= "Unauthorized Token" (:message result)))))))
