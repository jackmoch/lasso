(ns lasso.admin.handlers-test
  "Tests for admin API handlers."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.admin.handlers :as handlers]
            [lasso.admin.session :as admin-session]
            [lasso.session.store :as store]
            [lasso.auth.session :as auth-session]
            [lasso.polling.scheduler :as scheduler]
            [lasso.config]
            [clojure.core.async :as async]
            [clojure.data.json :as json]))

;; Test fixtures
(defn reset-stores-fixture [f]
  (admin-session/clear-all-admin-sessions!)
  (store/clear-all-sessions!)
  (reset! scheduler/active-pollers {})
  (f))

(use-fixtures :each reset-stores-fixture)

;; Helpers
(defn parse-json-body [response]
  (when-let [body (:body response)]
    (json/read-str body :key-fn keyword)))

(defn make-login-request [username password]
  {:body (json/write-str {:username username :password password})})

(defn extract-cookie [response cookie-name]
  (when-let [set-cookie (get-in response [:headers "Set-Cookie"])]
    (let [parts (.split set-cookie ";")
          cookie-part (first parts)]
      (when (.startsWith cookie-part (str cookie-name "="))
        (subs cookie-part (inc (count cookie-name)))))))

;; =============================================================================
;; login-handler tests
;; =============================================================================

(deftest login-handler-test
  (testing "valid credentials create admin session and set cookie"
    ;; Override config for this test
    (with-redefs [lasso.config/config
                  (assoc lasso.config/config
                         :admin {:username "admin"
                                 :password "secret"
                                 :session-ttl-ms 28800000}
                         :environment :development)]
      (let [response (handlers/login-handler (make-login-request "admin" "secret"))
            body (parse-json-body response)
            cookie (extract-cookie response "admin-session")]
        (is (= 200 (:status response)))
        (is (true? (:success body)))
        (is (some? cookie))
        ;; Session should exist in store
        (is (some? (admin-session/get-admin-session cookie))))))

  (testing "wrong password returns 401"
    (with-redefs [lasso.config/config
                  (assoc lasso.config/config
                         :admin {:username "admin"
                                 :password "secret"
                                 :session-ttl-ms 28800000})]
      (let [response (handlers/login-handler (make-login-request "admin" "wrong"))
            body (parse-json-body response)]
        (is (= 401 (:status response)))
        (is (= "ADMIN_AUTH_FAILED" (:error_code body))))))

  (testing "wrong username returns 401"
    (with-redefs [lasso.config/config
                  (assoc lasso.config/config
                         :admin {:username "admin"
                                 :password "secret"
                                 :session-ttl-ms 28800000})]
      (let [response (handlers/login-handler (make-login-request "notadmin" "secret"))
            body (parse-json-body response)]
        (is (= 401 (:status response)))
        (is (= "ADMIN_AUTH_FAILED" (:error_code body))))))

  (testing "missing credentials returns 401"
    (with-redefs [lasso.config/config
                  (assoc lasso.config/config
                         :admin {:username "admin"
                                 :password "secret"
                                 :session-ttl-ms 28800000})]
      (let [response (handlers/login-handler {:body "{}"})
            body (parse-json-body response)]
        (is (= 401 (:status response)))
        (is (= "ADMIN_AUTH_FAILED" (:error_code body))))))

  (testing "unset admin credentials always returns 401"
    (with-redefs [lasso.config/config
                  (assoc lasso.config/config
                         :admin {:username nil
                                 :password nil
                                 :session-ttl-ms 28800000})]
      (let [response (handlers/login-handler (make-login-request "admin" "anything"))
            body (parse-json-body response)]
        (is (= 401 (:status response)))
        (is (= "ADMIN_AUTH_FAILED" (:error_code body)))))))

;; =============================================================================
;; logout-handler tests
;; =============================================================================

(deftest logout-handler-test
  (testing "logout clears admin session and cookie"
    (let [{:keys [session-id]} (admin-session/create-admin-session)
          request {:admin-session {:session-id session-id}}
          response (handlers/logout-handler request)
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      ;; Cookie should be cleared (max-age=0)
      (is (.contains (get-in response [:headers "Set-Cookie"]) "Max-Age=0"))
      ;; Session should be gone
      (is (nil? (admin-session/get-admin-session session-id))))))

;; =============================================================================
;; status-handler tests
;; =============================================================================

(deftest status-handler-test
  (testing "returns empty metrics when no sessions"
    (let [response (handlers/status-handler {})
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (= 0 (get-in body [:metrics :total_sessions])))
      (is (= 0 (get-in body [:metrics :active_sessions])))
      (is (= 0 (get-in body [:metrics :paused_sessions])))
      (is (= 0 (get-in body [:metrics :idle_sessions])))
      (is (= [] (:sessions body)))))

  (testing "returns session data without session_key"
    ;; Create a user session
    (let [_ (auth-session/create-session "alice" "secret-key")
          response (handlers/status-handler {})
          body (parse-json-body response)
          session-data (first (:sessions body))]
      (is (= 1 (get-in body [:metrics :total_sessions])))
      (is (= 1 (get-in body [:metrics :idle_sessions])))
      (is (= "alice" (:username session-data)))
      (is (= "idle" (:state session-data)))
      ;; session_key must never appear in response
      (is (nil? (:session_key session-data)))))

  (testing "counts active pollers correctly"
    (reset! scheduler/active-pollers {"fake-id-1" :chan "fake-id-2" :chan})
    (let [response (handlers/status-handler {})
          body (parse-json-body response)]
      (is (= 2 (get-in body [:metrics :active_pollers])))))

  (testing "sessions sorted by created_at descending"
    ;; Create two sessions with different timestamps
    (let [_ (auth-session/create-session "first" "key1")
          _ (Thread/sleep 5)
          _ (auth-session/create-session "second" "key2")
          response (handlers/status-handler {})
          body (parse-json-body response)
          sessions (:sessions body)
          usernames (mapv :username sessions)]
      ;; Newest first
      (is (= "second" (first usernames)))
      (is (= "first" (second usernames))))))

;; =============================================================================
;; force-stop-handler tests
;; =============================================================================

(deftest force-stop-handler-test
  (testing "stops existing session"
    (let [{:keys [session-id]} (auth-session/create-session "bob" "key")
          ;; Register a fake poller
          _ (swap! scheduler/active-pollers assoc session-id (async/chan))
          request {:path-params {:session-id session-id}}
          response (handlers/force-stop-handler request)
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (= session-id (:session_id body)))
      ;; Session should be gone
      (is (nil? (store/get-session session-id)))
      ;; Poller should be removed
      (is (nil? (get @scheduler/active-pollers session-id)))))

  (testing "returns 404 for non-existent session"
    (let [request {:path-params {:session-id "nonexistent-id"}}
          response (handlers/force-stop-handler request)
          body (parse-json-body response)]
      (is (= 404 (:status response)))
      (is (= "SESSION_NOT_FOUND" (:error_code body))))))
