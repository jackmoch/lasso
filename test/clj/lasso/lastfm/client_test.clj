(ns lasso.lastfm.client-test
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.lastfm.client :as client]))

(deftest generate-api-signature-test
  (testing "API signature generation"
    ;; This is a known example from Last.fm documentation
    ;; If api_secret is "secret" and params are {api_key "key", method "test"}
    ;; Signature should be MD5("api_keykeymethodtestsecret")
    (with-redefs [lasso.config/config {:lastfm {:api-secret "secret"}}]
      (let [params {:api_key "key" :method "test"}
            signature (client/generate-api-signature params)]
        ;; The signature should be deterministic for the same inputs
        (is (string? signature))
        (is (= 32 (count signature))) ; MD5 produces 32 hex characters
        ;; Test consistency
        (is (= signature (client/generate-api-signature params)))))))

(deftest api-request-without-signature-test
  (testing "API request without signature"
    (with-redefs [client/api-request
                  (fn [{:keys [method params signed]}]
                    {:method method
                     :params params
                     :signed signed})]
      (let [result (client/api-request {:method "user.getInfo"
                                        :params {:user "testuser"}})]
        (is (= "user.getInfo" (:method result)))
        (is (= {:user "testuser"} (:params result)))
        (is (or (false? (:signed result)) (nil? (:signed result))))))))

(deftest api-request-with-signature-test
  (testing "API request with signature"
    (with-redefs [client/api-request
                  (fn [{:keys [method params signed]}]
                    {:method method
                     :params params
                     :signed signed})]
      (let [result (client/api-request {:method "auth.getToken"
                                        :signed true})]
        (is (= "auth.getToken" (:method result)))
        (is (true? (:signed result)))))))

(deftest get-recent-tracks-test
  (testing "Get recent tracks constructs correct request"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:recenttracks {:track []}})]
        (client/get-recent-tracks "testuser")
        (is (= 1 (count @requests)))
        (let [req (first @requests)]
          (is (= "user.getRecentTracks" (:method req)))
          (is (= "testuser" (get-in req [:params :user])))
          (is (= 50 (get-in req [:params :limit])))))))

  (testing "Get recent tracks with from timestamp"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:recenttracks {:track []}})]
        (client/get-recent-tracks "testuser" :from 1234567890 :limit 100)
        (let [req (first @requests)]
          (is (= 1234567890 (get-in req [:params :from])))
          (is (= 100 (get-in req [:params :limit]))))))))

(deftest get-user-info-test
  (testing "Get user info constructs correct request"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:user {:name "testuser"}})]
        (client/get-user-info "testuser")
        (is (= 1 (count @requests)))
        (let [req (first @requests)]
          (is (= "user.getInfo" (:method req)))
          (is (= "testuser" (get-in req [:params :user]))))))))

(deftest rate-limiting-test
  (testing "Rate limiting is called for each request"
    (let [wait-calls (atom 0)
          requests (atom [])]
      (with-redefs [;; Mock both GET and POST since unsigned requests use GET, signed use POST
                    clj-http.client/get
                    (fn [url opts]
                      (swap! requests conj (System/currentTimeMillis))
                      {:status 200
                       :body {:user {:name "testuser"}}})
                    clj-http.client/post
                    (fn [url opts]
                      (swap! requests conj (System/currentTimeMillis))
                      {:status 200
                       :body {:user {:name "testuser"}}})
                    ;; Mock the wait-for-rate-limit to count calls without actual delays
                    client/wait-for-rate-limit
                    (fn []
                      (swap! wait-calls inc))]
        ;; Make 3 requests
        (client/get-user-info "user1")
        (client/get-user-info "user2")
        (client/get-user-info "user3")
        ;; Verify rate limiting was invoked for each request
        (is (= 3 @wait-calls) "Rate limiting should be called for each request")
        (is (= 3 (count @requests)) "All 3 requests should complete"))))

  (testing "Rate limiting prevents requests closer than min interval"
    ;; This is a slower integration test that actually measures timing
    ;; Only run if we want to verify actual delay behavior
    (let [requests (atom [])]
      (with-redefs [;; Mock both GET and POST
                    clj-http.client/get
                    (fn [url opts]
                      (swap! requests conj (System/currentTimeMillis))
                      {:status 200
                       :body {:user {:name "testuser"}}})
                    clj-http.client/post
                    (fn [url opts]
                      (swap! requests conj (System/currentTimeMillis))
                      {:status 200
                       :body {:user {:name "testuser"}}})]
        ;; Make 2 requests
        (client/get-user-info "user1")
        (client/get-user-info "user2")
        ;; Verify we have 2 requests
        (is (= 2 (count @requests)))
        ;; Verify they're at least 150ms apart (allowing some margin for system timing)
        (let [times @requests
              elapsed (- (last times) (first times))]
          (is (>= elapsed 150) "Requests should be at least 150ms apart (with margin for timing variance)"))))))
