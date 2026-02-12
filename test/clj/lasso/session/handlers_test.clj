(ns lasso.session.handlers-test
  "Tests for session management HTTP handlers."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.session.handlers :as handlers]
            [lasso.session.manager :as manager]
            [lasso.auth.session :as auth-session]
            [lasso.session.store :as store]
            [lasso.lastfm.client :as lastfm]
            [clojure.data.json :as json]))

;; Test fixtures
(defn reset-sessions-fixture [f]
  "Reset session store before each test."
  (store/clear-all-sessions!)
  (f))

(use-fixtures :each reset-sessions-fixture)

;; Helper functions
(defn parse-json-body [response]
  "Parse JSON body from response."
  (when-let [body (:body response)]
    (json/read-str body :key-fn keyword)))

(defn make-request [session-id body-data]
  "Helper to create a test request with session and request body as InputStream."
  {:session {:session-id session-id}
   :body (java.io.ByteArrayInputStream. (.getBytes (json/write-str body-data)))})

(defn make-request-no-body [session-id]
  "Helper to create a test request with session but no body."
  {:session {:session-id session-id}})

;; Tests for start-session-handler
(deftest start-session-handler-test
  (testing "successfully start session with valid target user"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request session-id {:target_username "targetuser"})
            response (handlers/start-session-handler request)
            body (parse-json-body response)]
        (is (= 200 (:status response)))
        (is (= "active" (:state body)))
        (is (= "targetuser" (:target_username body)))
        (is (= 0 (:scrobble_count body))))))

  (testing "fail when target_username missing"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request (make-request session-id {})
          response (handlers/start-session-handler request)
          body (parse-json-body response)]
      (is (= 400 (:status response)))
      (is (= "Missing target_username" (:error body)))
      (is (= "MISSING_TARGET_USERNAME" (:error-code body)))))

  (testing "fail when target user doesn't exist"
    (with-redefs [lastfm/api-request (fn [_] {:error "User not found"})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request session-id {:target_username "nonexistent"})
            response (handlers/start-session-handler request)
            body (parse-json-body response)]
        (is (= 400 (:status response)))
        (is (string? (:error body)))
        (is (= "START_SESSION_FAILED" (:error-code body))))))

  (testing "handles exceptions gracefully"
    (with-redefs [manager/start-session (fn [_ _] (throw (Exception. "Database error")))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request session-id {:target_username "targetuser"})
            response (handlers/start-session-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Failed to start session" (:error body)))
        (is (= "START_SESSION_ERROR" (:error-code body)))))))

;; Tests for pause-session-handler
(deftest pause-session-handler-test
  (testing "successfully pause active session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            ;; Start session first
            _ (manager/start-session session-id "targetuser")
            request (make-request-no-body session-id)
            response (handlers/pause-session-handler request)
            body (parse-json-body response)]
        (is (= 200 (:status response)))
        (is (= "paused" (:state body)))
        (is (= "targetuser" (:target_username body))))))

  (testing "fail when no active session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request (make-request-no-body session-id)
          response (handlers/pause-session-handler request)
          body (parse-json-body response)]
      (is (= 400 (:status response)))
      (is (string? (:error body)))
      (is (= "PAUSE_SESSION_FAILED" (:error-code body)))))

  (testing "handles exceptions gracefully"
    (with-redefs [manager/pause-session (fn [_] (throw (Exception. "Error")))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request-no-body session-id)
            response (handlers/pause-session-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Failed to pause session" (:error body)))
        (is (= "PAUSE_SESSION_ERROR" (:error-code body)))))))

;; Tests for resume-session-handler
(deftest resume-session-handler-test
  (testing "successfully resume paused session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            ;; Start and pause session
            _ (manager/start-session session-id "targetuser")
            _ (manager/pause-session session-id)
            request (make-request-no-body session-id)
            response (handlers/resume-session-handler request)
            body (parse-json-body response)]
        (is (= 200 (:status response)))
        (is (= "active" (:state body)))
        (is (= "targetuser" (:target_username body))))))

  (testing "fail when session not paused"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            request (make-request-no-body session-id)
            response (handlers/resume-session-handler request)
            body (parse-json-body response)]
        (is (= 400 (:status response)))
        (is (string? (:error body)))
        (is (= "RESUME_SESSION_FAILED" (:error-code body))))))

  (testing "handles exceptions gracefully"
    (with-redefs [manager/resume-session (fn [_] (throw (Exception. "Error")))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request-no-body session-id)
            response (handlers/resume-session-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Failed to resume session" (:error body)))
        (is (= "RESUME_SESSION_ERROR" (:error-code body)))))))

;; Tests for stop-session-handler
(deftest stop-session-handler-test
  (testing "successfully stop session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            request (make-request-no-body session-id)
            response (handlers/stop-session-handler request)
            body (parse-json-body response)]
        (is (= 200 (:status response)))
        (is (true? (:success body)))
        ;; Verify session is cleared
        (let [session (store/get-session session-id)]
          (is (nil? (:following-session session)))))))

  (testing "stop when no session (no-op)"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request (make-request-no-body session-id)
          response (handlers/stop-session-handler request)
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))))

  (testing "handles exceptions gracefully"
    (with-redefs [manager/stop-session (fn [_] (throw (Exception. "Error")))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request-no-body session-id)
            response (handlers/stop-session-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Failed to stop session" (:error body)))
        (is (= "STOP_SESSION_ERROR" (:error-code body)))))))

;; Tests for status-handler
(deftest status-handler-test
  (testing "status with active session"
    (with-redefs [lastfm/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            _ (manager/start-session session-id "targetuser")
            request (make-request-no-body session-id)
            response (handlers/status-handler request)
            body (parse-json-body response)]
        (is (= 200 (:status response)))
        (is (true? (:authenticated body)))
        (is (= "testuser" (:username body)))
        (is (= "active" (get-in body [:session :state])))
        (is (= "targetuser" (get-in body [:session :target_username])))
        (is (= 0 (get-in body [:session :scrobble_count]))))))

  (testing "status without following session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request (make-request-no-body session-id)
          response (handlers/status-handler request)
          body (parse-json-body response)]
      (is (= 200 (:status response)))
      (is (true? (:authenticated body)))
      (is (= "testuser" (:username body)))
      (is (= "not-started" (get-in body [:session :state])))))

  (testing "handles exceptions gracefully"
    (with-redefs [manager/get-session-status (fn [_] (throw (Exception. "Error")))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            request (make-request-no-body session-id)
            response (handlers/status-handler request)
            body (parse-json-body response)]
        (is (= 500 (:status response)))
        (is (= "Failed to get session status" (:error body)))
        (is (= "STATUS_ERROR" (:error-code body)))))))
