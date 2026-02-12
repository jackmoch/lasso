(ns lasso.auth.session
  "Session lifecycle management with encryption."
  (:require [lasso.session.store :as store]
            [lasso.util.crypto :as crypto]
            [lasso.util.http :as http]
            [lasso.config :as config]))

(defn create-session
  "Create a new authenticated session with encrypted Last.fm session key.
   Returns a map with :session-id and :session-data."
  [username lastfm-session-key]
  (let [session-id (crypto/generate-uuid)
        encryption-secret (get-in config/config [:session :secret])
        encrypted-key (crypto/encrypt lastfm-session-key encryption-secret)
        session-data (store/create-session session-id username encrypted-key)]
    {:session-id session-id
     :session-data session-data}))

(defn destroy-session
  "Completely remove a session."
  [session-id]
  (store/delete-session session-id))

(defn get-session-id
  "Extract session ID from request cookie.
   Returns nil if not found."
  [request]
  (http/parse-cookie request "session-id"))

(defn get-session
  "Get session data by ID, returning nil if not found."
  [session-id]
  (store/get-session session-id))

(defn encrypt-session-key
  "Encrypt a Last.fm session key for storage."
  [session-key]
  (let [encryption-secret (get-in config/config [:session :secret])]
    (crypto/encrypt session-key encryption-secret)))

(defn decrypt-session-key
  "Decrypt a stored Last.fm session key."
  [encrypted-key]
  (let [encryption-secret (get-in config/config [:session :secret])]
    (crypto/decrypt encrypted-key encryption-secret)))

(defn get-decrypted-session-key
  "Get the decrypted Last.fm session key for a session.
   Returns nil if session not found."
  [session-id]
  (when-let [session (store/get-session session-id)]
    (decrypt-session-key (:session-key session))))

(defn update-session
  "Update session using a function. See store/update-session."
  [session-id update-fn]
  (store/update-session session-id update-fn))

(defn touch
  "Update last activity timestamp for a session."
  [session-id]
  (store/touch-session session-id))
