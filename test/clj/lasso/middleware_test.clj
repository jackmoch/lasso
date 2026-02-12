(ns lasso.middleware-test
  "Tests for Pedestal interceptors."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.middleware :as mw]
            [lasso.auth.session :as auth-session]
            [lasso.session.store :as store]
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
  "Helper to create a test request with optional cookie."
  [& {:keys [session-id]}]
  (cond-> {}
    session-id (assoc-in [:headers "cookie"] (str "session-id=" session-id))))

;; Tests for require-auth interceptor
(deftest require-auth-interceptor-test
  (testing "allows request with valid session cookie"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request (make-request :session-id session-id)
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result (enter-fn context)]
      ;; Should attach session to context
      (is (some? (:session result)))
      (is (= "testuser" (get-in result [:session :username])))
      (is (= session-id (get-in result [:session :session-id])))
      ;; Should NOT set response (allow request to continue)
      (is (nil? (:response result)))))

  (testing "rejects request with missing session cookie"
    (let [request (make-request)
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result (enter-fn context)
          response (:response result)
          body (parse-json-body response)]
      (is (some? response))
      (is (= 401 (:status response)))
      (is (= "Authentication required" (:error body)))
      (is (= "AUTH_REQUIRED" (:error-code body)))))

  (testing "rejects request with invalid session ID"
    (let [request (make-request :session-id "invalid-session-id")
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result (enter-fn context)
          response (:response result)
          body (parse-json-body response)]
      (is (some? response))
      (is (= 401 (:status response)))
      (is (= "Session not found or expired" (:error body)))
      (is (= "SESSION_EXPIRED" (:error-code body)))))

  (testing "rejects request with expired session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          ;; Manually delete the session to simulate expiration
          _ (store/delete-session session-id)
          request (make-request :session-id session-id)
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result (enter-fn context)
          response (:response result)
          body (parse-json-body response)]
      (is (some? response))
      (is (= 401 (:status response)))
      (is (= "Session not found or expired" (:error body)))))

  (testing "updates session last activity on valid request"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          initial-session (store/get-session session-id)
          initial-activity (:last-activity initial-session)
          ;; Wait a tiny bit to ensure timestamp changes
          _ (Thread/sleep 10)
          request (make-request :session-id session-id)
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          _ (enter-fn context)
          updated-session (store/get-session session-id)
          updated-activity (:last-activity updated-session)]
      (is (< initial-activity updated-activity)))))

;; Tests for helper functions
(deftest get-session-test
  (testing "extracts session from context"
    (let [session-data {:session-id "test-id" :username "testuser"}
          context {:session session-data}
          result (mw/get-session context)]
      (is (= session-data result))))

  (testing "returns nil when no session in context"
    (let [context {}
          result (mw/get-session context)]
      (is (nil? result)))))

(deftest get-session-id-test
  (testing "extracts session ID from context session"
    (let [context {:session {:session-id "test-id-123" :username "testuser"}}
          result (mw/get-session-id context)]
      (is (= "test-id-123" result))))

  (testing "returns nil when no session in context"
    (let [context {}
          result (mw/get-session-id context)]
      (is (nil? result))))

  (testing "returns nil when session has no session-id"
    (let [context {:session {:username "testuser"}}
          result (mw/get-session-id context)]
      (is (nil? result)))))
