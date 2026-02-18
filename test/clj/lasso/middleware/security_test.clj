(ns lasso.middleware.security-test
  "Tests for security middleware, focusing on IP extraction and rate limiting."
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.middleware.security :as security]))

(deftest extract-client-ip-test
  (testing "falls back to remote-addr when no forwarded headers present"
    (let [request {:remote-addr "1.2.3.4"}]
      (is (= "1.2.3.4" (security/extract-client-ip request)))))

  (testing "uses x-real-ip when present and no X-Forwarded-For"
    (let [request {:headers {"x-real-ip" "1.2.3.4"} :remote-addr "127.0.0.1"}]
      (is (= "1.2.3.4" (security/extract-client-ip request)))))

  (testing "extracts single IP from X-Forwarded-For"
    (let [request {:headers {"x-forwarded-for" "1.2.3.4"}}]
      (is (= "1.2.3.4" (security/extract-client-ip request)))))

  (testing "takes the last IP from a multi-hop X-Forwarded-For chain"
    ;; Cloud Run appends the real client IP last — taking the last entry
    ;; is the only entry that cannot be spoofed by the connecting client.
    (let [request {:headers {"x-forwarded-for" "10.0.0.1, 203.0.113.5"}}]
      (is (= "203.0.113.5" (security/extract-client-ip request)))))

  (testing "prevents bypass: spoofed prefix IPs are ignored"
    ;; An attacker sets X-Forwarded-For to a fake IP. Cloud Run appends the
    ;; real connecting IP. Taking last entry gives the real one.
    (let [request {:headers {"x-forwarded-for" "fake-ip-1, fake-ip-2, real-client-ip"}}]
      (is (= "real-client-ip" (security/extract-client-ip request)))))

  (testing "trims whitespace from the extracted IP"
    (let [request {:headers {"x-forwarded-for" "10.0.0.1,   203.0.113.5  "}}]
      (is (= "203.0.113.5" (security/extract-client-ip request)))))

  (testing "X-Forwarded-For takes precedence over x-real-ip"
    (let [request {:headers {"x-forwarded-for" "1.2.3.4"
                             "x-real-ip" "5.6.7.8"}}]
      (is (= "1.2.3.4" (security/extract-client-ip request)))))

  (testing "returns nil when no ip information available"
    (let [request {:headers {}}]
      (is (nil? (security/extract-client-ip request))))))
