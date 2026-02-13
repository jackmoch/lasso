(ns lasso.lastfm.oauth
  "Last.fm OAuth 2.0 authentication methods."
  (:require [lasso.lastfm.client :as client]
            [lasso.config :as config]))

(defn get-token
  "Request an authentication token from Last.fm.
   Returns {:token \"...\"} or {:error \"...\"}."
  []
  (client/api-request {:method "auth.getToken"
                       :signed false}))

(defn generate-auth-url
  "Generate the URL for user to authorize the application.
   For WEB authentication flow: No token needed - Last.fm generates the token
   and passes it to the callback URL after user authorization.
   Includes callback URL so Last.fm knows where to redirect after authorization."
  []
  (let [api-key (get-in config/config [:lastfm :api-key])
        callback-url (get-in config/config [:lastfm :callback-url])]
    (str "https://www.last.fm/api/auth/?api_key=" api-key
         "&cb=" (java.net.URLEncoder/encode callback-url "UTF-8"))))

(defn get-session-key
  "Exchange an authorized token for a session key.
   Token must have been authorized by the user.
   Returns {:session {:name \"username\" :key \"session-key\"}} or {:error \"...\"}."
  [token]
  (client/api-request {:method "auth.getSession"
                       :params {:token token}
                       :signed true}))
