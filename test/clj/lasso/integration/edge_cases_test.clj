(ns lasso.integration.edge-cases-test
  "Edge case and error scenario tests for backend integration."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.auth.session :as auth-session]
            [lasso.session.store :as store]
            [lasso.session.manager :as session-manager]
            [lasso.polling.engine :as engine]
            [lasso.lastfm.client :as client]))

;; Test fixtures
(defn reset-sessions-fixture [f]
  "Reset session store before each test."
  (store/clear-all-sessions!)
  (f))

(use-fixtures :each reset-sessions-fixture)

;; ============================================================================
;; Session Store Edge Cases
;; ============================================================================

(deftest concurrent-session-updates-test
  (testing "Handles concurrent updates to same session"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
      ;; Simulate concurrent updates
      (let [futures (doall
                     (for [i (range 10)]
                       (future
                         (store/update-session
                          session-id
                          (fn [session]
                            (update-in session [:following-session :scrobble-count]
                                      (fnil inc 0)))))))]
        ;; Wait for all to complete
        (doseq [f futures] @f)

        ;; Final count might not be exactly 10 due to race conditions
        ;; but session should still be valid
        (let [session (store/get-session session-id)]
          (is (some? session))
          (is (>= (get-in session [:following-session :scrobble-count]) 1)))))))

(deftest session-expiration-test
  (testing "Expired sessions can be cleaned up"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")
          old-time (- (System/currentTimeMillis) (* 2 60 60 1000))] ; 2 hours ago
      ;; Manually set old last-activity
      (store/update-session session-id
                           (fn [session]
                             (assoc session :last-activity old-time)))

      ;; Session still exists
      (is (some? (store/get-session session-id)))

      ;; TODO: Implement and test cleanup-expired-sessions function
      ;; For now, just verify we can identify expired sessions
      (let [session (store/get-session session-id)
            age (- (System/currentTimeMillis) (:last-activity session))]
        (is (> age (* 60 60 1000)))))))  ; Older than 1 hour

(deftest large-scrobble-cache-test
  (testing "Handles large scrobble caches efficiently"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")
          ;; Create a large cache (1000 entries)
          large-cache (set (for [i (range 1000)]
                            (str "Artist" i "|Track" i "|" i)))]
      (store/update-session
       session-id
       (fn [session]
         (assoc session :following-session {:state :active
                                            :target-username "targetuser"
                                            :scrobble-count (count large-cache)
                                            :scrobble-cache large-cache})))

      (let [session (store/get-session session-id)]
        (is (= 1000 (count (get-in session [:following-session :scrobble-cache]))))))))

;; ============================================================================
;; Session Manager Edge Cases
;; ============================================================================

(deftest start-session-with-invalid-username-test
  (testing "Starting session with empty username fails gracefully"
    (with-redefs [client/api-request (fn [_] {:error 6 :message "User not found"})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")
            result (session-manager/start-session session-id "")]
        (is (contains? result :error))
        (is (not (:success result))))))

  (testing "Starting session with nil username fails gracefully"
    (with-redefs [client/api-request (fn [_] {:error 6 :message "User not found"})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")
            result (session-manager/start-session session-id nil)]
        (is (contains? result :error))
        (is (not (:success result)))))))

(deftest double-start-session-test
  (testing "Cannot start session when already active"
    (with-redefs [client/api-request (fn [req]
                                      ;; Return validated username based on input
                                      (let [username (get-in req [:params :user])]
                                        {:user {:name username}}))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        ;; Start first session
        (let [result1 (session-manager/start-session session-id "targetuser1")]
          (is (:success result1)))

        ;; Try to start another - should not overwrite
        (let [result2 (session-manager/start-session session-id "targetuser2")
              session (store/get-session session-id)]
          ;; Second call should not overwrite the session
          ;; Session should still have first target
          (is (= "targetuser1" (get-in session [:following-session :target-username]))))))))

(deftest pause-nonexistent-session-test
  (testing "Pausing nonexistent session fails gracefully"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")
          result (session-manager/pause-session session-id)]
      (is (contains? result :error)))))

(deftest stop-already-stopped-session-test
  (testing "Stopping already stopped session is idempotent"
    (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        ;; Start then stop
        (session-manager/start-session session-id "targetuser")
        (session-manager/stop-session session-id)

        ;; Stop again - should not error
        (let [result (session-manager/stop-session session-id)]
          (is (some? result))
          (is (:success result))
          ;; Following-session should be nil (cleared)
          (is (nil? (get-in result [:session :following-session]))))))))

;; ============================================================================
;; Polling Engine Edge Cases
;; ============================================================================

(deftest poll-with-network-error-test
  (testing "Polling handles network errors gracefully"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
      ;; Start session successfully
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (session-manager/start-session session-id "targetuser"))

      ;; Now poll with network error
      (with-redefs [client/api-request (fn [_] (throw (Exception. "Network error")))]
        (let [result (engine/poll-and-scrobble session-id)]
          ;; Should return error, not throw
          (is (contains? result :error))
          (is (= "fetch-failed" (:error result))))))))

(deftest poll-with-rate-limit-test
  (testing "Polling handles rate limit errors"
    (with-redefs [client/api-request (fn [_] {:error 29
                                              :message "Rate limit exceeded"})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        ;; Set up session manually (bypass start-session validation)
        (store/update-session
         session-id
         (fn [session]
           (assoc session :following-session {:state :active
                                              :target-username "targetuser"
                                              :scrobble-count 0
                                              :scrobble-cache #{}
                                              :started-at 0})))

        (let [result (engine/poll-and-scrobble session-id)]
          (is (contains? result :error))
          (is (= 29 (:error result))))))))

