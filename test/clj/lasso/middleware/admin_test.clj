(ns lasso.middleware.admin-test
  "Tests for admin authentication interceptor."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.middleware.admin :as admin-mw]
            [lasso.admin.session :as admin-session]
            [clojure.data.json :as json]))

;; Test fixtures
(defn reset-admin-sessions-fixture [f]
  (admin-session/clear-all-admin-sessions!)
  (f))

(use-fixtures :each reset-admin-sessions-fixture)

;; Helpers
(defn parse-json-body [response]
  (when-let [body (:body response)]
    (json/read-str body :key-fn keyword)))

(defn make-request [& {:keys [session-id]}]
  (cond-> {}
    session-id (assoc-in [:headers "cookie"] (str "admin-session=" session-id))))

(def enter-fn (get-in admin-mw/require-admin-auth [:enter]))

;; Tests
(deftest require-admin-auth-test
  (testing "allows request with valid admin-session cookie"
    (let [{:keys [session-id]} (admin-session/create-admin-session)
          context {:request (make-request :session-id session-id)}
          result (enter-fn context)]
      (is (some? (get-in result [:request :admin-session])))
      (is (= session-id (get-in result [:request :admin-session :session-id])))
      (is (nil? (:response result)))))

  (testing "rejects request with no cookie"
    (let [context {:request (make-request)}
          result (enter-fn context)
          body (parse-json-body (:response result))]
      (is (= 401 (get-in result [:response :status])))
      (is (= "ADMIN_AUTH_REQUIRED" (:error_code body)))))

  (testing "rejects request with invalid session id"
    (let [context {:request (make-request :session-id "bogus-id")}
          result (enter-fn context)
          body (parse-json-body (:response result))]
      (is (= 401 (get-in result [:response :status])))
      (is (= "ADMIN_SESSION_EXPIRED" (:error_code body)))))

  (testing "rejects request with deleted session"
    (let [{:keys [session-id]} (admin-session/create-admin-session)
          _ (admin-session/destroy-admin-session session-id)
          context {:request (make-request :session-id session-id)}
          result (enter-fn context)
          body (parse-json-body (:response result))]
      (is (= 401 (get-in result [:response :status])))
      (is (= "ADMIN_SESSION_EXPIRED" (:error_code body)))))

  (testing "touches session last-activity on valid request"
    (let [{:keys [session-id]} (admin-session/create-admin-session)
          before (:last-activity (admin-session/get-admin-session session-id))
          _ (Thread/sleep 5)
          context {:request (make-request :session-id session-id)}
          _ (enter-fn context)
          after (:last-activity (admin-session/get-admin-session session-id))]
      (is (< before after)))))
