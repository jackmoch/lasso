(ns lasso.user.store
  "Firestore-backed user profile and session history operations."
  (:require [lasso.firestore.client :as fs]
            [taoensso.timbre :as log]))

(def ^:private users-collection "users")

;; =============================================================================
;; User document CRUD
;; =============================================================================

(defn upsert-user
  "Create or update the user document in Firestore.
   On first login: creates document with first_seen, last_seen, total_scrobbles=0.
   On subsequent logins: updates last_seen and encrypted_session_key only.
   No-op if Firestore is unavailable."
  [username encrypted-session-key]
  (when (fs/enabled?)
    (let [now (System/currentTimeMillis)
          existing (fs/get-doc users-collection username)]
      (if existing
        (fs/update-doc! users-collection username
                        {"encrypted_session_key" encrypted-session-key
                         "last_seen" now})
        (fs/set-doc! users-collection username
                     {"username" username
                      "encrypted_session_key" encrypted-session-key
                      "first_seen" now
                      "last_seen" now
                      "total_scrobbles" 0}))
      (log/info "Upserted user in Firestore" {:username username}))))

(defn get-user
  "Fetch a user document from Firestore. Returns nil if not found or unavailable."
  [username]
  (fs/get-doc users-collection username))

(defn update-last-target
  "Update last_target on the user document when a following session starts."
  [username target-username]
  (when (fs/enabled?)
    (fs/update-doc! users-collection username {"last_target" target-username})))

;; =============================================================================
;; Session history (subcollection)
;; =============================================================================

(defn add-session-record
  "Write a new following-session record to Firestore.
   Returns the generated session record ID."
  [username fs-session-id target-username started-at]
  (when (fs/enabled?)
    (fs/set-subcollection-doc!
     username fs-session-id
     {"id" fs-session-id
      "target_username" target-username
      "started_at" started-at
      "ended_at" nil
      "scrobble_count" 0
      "state" "active"})
    (log/info "Created Firestore session record"
              {:username username :session-id fs-session-id}))
  fs-session-id)

(defn finish-session-record
  "Update a session record as completed or stopped and increment the user's
   total_scrobbles counter. No-op if Firestore is unavailable."
  [username fs-session-id scrobble-count state]
  (when (and (fs/enabled?) fs-session-id)
    (let [now (System/currentTimeMillis)]
      (fs/update-subcollection-doc!
       username fs-session-id
       {"ended_at" now
        "scrobble_count" scrobble-count
        "state" state})
      (when (pos? scrobble-count)
        (fs/update-doc! users-collection username
                        {"total_scrobbles" (fs/increment-field scrobble-count)}))
      (log/info "Finished Firestore session record"
                {:username username :session-id fs-session-id :state state}))))

(defn get-latest-active-session
  "Fetch the most recent active following-session record for a user from Firestore.
   Returns nil if Firestore is unavailable, no sessions exist, or none are active."
  [username]
  (when (fs/enabled?)
    (->> (fs/query-subcollection username 10)
         (filter #(= "active" (:state %)))
         (sort-by :started_at >)
         first)))

(defn get-user-profile
  "Return the user document merged with up to 50 recent session records,
   sorted by started_at descending.
   Returns nil if Firestore unavailable. Returns a profile map with empty
   fields if the user doc doesn't exist yet (sessions are still fetched)."
  [username]
  (when (fs/enabled?)
    (let [user     (fs/get-doc users-collection username)
          sessions (or (fs/query-subcollection username 50) [])]
      {:username        username
       :first_seen      (:first_seen user)
       :last_seen       (:last_seen user)
       :total_scrobbles (or (:total_scrobbles user) 0)
       :last_target     (:last_target user)
       :sessions        sessions})))
