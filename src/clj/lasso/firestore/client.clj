(ns lasso.firestore.client
  "Firestore client with graceful fallback when unavailable.
   All operations are no-ops when Firestore is not configured.

   Collection names are automatically prefixed based on ENVIRONMENT:
     development → 'dev-users', 'dev-remember_tokens'
     staging     → 'staging-users', 'staging-remember_tokens'
     production  → 'users', 'remember_tokens'

   This ensures data isolation across environments that share a single GCP project."
  (:require [taoensso.timbre :as log]
            [lasso.config :as config])
  (:import [com.google.cloud.firestore FirestoreOptions FieldValue
            Query$Direction]))

(defonce ^:private db-atom (atom nil))

(defn init!
  "Initialise the Firestore client using Application Default Credentials.
   Explicitly sets the project ID from config so the correct GCP project is
   used regardless of GOOGLE_CLOUD_PROJECT environment variable presence.
   Logs a warning and disables persistence if credentials are unavailable."
  []
  (let [project-id (get-in config/config [:firestore :project-id])]
    (try
      (reset! db-atom (-> (FirestoreOptions/newBuilder)
                          (.setProjectId project-id)
                          .build
                          .getService))
      (log/info "Firestore client initialised"
                {:prefix (get-in config/config [:firestore :collection-prefix])
                 :project project-id})
      (catch Exception e
        (log/warn "Firestore unavailable — persistence disabled:" (.getMessage e))))))

(defn enabled?
  "Returns true if the Firestore client is initialised."
  []
  (some? @db-atom))

(defn- db [] @db-atom)

(defn- prefix-collection
  "Apply the environment-specific prefix to a collection name."
  [collection]
  (str (get-in config/config [:firestore :collection-prefix]) collection))

(defn- ->str-keys
  "Convert a Clojure map to a java.util.HashMap with String keys.
   Values that are keyword are converted to strings; other values pass through."
  [m]
  (java.util.HashMap.
   (into {} (map (fn [[k v]]
                   [(if (keyword? k) (name k) (str k)) v])
                 m))))

(defn- snap->map
  "Convert a Firestore document snapshot to a Clojure map with keyword keys.
   Returns nil if the document does not exist."
  [snap]
  (when (.exists snap)
    (into {} (map (fn [[k v]] [(keyword k) v]) (.getData snap)))))

;; =============================================================================
;; CRUD helpers
;; =============================================================================

(defn get-doc
  "Fetch a document by collection + id. Returns a keyword-keyed map or nil.
   Collection name is automatically prefixed for the current environment."
  [collection id]
  (when (enabled?)
    (try
      (-> (db)
          (.collection (prefix-collection collection))
          (.document id)
          .get
          .get
          snap->map)
      (catch Exception e
        (log/error e "Firestore get-doc error" {:collection collection :id id})
        nil))))

(defn set-doc!
  "Create or replace a document. No-op if Firestore unavailable.
   Collection name is automatically prefixed for the current environment."
  [collection id data]
  (when (enabled?)
    (try
      (-> (db)
          (.collection (prefix-collection collection))
          (.document id)
          (.set (->str-keys data))
          .get)
      (catch Exception e
        (log/error e "Firestore set-doc! error" {:collection collection :id id})))))

(defn update-doc!
  "Update specific fields in a document. No-op if Firestore unavailable.
   Supports FieldValue sentinels (e.g. FieldValue/increment) as values.
   Collection name is automatically prefixed for the current environment."
  [collection id data]
  (when (enabled?)
    (try
      (-> (db)
          (.collection (prefix-collection collection))
          (.document id)
          (.update (->str-keys data))
          .get)
      (catch Exception e
        (log/error e "Firestore update-doc! error" {:collection collection :id id})))))

(defn delete-doc!
  "Delete a document. No-op if Firestore unavailable.
   Collection name is automatically prefixed for the current environment."
  [collection id]
  (when (enabled?)
    (try
      (-> (db)
          (.collection (prefix-collection collection))
          (.document id)
          .delete
          .get)
      (catch Exception e
        (log/error e "Firestore delete-doc! error" {:collection collection :id id})))))

(defn query-subcollection
  "Query a subcollection at {prefix}users/{username}/sessions, ordered by started_at
   descending, limited to limit-n documents. Returns a vector of maps or nil."
  [username limit-n]
  (when (enabled?)
    (try
      (let [snaps (-> (db)
                      (.collection (prefix-collection "users"))
                      (.document username)
                      (.collection "sessions")
                      (.orderBy "started_at" Query$Direction/DESCENDING)
                      (.limit (int limit-n))
                      .get
                      .get
                      .getDocuments)
            results (mapv snap->map snaps)]
        (log/debug "query-subcollection" {:username username :count (count results)})
        results)
      (catch Exception e
        (log/error e "Firestore query-subcollection error" {:username username})
        nil))))

(defn set-subcollection-doc!
  "Create or replace a document in {prefix}users/{username}/sessions/{session-id}."
  [username session-id data]
  (when (enabled?)
    (try
      (-> (db)
          (.collection (prefix-collection "users"))
          (.document username)
          (.collection "sessions")
          (.document session-id)
          (.set (->str-keys data))
          .get)
      (catch Exception e
        (log/error e "Firestore set-subcollection-doc! error"
                   {:username username :session-id session-id})))))

(defn update-subcollection-doc!
  "Update fields in {prefix}users/{username}/sessions/{session-id}."
  [username session-id data]
  (when (enabled?)
    (try
      (-> (db)
          (.collection (prefix-collection "users"))
          (.document username)
          (.collection "sessions")
          (.document session-id)
          (.update (->str-keys data))
          .get)
      (catch Exception e
        (log/error e "Firestore update-subcollection-doc! error"
                   {:username username :session-id session-id})))))

(defn increment-field
  "Return a FieldValue sentinel for a server-side atomic increment by n."
  [n]
  (FieldValue/increment (long n)))
