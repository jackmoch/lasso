(ns lasso.config
  "Environment configuration management for Lasso application."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn load-env-file
  "Load environment variables from .env file.
   Returns a map of key -> value."
  []
  (try
    (when (.exists (io/file ".env"))
      (with-open [reader (io/reader ".env")]
        (->> (line-seq reader)
             (remove #(or (str/blank? %) (str/starts-with? % "#")))
             (map #(str/split % #"=" 2))
             (filter #(= 2 (count %)))
             (into {} (map (fn [[k v]] [k v]))))))
    (catch Exception e
      (println "Warning: Could not load .env file:" (.getMessage e))
      {})))

(def env-file-vars
  "Environment variables loaded from .env file."
  (load-env-file))

(defn get-env
  "Get environment variable with optional default value.
   Checks System environment first, then .env file, then default."
  [key & [default]]
  (or (System/getenv key)
      (get env-file-vars key)
      default))

(defn load-config
  "Load application configuration from environment variables."
  []
  {:server {:host (get-env "HOST" "0.0.0.0")
            :port (Integer/parseInt (get-env "PORT" "8080"))}
   :lastfm {:api-key (get-env "LASTFM_API_KEY")
            :api-secret (get-env "LASTFM_API_SECRET")
            :callback-url (get-env "OAUTH_CALLBACK_URL" "http://localhost:8080/api/auth/callback")}
   :session {:secret (get-env "SESSION_SECRET" "development-secret-change-in-production")}
   :polling {:interval-ms (Integer/parseInt (get-env "POLLING_INTERVAL_MS" "20000"))}
   :environment (keyword (get-env "ENVIRONMENT" "development"))})

(def config
  "Application configuration loaded from environment."
  (load-config))