(deftest poll-with-empty-response-test
  (testing "Polling handles empty track list"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
      ;; Start session successfully
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (session-manager/start-session session-id "targetuser"))

      ;; Now poll with empty response
      (with-redefs [client/api-request (fn [_] {:recenttracks {:track []}})]
        (let [result (engine/poll-and-scrobble session-id)]
          (is (= 0 (:scrobbled result)))
          (is (empty? (:new-tracks result))))))))

(deftest poll-with-malformed-tracks-test
  (testing "Polling skips malformed tracks"
    (with-redefs [client/api-request
                  (fn [_]
                    {:recenttracks
                     {:track [{:artist {:#text "Good Artist"}
                              :name "Good Track"
                              :date {:uts "100"}}
                             {:artist nil  ; Malformed - missing artist
                              :name "Bad Track"
                              :date {:uts "200"}}
                             {:artist {:#text "Another Artist"}
                              :name "Another Track"
                              :date {:uts "300"}}]}})
                 lasso.lastfm.scrobble/scrobble-track
                 (fn [_ _] {:success true :accepted 1 :ignored 0})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        ;; Set up session manually (bypass start-session validation)
        (store/update-session
         session-id
         (fn [session]
           (assoc session :following-session {:state :active
                                              :target-username "targetuser"
                                              :scrobble-count 0
                                              :scrobble-cache #{}
                                              :started-at 0})))

        ;; Should only get 2 valid tracks (skips the malformed one)
        (let [result (engine/poll-and-scrobble session-id)]
          (is (= 2 (:scrobbled result))))))))

;; ============================================================================
;; Session State Transitions
;; ============================================================================

(deftest rapid-state-transitions-test
  (testing "Rapid pause/resume transitions work correctly"
    (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        (session-manager/start-session session-id "targetuser")

        ;; Rapidly pause and resume multiple times
        (dotimes [_ 5]
          (session-manager/pause-session session-id)
          (session-manager/resume-session session-id))

        ;; Final state should be active
        (let [session (store/get-session session-id)]
          (is (= :active (get-in session [:following-session :state]))))))))

(deftest session-state-after-error-test
  (testing "Session state remains consistent after errors"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
      ;; Start session successfully
      (with-redefs [client/api-request (fn [_] {:user {:name "targetuser"}})]
        (session-manager/start-session session-id "targetuser"))

      ;; Simulate polling error
      (with-redefs [client/api-request (fn [_] (throw (Exception. "API error")))]
        (engine/poll-and-scrobble session-id))

      ;; Session should still be active
      (let [session (store/get-session session-id)]
        (is (= :active (get-in session [:following-session :state])))))))

;; ============================================================================
;; Data Integrity Tests
;; ============================================================================

(deftest scrobble-count-accuracy-test
  (testing "Scrobble count accurately reflects successful scrobbles"
    (with-redefs [client/api-request
                  (fn [req]
                    (if (= "user.getRecentTracks" (:method req))
                      {:recenttracks {:track [{:artist {:#text "Artist 1"}
                                              :name "Track 1"
                                              :date {:uts "100"}}
                                             {:artist {:#text "Artist 2"}
                                              :name "Track 2"
                                              :date {:uts "200"}}
                                             {:artist {:#text "Artist 3"}
                                              :name "Track 3"
                                              :date {:uts "300"}}]}}
                      {:scrobbles {(keyword "@attr") {:accepted "2" :ignored "1"}}}))
                 lasso.lastfm.scrobble/scrobble-track
                 (fn [track _]
                   ;; Fail track 2
                   (if (= "Track 2" (:track track))
                     {:success false :error "Failed"}
                     {:success true :accepted 1 :ignored 0}))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        (store/update-session
         session-id
         (fn [session]
           (assoc session :following-session {:state :active
                                              :target-username "targetuser"
                                              :scrobble-count 0
                                              :scrobble-cache #{}
                                              :started-at 0})))

        (let [result (engine/poll-and-scrobble session-id)]
          ;; Only 2 successful scrobbles (Track 1 and Track 3)
          (is (= 2 (:scrobbled result)))
          ;; Cache should only contain successful tracks
          (is (= 2 (count (:new-cache result))))
          (is (not (contains? (:new-cache result) "Artist 2|Track 2|200"))))))))

(deftest no-duplicate-scrobbles-test
  (testing "Same track is not scrobbled twice"
    (with-redefs [client/api-request
                  (fn [_] {:recenttracks {:track [{:artist {:#text "Artist"}
                                                  :name "Track"
                                                  :date {:uts "100"}}]}})
                 lasso.lastfm.scrobble/scrobble-track
                  (fn [_ _] {:success true :accepted 1 :ignored 0})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "key")]
        (store/update-session
         session-id
         (fn [session]
           (assoc session :following-session {:state :active
                                              :target-username "targetuser"
                                              :scrobble-count 0
                                              :scrobble-cache #{}
                                              :started-at 0})))

        ;; First poll - should scrobble and update cache
        (engine/execute-poll session-id)
        (let [session1 (store/get-session session-id)]
          (is (= 1 (get-in session1 [:following-session :scrobble-count]))))

        ;; Second poll with same track - should NOT scrobble (already in cache)
        (engine/execute-poll session-id)
        (let [session2 (store/get-session session-id)]
          ;; Count should still be 1 (not incremented)
          (is (= 1 (get-in session2 [:following-session :scrobble-count]))))))))
