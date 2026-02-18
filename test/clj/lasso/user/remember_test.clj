(ns lasso.user.remember-test
  "Tests for remember-me token lifecycle."
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.user.remember :as remember]
            [lasso.firestore.client :as fs]))

;; =============================================================================
;; Mock helpers
;; =============================================================================

(def ^:private test-docs (atom {}))

(defn- make-fs-mocks []
  {:enabled?  (fn [] true)
   :get-doc   (fn [collection id]
                (get-in @test-docs [collection id]))
   :set-doc!  (fn [collection id data]
                (swap! test-docs assoc-in [collection id]
                       (into {} (map (fn [[k v]] [(keyword k) v]) data))))
   :delete-doc! (fn [collection id]
                  (swap! test-docs update collection dissoc id))})

;; =============================================================================
;; save-token! tests
;; =============================================================================

(deftest save-token-creates-document
  (reset! test-docs {})
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?  (:enabled? mocks)
                  fs/set-doc!  (:set-doc! mocks)]
      (remember/save-token! "test-token-uuid" "alice")
      (let [doc (get-in @test-docs ["remember_tokens" "test-token-uuid"])]
        (is (some? doc) "Token document should be created")
        (is (= "alice" (:username doc)))
        (is (some? (:created_at doc)))
        (is (some? (:expires_at doc)))
        (is (> (:expires_at doc) (:created_at doc)))))))

(deftest save-token-noop-when-disabled
  (reset! test-docs {})
  (with-redefs [fs/enabled? (fn [] false)]
    (remember/save-token! "tok" "alice")
    (is (empty? @test-docs))))

;; =============================================================================
;; token-expired? tests
;; =============================================================================

(deftest token-expired-returns-true-for-past-expiry
  (let [past-time (- (System/currentTimeMillis) 1000)]
    (is (true? (remember/token-expired? {:expires_at past-time})))))

(deftest token-expired-returns-false-for-future-expiry
  (let [future-time (+ (System/currentTimeMillis) (* 90 24 60 60 1000))]
    (is (false? (remember/token-expired? {:expires_at future-time})))))

(deftest token-expired-returns-true-for-nil-expiry
  (is (true? (remember/token-expired? {}))))

;; =============================================================================
;; lookup-token tests
;; =============================================================================

(deftest lookup-token-returns-nil-for-missing-token
  (reset! test-docs {})
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled? (:enabled? mocks)
                  fs/get-doc  (:get-doc mocks)]
      (is (nil? (remember/lookup-token "nonexistent-token"))))))

(deftest lookup-token-returns-nil-for-expired-token
  (reset! test-docs {})
  (let [mocks (make-fs-mocks)
        past-time (- (System/currentTimeMillis) 1000)]
    (with-redefs [fs/enabled?    (:enabled? mocks)
                  fs/get-doc     (:get-doc mocks)
                  fs/delete-doc! (:delete-doc! mocks)]
      (swap! test-docs assoc-in ["remember_tokens" "expired-tok"]
             {:username "alice" :expires_at past-time})
      (is (nil? (remember/lookup-token "expired-tok")))
      ;; Expired token should be cleaned up
      (is (nil? (get-in @test-docs ["remember_tokens" "expired-tok"]))
          "Expired token should be deleted"))))

(deftest lookup-token-returns-user-data-for-valid-token
  (reset! test-docs {})
  (let [mocks (make-fs-mocks)
        future-time (+ (System/currentTimeMillis) 1000000)]
    (with-redefs [fs/enabled? (:enabled? mocks)
                  fs/get-doc  (:get-doc mocks)]
      (swap! test-docs assoc-in ["remember_tokens" "valid-tok"]
             {:username "alice" :expires_at future-time :created_at 1000})
      (swap! test-docs assoc-in ["users" "alice"]
             {:username "alice" :encrypted_session_key "enc-key"})
      (let [result (remember/lookup-token "valid-tok")]
        (is (some? result))
        (is (= "alice" (:username result)))
        (is (= "enc-key" (:encrypted_session_key result)))))))

(deftest lookup-token-returns-nil-when-firestore-disabled
  (with-redefs [fs/enabled? (fn [] false)]
    (is (nil? (remember/lookup-token "any-token")))))

(deftest lookup-token-returns-nil-for-nil-token
  (is (nil? (remember/lookup-token nil))))

;; =============================================================================
;; delete-token! tests
;; =============================================================================

(deftest delete-token-removes-document
  (reset! test-docs {})
  (let [mocks (make-fs-mocks)]
    (with-redefs [fs/enabled?    (:enabled? mocks)
                  fs/delete-doc! (:delete-doc! mocks)]
      (swap! test-docs assoc-in ["remember_tokens" "tok-to-delete"] {:username "alice"})
      (remember/delete-token! "tok-to-delete")
      (is (nil? (get-in @test-docs ["remember_tokens" "tok-to-delete"]))
          "Token should be removed"))))

(deftest delete-token-noop-for-nil
  (reset! test-docs {})
  (with-redefs [fs/enabled? (fn [] true)]
    ;; Should not throw
    (is (nil? (remember/delete-token! nil)))))
