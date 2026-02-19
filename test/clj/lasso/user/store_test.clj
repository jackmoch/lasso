(ns lasso.user.store-test
  "Tests for user store operations with mocked Firestore client."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.user.store :as user-store]
            [lasso.firestore.client :as fs]))

;; =============================================================================
;; Shared state for mocking
;; =============================================================================

(def ^:private test-docs (atom {}))
(def ^:private test-subdocs (atom {}))

(defn- reset-stores-fixture [f]
  (reset! test-docs {})
  (reset! test-subdocs {})
  (f))

(use-fixtures :each reset-stores-fixture)

;; =============================================================================
;; Mock helpers
;; =============================================================================

(defn- make-fs-mocks
  "Return a map of mock Firestore client functions for use with with-redefs."
  []
  {:enabled?                (fn [] true)
   :get-doc                 (fn [collection id]
                              (get-in @test-docs [collection id]))
   :set-doc!                (fn [collection id data]
                              (swap! test-docs assoc-in [collection id]
                                     (into {} (map (fn [[k v]] [(keyword k) v]) data))))
   :update-doc!             (fn [collection id data]
                              (when (get-in @test-docs [collection id])
                                (swap! test-docs update-in [collection id]
                                       merge (into {} (map (fn [[k v]] [(keyword k) v]) data)))))
   :set-subcollection-doc!  (fn [username session-id data]
                              (swap! test-subdocs assoc-in [username session-id]
                                     (into {} (map (fn [[k v]] [(keyword k) v]) data))))
   :update-subcollection-doc! (fn [username session-id data]
                                (swap! test-subdocs update-in [username session-id]
                                       merge (into {} (map (fn [[k v]] [(keyword k) v]) data))))
   :query-subcollection     (fn [username _limit]
                              (vec (vals (get @test-subdocs username {}))))
   :increment-field         (fn [n] n)})

;; =============================================================================
;; upsert-user tests
;; =============================================================================

(deftest upsert-user-creates-new-user
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?  (:enabled? mocks)
                  fs/get-doc   (:get-doc mocks)
                  fs/set-doc!  (:set-doc! mocks)
                  fs/update-doc! (:update-doc! mocks)]
      (user-store/upsert-user "alice" "encrypted-key-123")
      (let [doc (get-in @test-docs ["users" "alice"])]
        (is (some? doc) "Document should be created")
        (is (= "alice" (:username doc)))
        (is (= "encrypted-key-123" (:encrypted_session_key doc)))
        (is (= 0 (:total_scrobbles doc)))
        (is (some? (:first_seen doc)))
        (is (some? (:last_seen doc)))))))

(deftest upsert-user-updates-existing-user
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?    (:enabled? mocks)
                  fs/get-doc     (:get-doc mocks)
                  fs/set-doc!    (:set-doc! mocks)
                  fs/update-doc! (:update-doc! mocks)]
      ;; Pre-populate the doc
      (swap! test-docs assoc-in ["users" "alice"]
             {:username "alice" :encrypted_session_key "old-key"
              :first_seen 1000 :last_seen 1000 :total_scrobbles 42})
      (user-store/upsert-user "alice" "new-key")
      (let [doc (get-in @test-docs ["users" "alice"])]
        (is (= "new-key" (:encrypted_session_key doc)) "Session key should update")
        (is (= 1000 (:first_seen doc)) "first_seen must not change on update")
        (is (= 42 (:total_scrobbles doc)) "total_scrobbles must not change on update")))))

(deftest upsert-user-noop-when-firestore-disabled
  (with-redefs [fs/enabled? (fn [] false)]
    ;; Should not throw or mutate anything
    (is (nil? (user-store/upsert-user "alice" "key")))))

;; =============================================================================
;; add-session-record tests
;; =============================================================================

(deftest add-session-record-creates-record
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?               (:enabled? mocks)
                  fs/set-subcollection-doc! (:set-subcollection-doc! mocks)]
      (let [now 1706832000000]
        (user-store/add-session-record "alice" "sess-uuid-1" "radiohead" now)
        (let [doc (get-in @test-subdocs ["alice" "sess-uuid-1"])]
          (is (some? doc))
          (is (= "sess-uuid-1" (:id doc)))
          (is (= "radiohead" (:target_username doc)))
          (is (= now (:started_at doc)))
          (is (= "active" (:state doc)))
          (is (= 0 (:scrobble_count doc)))
          (is (nil? (:ended_at doc))))))))

;; =============================================================================
;; finish-session-record tests
;; =============================================================================

