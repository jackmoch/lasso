(ns lasso.lastfm.oauth
  "Last.fm OAuth 2.0 authentication methods."
  (:require [lasso.lastfm.client :as client]
            [lasso.config :as config]))

(defn get-token
  "Request an authentication token from Last.fm.
   Returns {:token \"...\"} or {:error \"...\"}."
  []
  (client/api-request {:method "auth.getToken"
                       :signed true}))

(defn generate-auth-url
  "Generate the URL for user to authorize the application.
   Takes the token from get-token."
  [token]
  (let [api-key (get-in config/config [:lastfm :api-key])]
    (str "https://www.last.fm/api/auth/?api_key=" api-key "&token=" token)))

(defn get-session-key
  "Exchange an authorized token for a session key.
   Token must have been authorized by the user.
   Returns {:session {:name \"username\" :key \"session-key\"}} or {:error \"...\"}."
  [token]
  (client/api-request {:method "auth.getSession"
                       :params {:token token}
                       :signed true}))
