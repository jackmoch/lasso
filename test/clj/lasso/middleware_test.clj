(ns lasso.middleware-test
  "Tests for Pedestal interceptors."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.middleware :as mw]
            [lasso.auth.session :as auth-session]
            [lasso.session.store :as store]
            [lasso.user.remember :as remember]
            [lasso.firestore.client :as fs]
            [lasso.util.crypto :as crypto]
            [lasso.config :as config]
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

(defn make-request
  "Helper to create a test request with optional cookies."
  [& {:keys [session-id remember-token]}]
  (let [cookies (cond-> []
                  session-id     (conj (str "session-id=" session-id))
                  remember-token (conj (str "lasso-remember=" remember-token)))]
    (cond-> {}
      (seq cookies) (assoc-in [:headers "cookie"] (clojure.string/join "; " cookies)))))

;; Tests for require-auth interceptor
(deftest require-auth-interceptor-test
  (testing "allows request with valid session cookie"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request  (make-request :session-id session-id)
          context  {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result   (enter-fn context)]
      ;; Should attach session to request within context
      (is (some? (get-in result [:request :session])))
      (is (= "testuser" (get-in result [:request :session :username])))
      (is (= session-id (get-in result [:request :session :session-id])))
      ;; Should NOT set response (allow request to continue)
      (is (nil? (:response result)))))

  (testing "rejects request with missing session cookie and no remember-me"
    (with-redefs [fs/enabled? (fn [] false)]
      (let [request  (make-request)
            context  {:request request}
            enter-fn (get-in mw/require-auth [:enter])
            result   (enter-fn context)
            response (:response result)
            body     (parse-json-body response)]
        (is (some? response))
        (is (= 401 (:status response)))
        (is (= "Authentication required" (:error body)))
        (is (= "AUTH_REQUIRED" (:error_code body))))))

  (testing "rejects request with invalid session ID and no remember-me"
    (with-redefs [fs/enabled? (fn [] false)]
      (let [request  (make-request :session-id "invalid-session-id")
            context  {:request request}
            enter-fn (get-in mw/require-auth [:enter])
            result   (enter-fn context)
            response (:response result)
            body     (parse-json-body response)]
        (is (some? response))
        (is (= 401 (:status response)))
        (is (= "Authentication required" (:error body)))
        (is (= "AUTH_REQUIRED" (:error_code body))))))

  (testing "rejects request with expired session and no remember-me"
    (with-redefs [fs/enabled? (fn [] false)]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (store/delete-session session-id)
            request  (make-request :session-id session-id)
            context  {:request request}
            enter-fn (get-in mw/require-auth [:enter])
            result   (enter-fn context)
            response (:response result)
            body     (parse-json-body response)]
        (is (some? response))
        (is (= 401 (:status response)))
        (is (= "AUTH_REQUIRED" (:error_code body))))))

  (testing "updates session last activity on valid request"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          initial-session   (store/get-session session-id)
          initial-activity  (:last-activity initial-session)
          _                 (Thread/sleep 10)
          request           (make-request :session-id session-id)
          context           {:request request}
          enter-fn          (get-in mw/require-auth [:enter])
          _                 (enter-fn context)
          updated-session   (store/get-session session-id)
          updated-activity  (:last-activity updated-session)]
      (is (< initial-activity updated-activity)))))

;; Tests for remember-me fallback
(deftest require-auth-remember-me-test
  (testing "restores session from valid remember-me token"
    (let [encryption-secret (get-in config/config [:session :secret])
          plaintext-key     "lastfm-session-key"
          encrypted-key     (crypto/encrypt plaintext-key encryption-secret)
          future-time       (+ (System/currentTimeMillis) 1000000)
          token-doc         {:username "rememberuser"
                             :encrypted_session_key encrypted-key
                             :expires_at future-time}]
      (with-redefs [fs/enabled?          (fn [] true)
                    remember/lookup-token (fn [_] token-doc)]
        (let [request  (make-request :remember-token "valid-remember-tok")
              context  {:request request}
              enter-fn (get-in mw/require-auth [:enter])
              result   (enter-fn context)]
          (is (nil? (:response result)) "Should not set 401 response")
          (is (some? (get-in result [:request :session]))
              "Should attach session to request")
          (is (= "rememberuser" (get-in result [:request :session :username]))
              "Should restore correct username")
          (is (some? (::mw/new-session-id result))
              "Should store new session-id for cookie refresh")))))

  (testing "sets fresh session-id cookie in :leave when restored from remember-me"
    (let [encryption-secret (get-in config/config [:session :secret])
          plaintext-key     "lastfm-session-key"
          encrypted-key     (crypto/encrypt plaintext-key encryption-secret)
          future-time       (+ (System/currentTimeMillis) 1000000)
          token-doc         {:username "rememberuser"
                             :encrypted_session_key encrypted-key
                             :expires_at future-time}]
      (with-redefs [fs/enabled?          (fn [] true)
                    remember/lookup-token (fn [_] token-doc)]
        (let [request  (make-request :remember-token "valid-remember-tok")
              context  {:request request}
              enter-fn (get-in mw/require-auth [:enter])
              leave-fn (get-in mw/require-auth [:leave])
              ;; Simulate full interceptor chain
              after-enter (enter-fn context)
              after-enter-with-response (assoc after-enter
                                               :response {:status 200
                                                          :headers {}
                                                          :body "{}"})
              after-leave (leave-fn after-enter-with-response)
              set-cookie  (get-in after-leave [:response :headers "Set-Cookie"])]
          (is (some? set-cookie) "Should set session-id cookie")
          (is (or (string? set-cookie)
                  (and (vector? set-cookie) (seq set-cookie)))
              "Cookie should be a string or non-empty vector")))))

  (testing "falls back to 401 when remember-me token missing"
    (with-redefs [fs/enabled?          (fn [] true)
                  remember/lookup-token (fn [_] nil)]
      (let [request  (make-request)
            context  {:request request}
            enter-fn (get-in mw/require-auth [:enter])
            result   (enter-fn context)]
        (is (some? (:response result)))
        (is (= 401 (:status (:response result)))))))

  (testing "falls back to 401 when remember-me token invalid/expired"
    (with-redefs [fs/enabled?          (fn [] true)
                  remember/lookup-token (fn [_] nil)]
      (let [request  (make-request :remember-token "expired-or-bad-tok")
            context  {:request request}
            enter-fn (get-in mw/require-auth [:enter])
            result   (enter-fn context)]
        (is (some? (:response result)))
        (is (= 401 (:status (:response result))))))))

;; Tests for helper functions
(deftest get-session-test
  (testing "extracts session from request"
    (let [session-data {:session-id "test-id" :username "testuser"}
          request      {:session session-data}
          result       (mw/get-session request)]
      (is (= session-data result))))

  (testing "returns nil when no session in request"
    (let [request {}
          result  (mw/get-session request)]
      (is (nil? result)))))

(deftest get-session-id-test
  (testing "extracts session ID from request session"
    (let [request {:session {:session-id "test-id-123" :username "testuser"}}
          result  (mw/get-session-id request)]
      (is (= "test-id-123" result))))

  (testing "returns nil when no session in request"
    (let [request {}
          result  (mw/get-session-id request)]
      (is (nil? result))))

  (testing "returns nil when session has no session-id"
    (let [request {:session {:username "testuser"}}
          result  (mw/get-session-id request)]
      (is (nil? result)))))
