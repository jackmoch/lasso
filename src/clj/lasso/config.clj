(ns lasso.config
  "Environment configuration management for Lasso application.")

(defn get-env
  "Get environment variable with optional default value."
  [key & [default]]
  (or (System/getenv key) default))

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
