(ns lasso.middleware.security-test
  "Tests for security middleware: IP extraction and auth-specific rate limiting."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.middleware.security :as security]))

(use-fixtures :each
  (fn [f]
    (security/reset-auth-rate-limit-counts!)
    (f)
    (security/reset-auth-rate-limit-counts!)))

;; =============================================================================
;; extract-client-ip tests
;; =============================================================================

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

;; =============================================================================
;; auth-rate-limit-interceptor tests
;; =============================================================================

(defn- make-context
  "Build a minimal interceptor context with a request from the given IP."
  [ip]
  {:request {:remote-addr ip :headers {}}})

(defn- make-context-with-forwarded
  "Build a context where X-Forwarded-For is set (proxy scenario)."
  [forwarded-for]
  {:request {:remote-addr "10.0.0.1"
             :headers {"x-forwarded-for" forwarded-for}}})

(defn- enter [context]
  ((get-in security/auth-rate-limit-interceptor [:enter]) context))

(deftest auth-rate-limit-allows-requests-below-limit-test
  (testing "allows requests when count is below the default limit of 10"
    (doseq [_ (range 9)]
      (let [result (enter (make-context "1.2.3.4"))]
        (is (nil? (:response result)) "request should pass through")))
    ;; 9 requests in — still under limit, 10th should also pass
    (let [result (enter (make-context "1.2.3.4"))]
      (is (nil? (:response result)) "10th request should pass (limit is 10)"))))

(deftest auth-rate-limit-blocks-at-limit-test
  (testing "blocks the request at the limit (11th request with default limit 10)"
    (doseq [_ (range 10)]
      (enter (make-context "5.6.7.8")))
    (let [result (enter (make-context "5.6.7.8"))]
      (is (= 429 (get-in result [:response :status])))
      (is (= "60" (get-in result [:response :headers "Retry-After"]))))))

(deftest auth-rate-limit-tracks-ips-independently-test
  (testing "different IPs have independent counters"
    (doseq [_ (range 10)]
      (enter (make-context "ip-a")))
    (is (= 429 (get-in (enter (make-context "ip-a")) [:response :status])))
    (is (nil? (:response (enter (make-context "ip-b")))))))

(deftest auth-rate-limit-independent-from-global-test
  (testing "auth counter is separate from the global rate limit counter"
    (doseq [_ (range 10)]
      (enter (make-context "3.3.3.3")))
    (is (= 429 (get-in (enter (make-context "3.3.3.3")) [:response :status])))
    ;; Global counter for this IP should still be at 0 — no cross-contamination
    (is (= 0 (security/get-request-count "3.3.3.3")))))

(deftest auth-rate-limit-uses-extract-client-ip-test
  (testing "uses the last IP in X-Forwarded-For chain (spoofing prevention)"
    ;; Exhaust limit for real-ip via chain — spoofed prefix should be ignored
    (doseq [_ (range 10)]
      (enter (make-context-with-forwarded "spoofed, real-ip")))
    ;; real-ip is now blocked
    (is (= 429 (get-in (enter (make-context-with-forwarded "spoofed, real-ip")) [:response :status])))
    ;; A different real-ip is unaffected regardless of the spoofed prefix
    (is (nil? (:response (enter (make-context-with-forwarded "spoofed, other-real-ip")))))))
