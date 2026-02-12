(ns lasso.integration.manual-testing-issues-test
  "Integration tests covering issues discovered during manual testing.
   These tests ensure regressions don't occur for bugs that were found
   through manual E2E testing rather than caught by existing tests."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.auth.handlers :as auth-handlers]
            [lasso.session.handlers :as session-handlers]
            [lasso.middleware :as mw]
            [lasso.lastfm.client :as client]
            [lasso.lastfm.scrobble :as scrobble]
            [lasso.lastfm.oauth :as oauth]
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
(defn parse-json-body [response]
  "Parse JSON body from response."
  (when-let [body (:body response)]
    (json/read-str body :key-fn keyword)))

;;; ============================================================================
;;; Issue #1: Handler Return Format
;;; ============================================================================
;;; Handlers were returning {:response {...}} instead of response map directly.
;;; This caused "Internal server error: exception" with no useful error messages.

(deftest handler-return-format-test
  (testing "Auth handlers return response maps directly (not wrapped in :response)"
    (with-redefs [oauth/get-token (fn [] {:token "test-token"})]
      (let [response (auth-handlers/auth-init-handler {})]
        ;; Should be a response map with :status, :headers, :body
        (is (map? response))
        (is (contains? response :status))
        (is (contains? response :headers))
        (is (contains? response :body))
        ;; Should NOT be wrapped in {:response ...}
        (is (not (contains? response :response))))))

  (testing "Session handlers return response maps directly"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request {:session {:session-id session-id}
                   :body (java.io.ByteArrayInputStream.
                          (.getBytes (json/write-str {:target_username "targetuser"})))}]
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (let [response (session-handlers/start-session-handler request)]
          ;; Should be a response map
          (is (map? response))
          (is (contains? response :status))
          (is (not (contains? response :response))))))))

;;; ============================================================================
;;; Issue #2: Middleware Session Attachment
;;; ============================================================================
;;; Middleware was attaching session to context instead of request,
;;; causing "session-id nil" errors in handlers.

(deftest middleware-session-attachment-test
  (testing "require-auth attaches session to request, not context"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request {:headers {"cookie" (str "session-id=" session-id)}}
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result (enter-fn context)]
      ;; Session should be on [:request :session], NOT [:session]
      (is (some? (get-in result [:request :session])))
      (is (= "testuser" (get-in result [:request :session :username])))
      (is (nil? (get result :session)) "Session should NOT be on context root")))

  (testing "get-session-id extracts from request correctly"
    (let [request {:session {:session-id "test-id" :username "testuser"}}]
      (is (= "test-id" (mw/get-session-id request))))))

;;; ============================================================================
;;; Issue #3: JSON Body Parsing from InputStream
;;; ============================================================================
;;; Request bodies arrive as InputStreams, not strings. Handlers must parse correctly.

(deftest json-body-parsing-test
  (testing "Session handler parses InputStream body correctly"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          json-str (json/write-str {:target_username "targetuser"})
          input-stream (java.io.ByteArrayInputStream. (.getBytes json-str))
          request {:session {:session-id session-id}
                   :body input-stream}]
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (let [response (session-handlers/start-session-handler request)
              body (parse-json-body response)]
          (is (= 200 (:status response)))
          (is (= "targetuser" (:target_username body)))))))

  (testing "Handler handles string body correctly (legacy support)"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request {:session {:session-id session-id}
                   :body (json/write-str {:target_username "targetuser"})}]
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (let [response (session-handlers/start-session-handler request)
              body (parse-json-body response)]
          (is (= 200 (:status response)))
          (is (= "targetuser" (:target_username body)))))))

  (testing "Handler handles map body correctly"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request {:session {:session-id session-id}
                   :body {:target_username "targetuser"}}]
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (let [response (session-handlers/start-session-handler request)
              body (parse-json-body response)]
          (is (= 200 (:status response)))
          (is (= "targetuser" (:target_username body))))))))

;;; ============================================================================
;;; Issue #4: HTTP Method Selection (GET vs POST)
;;; ============================================================================
;;; Last.fm API requires POST for write operations and signed requests.
;;; GET is used for read operations.

(deftest http-method-selection-test
  (testing "Unsigned requests use GET"
    (let [http-calls (atom [])]
      (with-redefs [clj-http.client/get (fn [url opts]
                                          (swap! http-calls conj {:method :get :url url :opts opts})
                                          {:status 200 :body {:user {:name "testuser"}}})
                    clj-http.client/post (fn [url opts]
                                           (swap! http-calls conj {:method :post :url url :opts opts})
                                           {:status 200 :body {:user {:name "testuser"}}})]
        (client/api-request {:method "user.getInfo" :params {:user "testuser"} :signed false})
        (is (= 1 (count @http-calls)))
        (is (= :get (:method (first @http-calls)))))))

  (testing "Signed requests use POST with form-params"
    (let [http-calls (atom [])]
      (with-redefs [clj-http.client/get (fn [url opts]
                                          (swap! http-calls conj {:method :get :url url :opts opts})
                                          {:status 200 :body {:scrobbles {:scrobble {}}}})
                    clj-http.client/post (fn [url opts]
                                           (swap! http-calls conj {:method :post :url url :opts opts})
                                           {:status 200 :body {:scrobbles {:scrobble {}}}})]
        (client/api-request {:method "track.scrobble"
                            :params {:artist "Test" :track "Song" :timestamp 123456}
                            :signed true})
        (is (= 1 (count @http-calls)))
        (is (= :post (:method (first @http-calls))))
        (is (contains? (:opts (first @http-calls)) :form-params))))))

