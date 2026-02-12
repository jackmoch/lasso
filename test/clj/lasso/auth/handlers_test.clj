(ns lasso.auth.handlers-test
  "Tests for OAuth authentication handlers."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.auth.handlers :as handlers]
            [lasso.lastfm.oauth :as oauth]
            [lasso.auth.session :as auth-session]
            [lasso.session.store :as store]
            [lasso.util.http :as http]
            [clojure.data.json :as json]))

;; Test fixtures
(defn reset-sessions-fixture [f]
  "Reset session store before each test."
  (store/clear-all-sessions!)
  (f))

(use-fixtures :each reset-sessions-fixture)

;; Helper functions
(defn parse-json-body
  "Parse JSON body from response."
  [response]
  (when-let [body (:body response)]
    (json/read-str body :key-fn keyword)))

(defn extract-cookie
  "Extract a cookie value from Set-Cookie header.
   Set-Cookie can be either a string or a vector of strings."
  [response cookie-name]
  (when-let [set-cookie (get-in response [:headers "Set-Cookie"])]
    (let [cookie-strings (if (vector? set-cookie) set-cookie [set-cookie])
          matching-cookie (first (filter #(.contains % cookie-name) cookie-strings))]
      (when matching-cookie
        (let [parts (.split matching-cookie ";")
              cookie-part (first parts)
              [_ value] (.split cookie-part "=" 2)]
          value)))))

;; Tests for auth-init-handler
(deftest auth-init-handler-test
  (testing "successful OAuth initialization"
    (with-redefs [oauth/get-token (fn [] {:token "test-token-123"})]
      (let [request {}
            response (handlers/auth-init-handler request)
            body (parse-json-body response)]
        (is (= 200 (:status response)))
        (is (= "application/json" (get-in response [:headers "Content-Type"])))
        (is (string? (:auth_url body)))
        (is (.contains (:auth_url body) "test-token-123")))))

  (testing "OAuth token request fails"
    (with-redefs [oauth/get-token (fn [] {:error "API_ERROR"})]
      (let [request {}
            response (handlers/auth-init-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Failed to initiate authentication" (:error body)))
        (is (= "OAUTH_TOKEN_FAILED" (:error-code body))))))

  (testing "handles exceptions gracefully"
    (with-redefs [oauth/get-token (fn [] (throw (Exception. "Network error")))]
      (let [request {}
            response (handlers/auth-init-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Authentication initialization failed" (:error body)))
        (is (= "OAUTH_INIT_ERROR" (:error-code body)))))))

;; Tests for auth-callback-handler
(deftest auth-callback-handler-test
  (testing "successful OAuth callback with valid token"
    (with-redefs [oauth/get-session-key (fn [token]
                                          (is (= "authorized-token" token))
                                          {:session {:name "testuser"
                                                    :key "session-key-abc"}})]
      (let [request {:params {:token "authorized-token"}}
            response (handlers/auth-callback-handler request)]
        ;; Verify redirect to frontend root
        (is (= 302 (:status response)))
        (is (= "/" (get-in response [:headers "Location"])))
        ;; Verify session cookie was set
        (is (some? (extract-cookie response "session-id")))
        ;; Verify session exists in store
        (let [session-id (extract-cookie response "session-id")
              session (store/get-session session-id)]
          (is (some? session))
          (is (= "testuser" (:username session)))))))

  (testing "missing token parameter"
    (let [request {:params {}}
          response (handlers/auth-callback-handler request)
          body (parse-json-body response)]
      (is (= 400 (:status response)))
      (is (= "Missing token parameter" (:error body)))
      (is (= "MISSING_TOKEN" (:error-code body)))))

  (testing "OAuth session exchange fails"
    (with-redefs [oauth/get-session-key (fn [_] {:error "INVALID_TOKEN"})]
      (let [request {:params {:token "bad-token"}}
            response (handlers/auth-callback-handler request)
            body (parse-json-body response)]
        (is (= 401 (:status response)))
        (is (= "Authentication failed" (:error body)))
        (is (= "OAUTH_SESSION_FAILED" (:error-code body))))))

  (testing "handles exceptions gracefully"
    (with-redefs [oauth/get-session-key (fn [_] (throw (Exception. "Network error")))]
      (let [request {:params {:token "some-token"}}
            response (handlers/auth-callback-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Authentication callback failed" (:error body)))
        (is (= "OAUTH_CALLBACK_ERROR" (:error-code body)))))))

;; Tests for logout-handler
(deftest logout-handler-test
  (testing "successful logout"
    (let [;; Create a session first
          {:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request {:session {:session-id session-id
                            :username "testuser"}}
          response (handlers/logout-handler request)
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      ;; Verify cookie is cleared (max-age=0)
      (is (.contains (get-in response [:headers "Set-Cookie"]) "Max-Age=0"))
      ;; Verify session was destroyed
      (is (nil? (store/get-session session-id)))))

  (testing "handles exceptions gracefully"
    (with-redefs [auth-session/destroy-session (fn [_] (throw (Exception. "Database error")))]
      (let [request {:session {:session-id "some-id"}}
            response (handlers/logout-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Logout failed" (:error body)))
        (is (= "LOGOUT_ERROR" (:error-code body)))))))
