(ns lasso.api
  "Backend API client for Lasso application."
  (:require [ajax.core :as ajax]))

(def api-base "/api")

(defn api-request
  "Generic HTTP request helper using cljs-ajax.
   Returns effect map for re-frame http-fx."
  [method endpoint {:keys [params on-success on-failure]}]
  {:method method
   :uri (str api-base endpoint)
   :params (or params {})
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :with-credentials true  ; Send session cookie
   :on-success on-success
   :on-failure on-failure})

;; -- Authentication API --

(defn init-auth
  "Initialize OAuth flow.
   Returns effect map for POST /api/auth/init."
  [on-success on-failure]
  (api-request :post "/auth/init"
               {:on-success on-success
                :on-failure on-failure}))

(defn logout
  "Logout and destroy session.
   Returns effect map for POST /api/auth/logout."
  [on-success on-failure]
  (api-request :post "/auth/logout"
               {:on-success on-success
                :on-failure on-failure}))

;; -- Session Management API --

(defn check-session-status
  "Check current session status.
   Returns effect map for GET /api/session/status."
  [on-success on-failure]
  (api-request :get "/session/status"
               {:on-success on-success
                :on-failure on-failure}))

(defn start-session
  "Start following a target Last.fm user.
   Returns effect map for POST /api/session/start."
  [username on-success on-failure]
  (api-request :post "/session/start"
               {:params {:target_username username}
                :on-success on-success
                :on-failure on-failure}))

(defn pause-session
  "Pause active session.
   Returns effect map for POST /api/session/pause."
  [on-success on-failure]
  (api-request :post "/session/pause"
               {:on-success on-success
                :on-failure on-failure}))

(defn resume-session
  "Resume paused session.
   Returns effect map for POST /api/session/resume."
  [on-success on-failure]
  (api-request :post "/session/resume"
               {:on-success on-success
                :on-failure on-failure}))

(defn stop-session
  "Stop and clear session.
   Returns effect map for POST /api/session/stop."
  [on-success on-failure]
  (api-request :post "/session/stop"
               {:on-success on-success
                :on-failure on-failure}))
