(ns lasso.middleware.security
  "Security middleware for production deployment.
   Implements CORS, security headers, and rate limiting."
  (:require [io.pedestal.interceptor :refer [interceptor]]
            [lasso.config :as config]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; =============================================================================
;; CORS (Cross-Origin Resource Sharing)
;; =============================================================================

(defn parse-allowed-origins
  "Parse CORS_ALLOWED_ORIGINS environment variable into a set.
   Supports comma-separated list of origins."
  []
  (if-let [origins (config/get-env "CORS_ALLOWED_ORIGINS")]
    (set (map str/trim (str/split origins #",")))
    #{"http://localhost:8080" "http://localhost:3000"})) ; Development defaults

(defn origin-allowed?
  "Check if request origin is in allowed list."
  [origin allowed-origins]
  (contains? allowed-origins origin))

(def cors-interceptor
  "CORS interceptor for handling cross-origin requests.
   Reads allowed origins from CORS_ALLOWED_ORIGINS environment variable."
  (interceptor
   {:name ::cors
    :leave (fn [context]
             (let [request (:request context)
                   origin (get-in request [:headers "origin"])
                   allowed-origins (parse-allowed-origins)
                   cors-enabled (config/get-env "CORS_ENABLED" "true")]

               (if (and (= cors-enabled "true")
                        origin
                        (origin-allowed? origin allowed-origins))
                 ;; Origin is allowed, add CORS headers to response
                 (update-in context [:response :headers] merge
                            {"Access-Control-Allow-Origin" origin
                             "Access-Control-Allow-Credentials" "true"
                             "Access-Control-Allow-Methods" "GET, POST, PUT, DELETE, OPTIONS"
                             "Access-Control-Allow-Headers" "Content-Type, Authorization"
                             "Access-Control-Max-Age" "3600"})
                 ;; Origin not allowed or CORS disabled
                 context)))}))

;; =============================================================================
;; Security Headers
;; =============================================================================

(def security-headers-interceptor
  "Add security headers to all responses.
   Implements OWASP security header recommendations."
  (interceptor
   {:name ::security-headers
    :leave (fn [context]
             (let [environment (get-in config/config [:environment])
                   is-production (= environment :production)
                   default-csp (if is-production
                                 "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'"
                                 ;; Dev mode needs unsafe-eval (shadow-cljs evalLoad) and ws: (hot-reload)
                                 "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' ws: wss:")
                   csp-policy (or (config/get-env "CSP_POLICY") default-csp)
                   headers {"X-Content-Type-Options" "nosniff"
                            "X-Frame-Options" "DENY"
                            "X-XSS-Protection" "1; mode=block"
                            "Referrer-Policy" "strict-origin-when-cross-origin"
                            "Content-Security-Policy" csp-policy}
                   ;; Add HSTS only in production over HTTPS
                   headers (if is-production
                            (assoc headers "Strict-Transport-Security"
                                   "max-age=31536000; includeSubDomains; preload")
                            headers)]
               (update-in context [:response :headers] merge headers)))}))

;; =============================================================================
;; Rate Limiting
;; =============================================================================

(def request-counts (atom {}))  ; {ip-address {minute-bucket request-count}}

(defn extract-client-ip
  "Extract the real client IP from request headers.
   Cloud Run (and trusted reverse proxies) append the connecting client's IP
   as the last entry in X-Forwarded-For. Taking the last entry prevents
   rate limit bypass via header spoofing — an attacker can prepend fake IPs
   but cannot forge the final entry added by the trusted proxy."
  [request]
  (if-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
    (-> forwarded-for
        (str/split #",")
        last
        str/trim)
    (or (get-in request [:headers "x-real-ip"])
        (:remote-addr request))))

(defn current-minute-bucket
  "Get current minute bucket for rate limiting (e.g., 2026-02-13T16:45)."
  []
  (let [now (System/currentTimeMillis)
        minutes (quot now 60000)]
    minutes))

(defn cleanup-old-buckets!
  "Remove rate limit data older than 2 minutes to prevent memory leak."
  []
  (let [current-bucket (current-minute-bucket)
        cutoff (- current-bucket 2)]
    (swap! request-counts
           (fn [counts]
             (into {}
                   (map (fn [[ip buckets]]
                          [ip (into {} (filter #(> (key %) cutoff) buckets))])
                        counts))))))

(defn increment-request-count!
  "Increment request count for IP address in current minute bucket."
  [ip-address]
  (let [bucket (current-minute-bucket)]
    (swap! request-counts
           (fn [counts]
             (update-in counts [ip-address bucket] (fnil inc 0))))))

(defn get-request-count
  "Get request count for IP address in current minute bucket."
  [ip-address]
  (let [bucket (current-minute-bucket)]
    (get-in @request-counts [ip-address bucket] 0)))

(def rate-limit-interceptor
  "Rate limiting interceptor to prevent abuse.
   Limits requests per IP address per time window."
  (interceptor
   {:name ::rate-limit
    :enter (fn [context]
             (let [enabled (= (config/get-env "RATE_LIMIT_ENABLED" "true") "true")
                   max-requests (Integer/parseInt (config/get-env "RATE_LIMIT_MAX_REQUESTS" "100"))
                   request (:request context)
                   ip-address (extract-client-ip request)]

               (if enabled
                 (do
                   ;; Cleanup old buckets periodically (every ~100 requests)
                   (when (zero? (rand-int 100))
                     (cleanup-old-buckets!))

                   (let [current-count (get-request-count ip-address)]
                     (if (>= current-count max-requests)
                       ;; Rate limit exceeded
                       (do
                         (log/warn "Rate limit exceeded for IP:" ip-address
                                  "count:" current-count)
                         (assoc context :response
                                {:status 429
                                 :headers {"Content-Type" "application/json"
                                          "Retry-After" "60"}
                                 :body "{\"error\": \"Too many requests. Please try again later.\"}"}))
                       ;; Under limit, increment and continue
                       (do
                         (increment-request-count! ip-address)
                         context))))
                 ;; Rate limiting disabled
                 context)))}))

;; =============================================================================
;; Request Logging (Security Audit Trail)
;; =============================================================================

(def request-logging-interceptor
  "Log requests for security audit trail in production."
  (interceptor
   {:name ::request-logging
    :enter (fn [context]
             (let [request (:request context)
                   method (:request-method request)
                   uri (:uri request)
                   ip (or (get-in request [:headers "x-forwarded-for"])
                         (:remote-addr request))
                   user-agent (get-in request [:headers "user-agent"])]
               (log/info "Request"
                        :method method
                        :uri uri
                        :ip ip
                        :user-agent user-agent)
               context))
    :leave (fn [context]
             (let [status (get-in context [:response :status])]
               (when (and status (>= status 400))
                 (log/warn "HTTP Error Response"
                          :status status
                          :uri (get-in context [:request :uri])))
               context))}))

;; =============================================================================
;; Auth Endpoint Rate Limiting
;; =============================================================================

;; A separate counter from the global rate limiter so auth limits are tracked
;; independently — a flood of auth attempts won't consume the global budget,
;; and we can enforce a much tighter ceiling on these specific endpoints.
(def ^:private auth-request-counts (atom {}))

(defn- get-auth-request-count [ip-address]
  (get-in @auth-request-counts [ip-address (current-minute-bucket)] 0))

(defn- increment-auth-request-count! [ip-address]
  (swap! auth-request-counts update-in [ip-address (current-minute-bucket)] (fnil inc 0)))

(defn reset-auth-rate-limit-counts!
  "Reset auth rate limit counters. For testing use only."
  []
  (reset! auth-request-counts {}))

(def auth-rate-limit-interceptor
  "Stricter per-route rate limit for authentication endpoints.
   /api/auth/init and /api/auth/callback each trigger upstream Last.fm API
   calls, so a tighter ceiling prevents abuse and protects Last.fm quota.

   Controlled by AUTH_RATE_LIMIT_MAX_REQUESTS env var (default: 10/min per IP).
   Respects the global RATE_LIMIT_ENABLED flag."
  (interceptor
   {:name ::auth-rate-limit
    :enter (fn [context]
             (let [enabled (= (config/get-env "RATE_LIMIT_ENABLED" "true") "true")
                   max-requests (Integer/parseInt
                                 (config/get-env "AUTH_RATE_LIMIT_MAX_REQUESTS" "10"))
                   request (:request context)
                   ip-address (extract-client-ip request)]
               (if enabled
                 (do
                   ;; Periodically sweep old minute buckets
                   (when (zero? (rand-int 100))
                     (let [cutoff (- (current-minute-bucket) 2)]
                       (swap! auth-request-counts
                              (fn [counts]
                                (into {}
                                      (map (fn [[ip buckets]]
                                             [ip (into {} (filter #(> (key %) cutoff) buckets))])
                                           counts))))))
                   (let [current-count (get-auth-request-count ip-address)]
                     (if (>= current-count max-requests)
                       (do
                         (log/warn "Auth rate limit exceeded for IP:" ip-address
                                   "count:" current-count)
                         (assoc context :response
                                {:status 429
                                 :headers {"Content-Type" "application/json"
                                           "Retry-After" "60"}
                                 :body "{\"error\": \"Too many requests. Please try again later.\"}"}))
                       (do
                         (increment-auth-request-count! ip-address)
                         context))))
                 context)))}))

;; =============================================================================
;; Sensitive Data Filtering
;; =============================================================================

(defn filter-sensitive-params
  "Remove sensitive parameters from request before logging."
  [params]
  (let [sensitive-keys #{:password :token :secret :api-secret :session-key}]
    (reduce (fn [m k]
              (if (contains? m k)
                (assoc m k "[REDACTED]")
                m))
            params
            sensitive-keys)))

;; =============================================================================
;; Security Middleware Stack
;; =============================================================================

(def security-interceptors
  "Complete security interceptor stack.
   Apply these in your route configuration."
  [cors-interceptor
   security-headers-interceptor
   rate-limit-interceptor
   request-logging-interceptor])
