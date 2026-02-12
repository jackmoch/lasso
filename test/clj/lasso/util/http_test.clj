(ns lasso.util.http-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.data.json :as json]
            [lasso.util.http :as http]))

(deftest cookie-string-test
  (testing "Cookie string generation with defaults"
    (let [cookie-str (http/cookie-string "session-id" "abc123")]
      (is (re-find #"session-id=abc123" cookie-str))
      (is (re-find #"Path=/" cookie-str))
      (is (re-find #"Secure" cookie-str))
      (is (re-find #"HttpOnly" cookie-str))
      (is (re-find #"SameSite=Lax" cookie-str))
      (is (re-find #"Max-Age=\d+" cookie-str))))

  (testing "Cookie string with custom options"
    (let [cookie-str (http/cookie-string "test" "value"
                                         :max-age 3600
                                         :path "/api"
                                         :same-site "Strict")]
      (is (re-find #"test=value" cookie-str))
      (is (re-find #"Max-Age=3600" cookie-str))
      (is (re-find #"Path=/api" cookie-str))
      (is (re-find #"SameSite=Strict" cookie-str))))

  (testing "Cookie string without secure flag"
    (let [cookie-str (http/cookie-string "dev" "test" :secure false)]
      (is (not (re-find #"Secure" cookie-str))))))

(deftest parse-cookie-test
  (testing "Parse single cookie"
    (let [request {:headers {"cookie" "session-id=abc123"}}]
      (is (= "abc123" (http/parse-cookie request "session-id")))))

  (testing "Parse multiple cookies"
    (let [request {:headers {"cookie" "session-id=abc123; user=john; theme=dark"}}]
      (is (= "abc123" (http/parse-cookie request "session-id")))
      (is (= "john" (http/parse-cookie request "user")))
      (is (= "dark" (http/parse-cookie request "theme")))))

  (testing "Parse cookie with spaces"
    (let [request {:headers {"cookie" "session-id=abc123 ; user=john"}}]
      (is (= "abc123" (http/parse-cookie request "session-id")))
      (is (= "john" (http/parse-cookie request "user")))))

  (testing "Missing cookie returns nil"
    (let [request {:headers {"cookie" "session-id=abc123"}}]
      (is (nil? (http/parse-cookie request "missing")))))

  (testing "No cookie header returns nil"
    (let [request {:headers {}}]
      (is (nil? (http/parse-cookie request "session-id"))))))

(deftest json-response-test
  (testing "Basic JSON response"
    (let [response (http/json-response {:status "ok"})]
      (is (= 200 (:status response)))
      (is (= "application/json" (get-in response [:headers "Content-Type"])))
      (is (= "{\"status\":\"ok\"}" (:body response)))))

  (testing "JSON response with custom status"
    (let [response (http/json-response {:data "test"} :status 201)]
      (is (= 201 (:status response)))))

  (testing "JSON response with additional headers"
    (let [response (http/json-response {:data "test"}
                                       :headers {"X-Custom" "value"})]
      (is (= "value" (get-in response [:headers "X-Custom"])))
      (is (= "application/json" (get-in response [:headers "Content-Type"])))))

  (testing "JSON response with cookies"
    (let [response (http/json-response {:data "test"}
                                       :cookies {"session-id" "abc123"})]
      (is (vector? (get-in response [:headers "Set-Cookie"])))
      (is (some #(re-find #"session-id=abc123" %)
                (get-in response [:headers "Set-Cookie"]))))))

(deftest error-response-test
  (testing "Basic error response"
    (let [response (http/error-response "Something went wrong")]
      (is (= 500 (:status response)))
      (is (= "application/json" (get-in response [:headers "Content-Type"])))
      (let [body (json/read-str (:body response) :key-fn keyword)]
        (is (= "Something went wrong" (:error body))))))

  (testing "Error response with custom status"
    (let [response (http/error-response "Not found" :status 404)]
      (is (= 404 (:status response)))))

  (testing "Error response with error code"
    (let [response (http/error-response "Invalid input"
                                       :status 400
                                       :error-code "VALIDATION_ERROR")
          body (json/read-str (:body response) :key-fn keyword)]
      (is (= "Invalid input" (:error body)))
      (is (= "VALIDATION_ERROR" (:error-code body)))))

  (testing "Error response with details"
    (let [response (http/error-response "Validation failed"
                                       :status 400
                                       :details {:field "username" :issue "too short"})
          body (json/read-str (:body response) :key-fn keyword)]
      (is (= "Validation failed" (:error body)))
      (is (map? (:details body))))))
