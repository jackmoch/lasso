(ns lasso.session.store
  "In-memory session storage with atomic operations."
  (:require [clojure.set :as set]))

(defonce sessions
  ;; Atom storing all user sessions. Map of session-id -> session data.
  (atom {}))

(defn create-session
  "Create a new session and store it.
   Returns the session data."
  [session-id username session-key]
  (let [now (System/currentTimeMillis)
        session {:session-id session-id
                 :username username
                 :session-key session-key
                 :created-at now
                 :last-activity now
                 :following-session nil}]
    (swap! sessions assoc session-id session)
    session))

(defn get-session
  "Retrieve a session by ID. Returns nil if not found."
  [session-id]
  (get @sessions session-id))

(defn update-session
  "Update a session atomically using a function.
   The function receives the current session data and returns updated data.
   Returns the updated session."
  [session-id update-fn]
  (let [result (atom nil)]
    (swap! sessions
           (fn [sessions-map]
             (if-let [session (get sessions-map session-id)]
               (let [updated (update-fn session)]
                 (reset! result updated)
                 (assoc sessions-map session-id updated))
               (do
                 (reset! result nil)
                 sessions-map))))
    @result))

(defn delete-session
  "Remove a session from storage."
  [session-id]
  (swap! sessions dissoc session-id)
  nil)

(defn touch-session
  "Update the last-activity timestamp for a session."
  [session-id]
  (update-session session-id
                  (fn [session]
                    (assoc session :last-activity (System/currentTimeMillis)))))

(defn get-active-following-sessions
  "Query all sessions that have an active following session.
   Returns a sequence of session maps."
  []
  (->> @sessions
       vals
       (filter (fn [session]
                 (and (:following-session session)
                      (= :active (get-in session [:following-session :state])))))))

(defn count-sessions
  "Return the total number of active sessions."
  []
  (count @sessions))

(defn clear-all-sessions!
  "Clear all sessions. Used for testing and development."
  []
  (reset! sessions {}))
