(ns lasso.user.handlers
  "HTTP handlers for user profile API."
  (:require [lasso.user.store :as user-store]
            [lasso.firestore.client :as fs]
            [lasso.util.http :as http]
            [taoensso.timbre :as log]))

(defn profile-handler
  "GET /api/user/profile
   Returns the authenticated user's profile and session history.
   Requires authentication (via require-auth interceptor).
   Returns 503 if Firestore is not available."
  [request]
  (try
    (let [username (get-in request [:session :username])]
      (if-not (fs/enabled?)
        (http/error-response "Profile history unavailable — persistence not configured"
                             :status 503
                             :error-code "PERSISTENCE_UNAVAILABLE")
        (if-let [profile (user-store/get-user-profile username)]
          (http/json-response
           {:username        (:username profile)
            :first_seen      (:first_seen profile)
            :last_seen       (:last_seen profile)
            :total_scrobbles (or (:total_scrobbles profile) 0)
            :last_target     (:last_target profile)
            :sessions        (mapv (fn [s]
                                     {:id              (:id s)
                                      :target_username (:target_username s)
                                      :started_at      (:started_at s)
                                      :ended_at        (:ended_at s)
                                      :scrobble_count  (or (:scrobble_count s) 0)
                                      :state           (:state s)})
                                   (or (:sessions profile) []))})
          ;; User not in Firestore yet (no OAuth login with Firestore enabled)
          (http/json-response
           {:username        username
            :first_seen      nil
            :last_seen       nil
            :total_scrobbles 0
            :last_target     nil
            :sessions        []}))))
    (catch Exception e
      (log/error e "Error in profile-handler")
      (http/error-response "Failed to retrieve profile"
                           :status 500
                           :error-code "PROFILE_ERROR"))))
