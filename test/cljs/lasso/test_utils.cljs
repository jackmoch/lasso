(ns lasso.test-utils
  "Test utilities for Re-frame testing"
  (:require [re-frame.core :as rf]
            [re-frame.db :as rf-db]
            [cljs.test :refer [is]]))

;; ============================================================================
;; Re-frame Test Utilities
;; ============================================================================

(defn reset-re-frame!
  "Reset Re-frame state to a clean slate.
   Call this before each test to ensure test isolation."
  []
  (rf/clear-subscription-cache!)
  (reset! rf-db/app-db {}))

(defn with-fresh-db
  "Execute test function with a fresh Re-frame db.
   Automatically resets before and after the test."
  [f]
  (reset-re-frame!)
  (try
    (f)
    (finally
      (reset-re-frame!))))

(defn test-db
  "Get the current Re-frame db value."
  []
  @rf-db/app-db)

(defn set-db!
  "Set the Re-frame db to a specific state."
  [db]
  (reset! rf-db/app-db db))

(defn dispatch-sync
  "Dispatch an event synchronously and return the db state.
   Useful for testing event handlers."
  [event]
  (rf/dispatch-sync event)
  (test-db))

(defn subscribe-value
  "Get the current value of a subscription.
   Returns the dereferenced subscription value."
  [query]
  (let [sub (rf/subscribe query)]
    @sub))

;; ============================================================================
;; Assertion Helpers
;; ============================================================================

(defn db-contains?
  "Assert that the db contains a value at the given path."
  [path expected]
  (is (= expected (get-in (test-db) path))
      (str "Expected db path " path " to contain " expected)))

(defn db-has-key?
  "Assert that the db has a specific key path."
  [path]
  (is (some? (get-in (test-db) path))
      (str "Expected db to have key path " path)))

(defn db-missing-key?
  "Assert that the db does NOT have a specific key path."
  [path]
  (is (nil? (get-in (test-db) path))
      (str "Expected db to NOT have key path " path)))

(defn subscription-equals?
  "Assert that a subscription returns the expected value."
  [query expected]
  (is (= expected (subscribe-value query))
      (str "Expected subscription " query " to return " expected)))

;; ============================================================================
;; Event Handler Testing
;; ============================================================================

(defn test-event-handler
  "Test an event handler with a given db state and event.
   Returns the resulting db state.

   Usage:
   (test-event-handler initial-db [:event-id arg1 arg2])"
  [db event]
  (set-db! db)
  (dispatch-sync event))

(defn test-fx-event-handler
  "Test an -fx event handler that returns effects.
   Returns the effects map (not just the db).

   Usage:
   (let [effects (test-fx-event-handler initial-db [:event-id arg1])]
     (is (= (:dispatch effects) [:other-event])))"
  [db event]
  (let [handler (rf/reg-event-fx (first event))
        cofx {:db db}
        effects (handler cofx event)]
    effects))

;; ============================================================================
;; Mock Utilities
;; ============================================================================

(defn mock-http-success
  "Create a mock HTTP success response effect.
   Use this to test event handlers that trigger HTTP calls."
  [on-success result]
  {:http-xhrio {:on-success on-success
                :on-failure [:http-failure]
                :method :get
                :uri "/test"}})

(defn mock-http-failure
  "Create a mock HTTP failure response effect."
  [on-failure error]
  {:http-xhrio {:on-failure on-failure
                :on-success [:http-success]
                :method :get
                :uri "/test"}})

(defn simulate-http-success
  "Simulate a successful HTTP response by dispatching the on-success event."
  [on-success-event result]
  (dispatch-sync (conj on-success-event result)))

(defn simulate-http-failure
  "Simulate a failed HTTP response by dispatching the on-failure event."
  [on-failure-event error]
  (dispatch-sync (conj on-failure-event error)))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(def empty-db
  "Empty initial db state."
  {})

(def authenticated-db
  "DB state with authenticated user."
  {:auth {:authenticated? true
          :username "testuser"}
   :session {:state :not-started
             :target-username nil
             :scrobble-count 0
             :recent-scrobbles []
             :last-poll nil}
   :ui {:loading? false
        :error nil}})

(def active-session-db
  "DB state with active session."
  (assoc-in authenticated-db [:session]
            {:state :active
             :target-username "target-user"
             :scrobble-count 5
             :recent-scrobbles [{:artist "Artist 1"
                                 :track "Track 1"
                                 :timestamp 1234567890}
                                {:artist "Artist 2"
                                 :track "Track 2"
                                 :timestamp 1234567900}]
             :last-poll 1234567890}))

(def paused-session-db
  "DB state with paused session."
  (assoc-in active-session-db [:session :state] :paused))

;; ============================================================================
;; Test Data Generators
;; ============================================================================

(defn gen-scrobble
  "Generate a mock scrobble for testing."
  ([artist track]
   (gen-scrobble artist track (.now js/Date)))
  ([artist track timestamp]
   {:artist artist
    :track track
    :timestamp timestamp}))

(defn gen-scrobbles
  "Generate multiple mock scrobbles."
  [n]
  (mapv #(gen-scrobble (str "Artist " %) (str "Track " %) (+ 1234567890 (* % 60)))
        (range n)))

;; ============================================================================
;; Async Test Helpers
;; ============================================================================

(defn wait-for
  "Wait for a predicate to become true (max 5 seconds).
   Useful for testing async operations.

   Usage:
   (wait-for #(= :active (get-in (test-db) [:session :state])))"
  ([pred]
   (wait-for pred 5000))
  ([pred timeout-ms]
   (let [start (.now js/Date)
         check-interval 50]
     (loop []
       (cond
         (pred) true
         (> (- (.now js/Date) start) timeout-ms) false
         :else (do
                 (js/setTimeout #() check-interval)
                 (recur)))))))

(defn wait-for-dispatch
  "Dispatch an event and wait for a condition to be true.

   Usage:
   (wait-for-dispatch [:start-session \"user\"]
                      #(= :active (get-in (test-db) [:session :state])))"
  [event pred]
  (rf/dispatch event)
  (wait-for pred))
