(ns lasso.user.remember
  "Remember-me token management backed by Firestore.
   Tokens are secure random UUIDs stored in the `remember_tokens` collection.
   Each token expires 90 days after creation."
  (:require [lasso.firestore.client :as fs]
            [lasso.util.crypto :as crypto]
            [taoensso.timbre :as log]))

(def ^:private collection "remember_tokens")
(def ^:private token-ttl-ms (* 90 24 60 60 1000))   ; 90 days in ms
(def token-max-age-seconds (* 90 24 60 60))           ; 90 days in seconds

(defn save-token!
  "Persist a new remember-me token linked to username.
   The token itself is the document ID (a UUID).
   No-op if Firestore is unavailable."
  [token username]
  (when (fs/enabled?)
    (let [now (System/currentTimeMillis)
          expires-at (+ now token-ttl-ms)]
      (fs/set-doc! collection token
                   {"username" username
                    "created_at" now
                    "expires_at" expires-at})
      (log/info "Saved remember-me token" {:username username}))))

(defn token-expired?
  "Returns true if the token document is past its expires_at timestamp."
  [token-doc]
  (let [expires-at (:expires_at token-doc)]
    (or (nil? expires-at)
        (> (System/currentTimeMillis) expires-at))))

(defn lookup-token
  "Look up a remember-me token. Returns the token document map (with :username
   and :encrypted_session_key from the user document) if valid and not expired,
   or nil if missing, expired, or Firestore unavailable."
  [token]
  (when (and token (fs/enabled?))
    (try
      (when-let [token-doc (fs/get-doc collection token)]
        (if (token-expired? token-doc)
          (do
            (log/info "Remember-me token expired, ignoring" {:token (subs token 0 8)})
            (fs/delete-doc! collection token)
            nil)
          ;; Token valid — fetch user document for encrypted_session_key
          (when-let [user-doc (fs/get-doc "users" (:username token-doc))]
            (merge token-doc user-doc))))
      (catch Exception e
        (log/error e "Error looking up remember-me token")
        nil))))

(defn delete-token!
  "Delete a remember-me token on logout. No-op if unavailable or token nil."
  [token]
  (when (and token (fs/enabled?))
    (fs/delete-doc! collection token)
    (log/info "Deleted remember-me token")))

(defn generate-token
  "Generate a new secure random token (UUID string)."
  []
  (crypto/generate-uuid))
