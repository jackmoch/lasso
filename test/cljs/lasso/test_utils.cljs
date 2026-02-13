(ns lasso.test-utils
  "Test utilities for Re-frame testing"
  (:require [re-frame.core :as rf]
            [re-frame.db :as rf-db]
            [cljs.test :refer [is]]))

;; Register no-op effect handlers for testing
(rf/reg-fx :http-xhrio (fn [_] nil))

;; ============================================================================
;; Basic DB Utilities
;; ============================================================================

(defn reset-db!
  "Reset Re-frame db to empty state."
  []
  (rf/clear-subscription-cache!)
  (reset! rf-db/app-db {}))

(defn get-db
  "Get current Re-frame db."
  []
  @rf-db/app-db)

(defn set-db!
  "Set Re-frame db to specific state."
  [db]
  (reset! rf-db/app-db db))

(defn get-in-db
  "Get value at path in db."
  [path]
  (get-in (get-db) path))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(def empty-db {})

(def default-db
  {:auth {:authenticated? false
          :username nil
          :checking? false}
   :session {:state :not-started
             :target-username nil
             :scrobble-count 0
             :recent-scrobbles []
             :started-at nil
             :last-poll nil}
   :ui {:loading? false
        :error nil
        :session-control-loading? false
        :polling? false}})

(def authenticated-db
  (assoc-in default-db [:auth] {:authenticated? true
                                 :username "testuser"
                                 :checking? false}))

(def active-session-db
  (-> authenticated-db
      (assoc-in [:session :state] :active)
      (assoc-in [:session :target-username] "target-user")
      (assoc-in [:session :scrobble-count] 5)
      (assoc-in [:session :recent-scrobbles]
                [{:artist "Artist 1" :track "Track 1" :timestamp 1234567890}
                 {:artist "Artist 2" :track "Track 2" :timestamp 1234567900}])))

(def paused-session-db
  (assoc-in active-session-db [:session :state] :paused))

;; ============================================================================
;; Test Helpers
;; ============================================================================

(defn with-fresh-db [f]
  "Fixture to reset db before each test."
  (reset-db!)
  (try
    (f)
    (finally
      (reset-db!))))
