(ns lasso.server-test
  "Tests for server configuration and JSON logging."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.data.json :as json]
            [lasso.server :as server]))

;; =============================================================================
;; json-output-fn tests
;; =============================================================================

(defn- invoke-json-output-fn
  "Call the private json-output-fn via reflection for testing."
  [level ns-str message]
  (#'server/json-output-fn
   {:level     level
    :?ns-str   ns-str
    :msg_      (delay message)
    :timestamp_ (delay "2026-02-19T12:00:00.000Z")}))

(deftest json-output-fn-produces-valid-json
  (testing "output is a valid JSON string"
    (let [output (invoke-json-output-fn :info "lasso.server" "test message")
          parsed (json/read-str output :key-fn keyword)]
      (is (string? output))
      (is (map? parsed))
      (is (= "test message" (:message parsed)))
      (is (= "INFO" (:severity parsed)))
      (is (= "lasso.server" (:logger parsed)))
      (is (some? (:timestamp parsed))))))

(deftest json-output-fn-contains-required-keys
  (testing "JSON contains all required Cloud Logging keys"
    (let [output (invoke-json-output-fn :error "lasso.polling.engine" "poll failed")
          parsed (json/read-str output :key-fn keyword)]
      (is (contains? parsed :severity))
      (is (contains? parsed :message))
      (is (contains? parsed :timestamp))
      (is (contains? parsed :logger)))))

(deftest json-output-fn-maps-severity-correctly
  (testing "Timbre levels map to Cloud Logging severity names"
    (let [cases {:trace "DEBUG"
                 :debug "DEBUG"
                 :info  "INFO"
                 :warn  "WARNING"
                 :error "ERROR"
                 :fatal "CRITICAL"}]
      (doseq [[level expected-severity] cases]
        (let [output (invoke-json-output-fn level "ns" "msg")
              parsed (json/read-str output :key-fn keyword)]
          (is (= expected-severity (:severity parsed))
              (str "Level " level " should map to " expected-severity)))))))