(deftest finish-session-record-updates-record
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?                  (:enabled? mocks)
                  fs/get-doc                   (:get-doc mocks)
                  fs/update-subcollection-doc! (:update-subcollection-doc! mocks)
                  fs/update-doc!               (:update-doc! mocks)
                  fs/increment-field           (:increment-field mocks)]
      ;; Pre-populate user doc to allow update-doc! to work
      (swap! test-docs assoc-in ["users" "alice"]
             {:username "alice" :total_scrobbles 10})
      ;; Pre-populate subcollection doc
      (swap! test-subdocs assoc-in ["alice" "sess-1"]
             {:id "sess-1" :state "active" :scrobble_count 0})
      (user-store/finish-session-record "alice" "sess-1" 42 "completed")
      (let [doc (get-in @test-subdocs ["alice" "sess-1"])]
        (is (= "completed" (:state doc)))
        (is (= 42 (:scrobble_count doc)))
        (is (some? (:ended_at doc)))))))

(deftest finish-session-record-noop-with-nil-session-id
  (let [mocks (make-fs-mocks)
        calls (atom 0)]
    (with-redefs [fs/enabled?                  (:enabled? mocks)
                  fs/update-subcollection-doc! (fn [& _] (swap! calls inc))]
      (user-store/finish-session-record "alice" nil 10 "stopped")
      (is (zero? @calls) "Should not call Firestore when fs-session-id is nil"))))

;; =============================================================================
;; get-user-profile tests
;; =============================================================================

(deftest get-user-profile-returns-user-with-sessions
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?              (:enabled? mocks)
                  fs/get-doc               (:get-doc mocks)
                  fs/query-subcollection   (:query-subcollection mocks)]
      (swap! test-docs assoc-in ["users" "alice"]
             {:username "alice" :total_scrobbles 99 :first_seen 1000})
      (swap! test-subdocs assoc-in ["alice" "s1"]
             {:id "s1" :target_username "radiohead" :scrobble_count 42})
      (let [profile (user-store/get-user-profile "alice")]
        (is (some? profile))
        (is (= "alice" (:username profile)))
        (is (= 99 (:total_scrobbles profile)))
        (is (= 1 (count (:sessions profile))))))))

(deftest get-user-profile-returns-nil-when-firestore-disabled
  (with-redefs [fs/enabled? (fn [] false)]
    (is (nil? (user-store/get-user-profile "alice")))))

(deftest get-user-profile-returns-empty-profile-for-unknown-user
  ;; get-user-profile no longer returns nil for unknown users — it returns a
  ;; profile map with nil fields so sessions are still shown even if the user
  ;; doc was never written (e.g. Firestore unavailable during first login).
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?              (:enabled? mocks)
                  fs/get-doc               (:get-doc mocks)
                  fs/query-subcollection   (fn [_ _] [])]
      (let [profile (user-store/get-user-profile "unknown-user")]
        (is (= "unknown-user" (:username profile)))
        (is (nil? (:first_seen profile)))
        (is (= 0 (:total_scrobbles profile)))
        (is (= [] (:sessions profile)))))))

;; =============================================================================
;; get-latest-active-session tests
;; =============================================================================

(deftest get-latest-active-session-returns-nil-when-no-sessions
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?            (:enabled? mocks)
                  fs/query-subcollection (:query-subcollection mocks)]
      (is (nil? (user-store/get-latest-active-session "alice"))))))

(deftest get-latest-active-session-skips-non-active-sessions
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?            (:enabled? mocks)
                  fs/query-subcollection (:query-subcollection mocks)]
      ;; Populate with completed and stopped sessions only
      (swap! test-subdocs assoc-in ["alice" "s1"]
             {:id "s1" :target_username "radiohead" :state "completed" :started_at 1000})
      (swap! test-subdocs assoc-in ["alice" "s2"]
             {:id "s2" :target_username "radiohead" :state "stopped" :started_at 2000})
      (is (nil? (user-store/get-latest-active-session "alice"))))))

(deftest get-latest-active-session-returns-newest-active-session
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?            (:enabled? mocks)
                  fs/query-subcollection (:query-subcollection mocks)]
      ;; Populate with two active sessions — should return the newer one
      (swap! test-subdocs assoc-in ["alice" "s-old"]
             {:id "s-old" :target_username "radiohead" :state "active" :started_at 1000})
      (swap! test-subdocs assoc-in ["alice" "s-new"]
             {:id "s-new" :target_username "bjork" :state "active" :started_at 5000})
      (let [result (user-store/get-latest-active-session "alice")]
        (is (some? result))
        (is (= "s-new" (:id result)))
        (is (= "bjork" (:target_username result)))))))

(deftest get-latest-active-session-returns-nil-when-firestore-disabled
  (with-redefs [fs/enabled? (fn [] false)]
    (is (nil? (user-store/get-latest-active-session "alice")))))
