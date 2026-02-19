(ns lasso.middleware
  "Pedestal interceptors for authentication and request processing."
  (:require [io.pedestal.interceptor :as interceptor]
            [lasso.auth.session :as auth-session]
            [lasso.util.http :as http]
            [lasso.util.crypto :as crypto]
            [lasso.session.store :as store]
            [lasso.user.remember :as remember]
            [lasso.user.store :as user-store]
            [lasso.config :as config]))

(def require-auth
  "Interceptor that requires a valid session cookie.
   Primary path: checks session-id cookie → in-memory store.
   Fallback path: checks lasso-remember cookie → Firestore → restores session.
   Returns 401 if neither is valid.
   Sets a fresh session-id cookie in the response when restoring from remember-me."
  (interceptor/interceptor
   {:name ::require-auth
    :enter (fn [context]
             (let [request    (:request context)
                   session-id (http/parse-cookie request "session-id")]
               (if-let [session (and session-id (store/get-session session-id))]
                 ;; Valid session-id — attach to request and touch activity
                 (do
                   (auth-session/touch session-id)
                   (assoc-in context [:request :session] session))
                 ;; No valid session-id — try remember-me fallback
                 (let [remember-token (http/parse-cookie request "lasso-remember")
                       user           (remember/lookup-token remember-token)]
                   (if user
                     ;; Remember-me valid — restore session from Firestore data
                     (let [secret    (get-in config/config [:session :secret])
                           plain-key (crypto/decrypt (:encrypted_session_key user) secret)
                           {:keys [session-id session-data]}
                           (let [r (auth-session/create-session (:username user) plain-key)]
                             {:session-id   (:session-id r)
                              :session-data (:session-data r)})]
                       ;; Re-upsert user to keep last_seen current and create the doc
                       ;; if the initial OAuth write failed (e.g. IAM not yet propagated)
                       (user-store/upsert-user (:username user) (:encrypted_session_key user))
                       (-> context
                           (assoc-in [:request :session] session-data)
                           ;; Store new session-id so :leave can set the cookie
                           (assoc ::new-session-id session-id)))
                     ;; No valid remember-me — 401
                     (assoc context :response
                            (http/error-response "Authentication required"
                                                 :status 401
                                                 :error-code "AUTH_REQUIRED")))))))

    :leave (fn [context]
             ;; If we restored from remember-me, set fresh session-id cookie
             (if-let [new-id (::new-session-id context)]
               (let [is-prod? (= :production (:environment config/config))
                     cookie   (http/cookie-string "session-id" new-id
                                                  :max-age (* 60 60 24 7)
                                                  :path "/"
                                                  :http-only true
                                                  :secure is-prod?
                                                  :same-site "Lax")]
                 (update-in context [:response :headers "Set-Cookie"]
                            (fn [existing]
                              (cond
                                (vector? existing) (conj existing cookie)
                                existing           [existing cookie]
                                :else              cookie))))
               context))}))

(defn get-session
  "Extract session data from request (attached by require-auth interceptor).
   Returns nil if no session present."
  [request]
  (:session request))

(defn get-session-id
  "Extract session ID from request session data.
   Returns nil if no session present."
  [request]
  (when-let [session (get-session request)]
    (:session-id session)))
