(ns lasso.polling.scheduler
  "Scheduler for managing polling loops across sessions."
  (:require [lasso.polling.engine :as engine]
            [lasso.session.store :as store]
            [lasso.config :as config]
            [clojure.core.async :as async :refer [go-loop <! >! timeout]]
            [clojure.set :as set]
            [taoensso.timbre :as log]))

(defonce active-pollers
  ;; Atom storing active polling loops. Map of session-id -> control channel.
  (atom {}))

(defn stop-poller
  "Stop the polling loop for a session."
  [session-id]
  (when-let [control-chan (get @active-pollers session-id)]
    (log/info "Stopping poller" {:session-id session-id})
    (async/close! control-chan)
    (swap! active-pollers dissoc session-id)))

(defn start-poller
  "Start a polling loop for a session.
   Polls at the configured interval until stopped."
  [session-id]
  (let [interval-ms (get-in config/config [:polling :interval-ms])
        control-chan (async/chan)]

    (log/info "Starting poller" {:session-id session-id
                                 :interval-ms interval-ms})

    ;; Store control channel
    (swap! active-pollers assoc session-id control-chan)

    ;; Start polling loop
    (go-loop []
      (let [timeout-chan (timeout interval-ms)
            [_ port] (async/alts! [control-chan timeout-chan])]

        (cond
          ;; Control channel closed - stop
          (= port control-chan)
          (log/info "Poller stopped" {:session-id session-id})

          ;; Timeout - execute poll
          (= port timeout-chan)
          (if-let [session (store/get-session session-id)]
            (let [state (get-in session [:following-session :state])]
              (if (= :active state)
                ;; Still active - poll and continue
                (do
                  (engine/execute-poll session-id)
                  (recur))
                ;; No longer active - stop
                (do
                  (log/info "Session no longer active, stopping poller"
                           {:session-id session-id :state state})
                  (stop-poller session-id))))
            ;; Session not found - stop
            (do
              (log/warn "Session not found, stopping poller" {:session-id session-id})
              (stop-poller session-id))))))

    control-chan))

(defn ensure-poller-for-session
  "Ensure a poller is running for an active session.
   Starts one if needed, does nothing if already running."
  [session-id]
  (when-let [session (store/get-session session-id)]
    (let [state (get-in session [:following-session :state])]
      (when (= :active state)
        (when-not (get @active-pollers session-id)
          (log/info "Starting poller for active session" {:session-id session-id})
          (start-poller session-id))))))

(defn sync-pollers
  "Synchronize pollers with current session state.
   Starts pollers for active sessions, stops for non-active."
  []
  (let [active-sessions (store/get-active-following-sessions)
        active-session-ids (set (map :session-id active-sessions))
        running-session-ids (set (keys @active-pollers))]

    ;; Stop pollers for sessions that are no longer active
    (doseq [session-id (set/difference running-session-ids active-session-ids)]
      (stop-poller session-id))

    ;; Start pollers for active sessions that don't have one
    (doseq [session-id (set/difference active-session-ids running-session-ids)]
      (start-poller session-id))))

(defn stop-all-pollers
  "Stop all active polling loops. Used for shutdown."
  []
  (log/info "Stopping all pollers" {:count (count @active-pollers)})
  (doseq [[session-id _] @active-pollers]
    (stop-poller session-id)))

(defn get-poller-status
  "Get status of all active pollers.
   Returns map of session-id -> {:active true}."
  []
  (into {}
        (map (fn [[session-id _]]
               [session-id {:active true}])
             @active-pollers)))

(defn handle-session-state-change
  "Handle a session state change. Start/stop poller as needed.
   Call this after any session state transition."
  [session-id new-state]
  (case new-state
    :active (ensure-poller-for-session session-id)
    :paused (stop-poller session-id)
    :stopped (stop-poller session-id)
    nil))
