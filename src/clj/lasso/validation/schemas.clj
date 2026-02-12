(ns lasso.validation.schemas
  "Malli schemas for data validation throughout the application."
  (:require [malli.core :as m]
            [malli.error :as me]))

;; Session state enum
(def SessionState
  [:enum :not-started :active :paused :stopped])

;; Last.fm track schema
(def Track
  [:map
   [:artist :string]
   [:track :string]
   [:album {:optional true} [:maybe :string]]
   [:timestamp :int]])

;; Following session schema
(def FollowingSession
  [:map
   [:target-username :string]
   [:state SessionState]
   [:started-at :int]
   [:last-poll {:optional true} [:maybe :int]]
   [:scrobble-count {:optional true} [:int {:min 0}]]
   [:scrobble-cache {:optional true} [:set :string]]
   [:paused-at {:optional true} [:maybe :int]]
   [:stopped-at {:optional true} [:maybe :int]]])

;; Complete user session schema
(def UserSession
  [:map
   [:session-id :string]
   [:username :string]
   [:session-key :string] ; Encrypted Last.fm session key
   [:created-at :int]
   [:last-activity :int]
   [:following-session {:optional true} [:maybe FollowingSession]]])

;; API Request schemas
(def StartSessionRequest
  [:map
   [:target_username :string]])

(def ResumeSessionRequest
  [:map])

(def PauseSessionRequest
  [:map])

(def StopSessionRequest
  [:map])

;; API Response schemas
(def SessionStatusResponse
  [:map
   [:authenticated :boolean]
   [:username [:maybe :string]]
   [:session [:maybe
              [:map
               [:state SessionState]
               [:target-username [:maybe :string]]
               [:scrobble-count :int]
               [:recent-scrobbles [:vector Track]]
               [:started-at [:maybe :int]]
               [:last-poll [:maybe :int]]]]]])

(def ErrorResponse
  [:map
   [:error :string]
   [:error-code {:optional true} [:maybe :string]]
   [:details {:optional true} [:maybe :any]]])

;; OAuth schemas
(def OAuthToken
  [:map
   [:token :string]])

(def OAuthSession
  [:map
   [:name :string]
   [:key :string]])

;; Validation helper
(defn validate
  "Validate data against a schema. Returns validation result with :valid? flag."
  [schema data]
  (if (m/validate schema data)
    {:valid? true :data data}
    {:valid? false
     :errors (me/humanize (m/explain schema data))}))

(defn valid?
  "Check if data is valid against a schema. Returns boolean."
  [schema data]
  (m/validate schema data))

(defn explain-errors
  "Get human-readable error messages for validation failures."
  [schema data]
  (when-not (m/validate schema data)
    (me/humanize (m/explain schema data))))
