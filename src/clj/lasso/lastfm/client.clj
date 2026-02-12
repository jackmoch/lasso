(ns lasso.lastfm.client
  "Last.fm API client with rate limiting."
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [lasso.config :as config]
            [lasso.util.crypto :as crypto]
            [taoensso.timbre :as log]))

(def api-base "https://ws.audioscrobbler.com/2.0/")

(def ^:private last-request-time (atom 0))
(def ^:private min-interval-ms 200) ; 5 req/sec = 200ms between requests

(defn- wait-for-rate-limit
  "Ensure we don't exceed Last.fm's rate limit of 5 req/sec."
  []
  (let [now (System/currentTimeMillis)
        elapsed (- now @last-request-time)]
    (when (< elapsed min-interval-ms)
      (Thread/sleep (- min-interval-ms elapsed)))
    (reset! last-request-time (System/currentTimeMillis))))

(defn generate-api-signature
  "Generate Last.fm API signature: MD5(sorted_params + api_secret).
   Params should be a map of parameter name -> value."
  [params]
  (let [api-secret (get-in config/config [:lastfm :api-secret])
        sorted-params (sort-by first params)
        signature-string (str (apply str (mapcat (fn [[k v]] [(name k) v]) sorted-params))
                             api-secret)]
    (crypto/md5 signature-string)))

(defn api-request
  "Make a request to the Last.fm API.
   Options:
   - :method - Last.fm API method (required)
   - :params - Additional parameters (map)
   - :signed - Whether to include API signature (default: false)
   - :format - Response format (default: json)"
  [{:keys [method params signed format]
    :or {signed false format "json"}}]
  (wait-for-rate-limit)
  (let [api-key (get-in config/config [:lastfm :api-key])
        base-params (merge {:method method
                            :api_key api-key
                            :format format}
                          params)
        final-params (if signed
                      (assoc base-params :api_sig (generate-api-signature
                                                   (dissoc base-params :format)))
                      base-params)]
    (try
      (log/debug "Last.fm API request:" method params)
      (let [;; Use POST for signed requests (writes/auth), GET for reads
            http-method (if signed http/post http/get)
            request-params (if signed
                            {:form-params final-params
                             :content-type :x-www-form-urlencoded
                             :as :json}
                            {:query-params final-params
                             :as :json})
            response (http-method api-base request-params)
            body (:body response)]
        (log/debug "Last.fm API response:" (pr-str body))
        (if (contains? body :error)
          {:error (:error body)
           :message (:message body)}
          body))
      (catch Exception e
        (log/error e "Last.fm API request failed:" method)
        {:error "api-error"
         :message (.getMessage e)}))))

(defn get-recent-tracks
  "Get recent tracks for a user.
   Options:
   - :from - Unix timestamp to get tracks from (optional)
   - :limit - Number of tracks to return (default: 50, max: 200)"
  [username & {:keys [from limit]
               :or {limit 50}}]
  (let [params (cond-> {:user username
                        :limit limit}
                 from (assoc :from from))]
    (api-request {:method "user.getRecentTracks"
                  :params params})))

(defn get-user-info
  "Get user information to validate username exists."
  [username]
  (api-request {:method "user.getInfo"
                :params {:user username}}))
