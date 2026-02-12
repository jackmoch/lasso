(ns lasso.util.http
  "HTTP utilities for cookies, responses, and request handling."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]))

(defn cookie-string
  "Generate a Set-Cookie header value with security settings.
   Options:
   - :max-age - Cookie lifetime in seconds (default: 7 days)
   - :path - Cookie path (default: /)
   - :secure - Secure flag (default: true in production)
   - :http-only - HttpOnly flag (default: true)
   - :same-site - SameSite attribute (default: Lax)"
  [name value & {:keys [max-age path secure http-only same-site]
                 :or {max-age (* 7 24 60 60) ; 7 days
                      path "/"
                      secure true
                      http-only true
                      same-site "Lax"}}]
  (str/join "; "
            (cond-> [(str name "=" value)]
              max-age (conj (str "Max-Age=" max-age))
              path (conj (str "Path=" path))
              secure (conj "Secure")
              http-only (conj "HttpOnly")
              same-site (conj (str "SameSite=" same-site)))))

(defn parse-cookie
  "Extract a cookie value from the Cookie header in a request.
   Returns nil if cookie not found."
  [request cookie-name]
  (when-let [cookie-header (get-in request [:headers "cookie"])]
    (let [cookies (str/split cookie-header #";\s*")
          cookie-map (into {}
                           (keep (fn [cookie]
                                   (when-let [[k v] (str/split cookie #"=" 2)]
                                     [(str/trim k) (str/trim v)]))
                                 cookies))]
      (get cookie-map cookie-name))))

(defn json-response
  "Create a standard JSON response.
   Options:
   - :status - HTTP status code (default: 200)
   - :headers - Additional headers (merged with Content-Type: application/json)
   - :cookies - Map of cookie-name -> cookie-value for Set-Cookie headers"
  [data & {:keys [status headers cookies]
           :or {status 200
                headers {}}}]
  (let [base-headers (merge {"Content-Type" "application/json"}
                            headers)
        cookie-headers (when cookies
                        {"Set-Cookie" (mapv (fn [[k v]]
                                             (cookie-string k v))
                                           cookies)})]
    {:status status
     :headers (merge base-headers cookie-headers)
     :body (json/write-str data)}))

(defn error-response
  "Create a standard error response.
   Options:
   - :status - HTTP status code (default: 500)
   - :error-code - Application-specific error code
   - :details - Additional error details"
  [message & {:keys [status error-code details]
              :or {status 500}}]
  (json-response (cond-> {:error message}
                   error-code (assoc :error-code error-code)
                   details (assoc :details details))
                 :status status))