;;; ============================================================================
;;; Issue #5: Scrobble Response Parsing (String vs Integer)
;;; ============================================================================
;;; Last.fm returns :accepted and :ignored as integers, not strings.
;;; Parsing with Integer/parseInt on integers causes ClassCastException.

(deftest scrobble-response-parsing-test
  (testing "Handles integer values for accepted and ignored"
    (let [response {:scrobbles {(keyword "@attr") {:accepted 1 :ignored 0}}}
          result (scrobble/validate-scrobble-response response)]
      (is (:success result))
      (is (= 1 (:accepted result)))
      (is (= 0 (:ignored result)))))

  (testing "Handles string values for accepted and ignored (legacy)"
    (let [response {:scrobbles {(keyword "@attr") {:accepted "2" :ignored "1"}}}
          result (scrobble/validate-scrobble-response response)]
      (is (:success result))
      (is (= 2 (:accepted result)))
      (is (= 1 (:ignored result)))))

  (testing "Handles mixed integer and string values"
    (let [response {:scrobbles {(keyword "@attr") {:accepted 3 :ignored "0"}}}
          result (scrobble/validate-scrobble-response response)]
      (is (:success result))
      (is (= 3 (:accepted result)))
      (is (= 0 (:ignored result)))))

  (testing "Handles error response"
    (let [response {:error 14 :message "Unauthorized"}
          result (scrobble/validate-scrobble-response response)]
      (is (not (:success result)))
      (is (= 14 (:error result))))))

;;; ============================================================================
;;; Issue #6: OAuth Token Expiration
;;; ============================================================================
;;; OAuth tokens expire after ~60 seconds. This isn't directly testable
;;; in unit tests but we can verify error handling.

(deftest oauth-token-expiration-handling-test
  (testing "auth.getSession handles expired token error gracefully"
    (with-redefs [client/api-request (fn [_] {:error 14
                                               :message "Unauthorized Token - This token has not been authorized"})]
      (let [result (oauth/get-session-key "expired-token")]
        (is (contains? result :error))
        (is (= 14 (:error result)))
        (is (string? (:message result))))))

  (testing "auth-callback-handler returns 401 for expired token"
    (with-redefs [oauth/get-session-key (fn [_] {:error "INVALID_TOKEN"})]
      (let [request {:params {:token "expired-token"}}
            response (auth-handlers/auth-callback-handler request)
            body (parse-json-body response)]
        (is (= 401 (:status response)))
        (is (= "Authentication failed" (:error body)))
        (is (= "OAUTH_SESSION_FAILED" (:error-code body)))))))

;;; ============================================================================
;;; Issue #7: Cookie Parsing
;;; ============================================================================
;;; Cookies must be correctly extracted from request headers.

(deftest cookie-parsing-test
  (testing "parse-cookie extracts session-id from cookie header"
    (let [request {:headers {"cookie" "session-id=abc-123-def; other-cookie=value"}}]
      (is (= "abc-123-def" (lasso.util.http/parse-cookie request "session-id")))))

  (testing "parse-cookie returns nil when cookie not present"
    (let [request {:headers {"cookie" "other-cookie=value"}}]
      (is (nil? (lasso.util.http/parse-cookie request "session-id")))))

  (testing "parse-cookie handles missing cookie header"
    (let [request {:headers {}}]
      (is (nil? (lasso.util.http/parse-cookie request "session-id")))))

  (testing "require-auth uses cookie to retrieve session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          request {:headers {"cookie" (str "session-id=" session-id)}}
          context {:request request}
          enter-fn (get-in mw/require-auth [:enter])
          result (enter-fn context)]
      (is (some? (get-in result [:request :session])))
      (is (= "testuser" (get-in result [:request :session :username]))))))

;;; ============================================================================
;;; Integration Test: Complete OAuth Flow
;;; ============================================================================

(deftest complete-oauth-flow-test
  (testing "Complete OAuth flow from init to callback"
    (with-redefs [oauth/get-token (fn [] {:token "test-token-123"})
                  oauth/get-session-key (fn [token]
                                          (is (= "test-token-123" token))
                                          {:session {:name "testuser" :key "session-key-abc"}})]
      ;; Step 1: Initialize OAuth
      (let [init-response (auth-handlers/auth-init-handler {})
            init-body (parse-json-body init-response)]
        (is (= 200 (:status init-response)))
        (is (string? (:auth_url init-body)))
        (is (.contains (:auth_url init-body) "test-token-123"))

        ;; Step 2: Callback after user authorization
        (let [callback-response (auth-handlers/auth-callback-handler
                                {:params {:token "test-token-123"}})
              set-cookie-header (get-in callback-response [:headers "Set-Cookie"])
              ;; Set-Cookie can be a string or vector
              session-cookie (if (vector? set-cookie-header)
                              (first (filter #(.contains % "session-id=") set-cookie-header))
                              set-cookie-header)]
          ;; Verify redirect to frontend root
          (is (= 302 (:status callback-response)))
          (is (= "/" (get-in callback-response [:headers "Location"])))
          ;; Verify session cookie was set
          (is (some? session-cookie))
          (is (.contains session-cookie "session-id="))

          ;; Verify session was created in store
          (let [session-id (second (re-find #"session-id=([^;]+)" session-cookie))
                session (store/get-session session-id)]
            (is (some? session))
            (is (= "testuser" (:username session)))))))))
