(ns lasso.validation.schemas-test
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.validation.schemas :as schemas]))

(deftest session-state-test
  (testing "Valid session states"
    (is (schemas/valid? schemas/SessionState :not-started))
    (is (schemas/valid? schemas/SessionState :active))
    (is (schemas/valid? schemas/SessionState :paused))
    (is (schemas/valid? schemas/SessionState :stopped)))

  (testing "Invalid session states"
    (is (not (schemas/valid? schemas/SessionState :invalid)))
    (is (not (schemas/valid? schemas/SessionState "active")))
    (is (not (schemas/valid? schemas/SessionState nil)))))

(deftest track-test
  (testing "Valid track"
    (is (schemas/valid? schemas/Track
                       {:artist "The Beatles"
                        :track "Hey Jude"
                        :album "The Beatles 1967-1970"
                        :timestamp 1234567890})))

  (testing "Track without optional album"
    (is (schemas/valid? schemas/Track
                       {:artist "The Beatles"
                        :track "Hey Jude"
                        :timestamp 1234567890})))

  (testing "Track with nil album"
    (is (schemas/valid? schemas/Track
                       {:artist "The Beatles"
                        :track "Hey Jude"
                        :album nil
                        :timestamp 1234567890})))

  (testing "Invalid track - missing required fields"
    (is (not (schemas/valid? schemas/Track
                             {:artist "The Beatles"})))
    (is (not (schemas/valid? schemas/Track
                             {:track "Hey Jude"
                              :timestamp 1234567890}))))

  (testing "Invalid track - wrong types"
    (is (not (schemas/valid? schemas/Track
                             {:artist 123
                              :track "Hey Jude"
                              :timestamp 1234567890})))))

(deftest following-session-test
  (testing "Valid following session - minimal"
    (is (schemas/valid? schemas/FollowingSession
                       {:target-username "testuser"
                        :state :active
                        :started-at 1234567890})))

  (testing "Valid following session - complete"
    (is (schemas/valid? schemas/FollowingSession
                       {:target-username "testuser"
                        :state :active
                        :started-at 1234567890
                        :last-poll 1234567900
                        :scrobble-count 5
                        :scrobble-cache #{"Artist|Track|123456"}})))

  (testing "Valid following session - paused"
    (is (schemas/valid? schemas/FollowingSession
                       {:target-username "testuser"
                        :state :paused
                        :started-at 1234567890
                        :paused-at 1234567920})))

  (testing "Invalid following session - invalid state"
    (is (not (schemas/valid? schemas/FollowingSession
                             {:target-username "testuser"
                              :state :invalid
                              :started-at 1234567890}))))

  (testing "Invalid following session - negative scrobble count"
    (is (not (schemas/valid? schemas/FollowingSession
                             {:target-username "testuser"
                              :state :active
                              :started-at 1234567890
                              :scrobble-count -1})))))

(deftest user-session-test
  (testing "Valid user session without following"
    (is (schemas/valid? schemas/UserSession
                       {:session-id "uuid-123"
                        :username "testuser"
                        :session-key "encrypted-key"
                        :created-at 1234567890
                        :last-activity 1234567900})))

  (testing "Valid user session with following"
    (is (schemas/valid? schemas/UserSession
                       {:session-id "uuid-123"
                        :username "testuser"
                        :session-key "encrypted-key"
                        :created-at 1234567890
                        :last-activity 1234567900
                        :following-session {:target-username "targetuser"
                                           :state :active
                                           :started-at 1234567895}})))

  (testing "Invalid user session - missing required fields"
    (is (not (schemas/valid? schemas/UserSession
                             {:session-id "uuid-123"
                              :username "testuser"})))))

(deftest start-session-request-test
  (testing "Valid start session request"
    (is (schemas/valid? schemas/StartSessionRequest
                       {:target_username "testuser"})))

  (testing "Invalid start session request - missing field"
    (is (not (schemas/valid? schemas/StartSessionRequest {})))))

(deftest session-status-response-test
  (testing "Valid status response - not authenticated"
    (is (schemas/valid? schemas/SessionStatusResponse
                       {:authenticated false
                        :username nil
                        :session nil})))

  (testing "Valid status response - authenticated without session"
    (is (schemas/valid? schemas/SessionStatusResponse
                       {:authenticated true
                        :username "testuser"
                        :session nil})))

  (testing "Valid status response - with active session"
    (is (schemas/valid? schemas/SessionStatusResponse
                       {:authenticated true
                        :username "testuser"
                        :session {:state :active
                                 :target-username "targetuser"
                                 :scrobble-count 10
                                 :recent-scrobbles []
                                 :started-at 1234567890
                                 :last-poll 1234567900}}))))

(deftest validate-helper-test
  (testing "Validate returns success for valid data"
    (let [result (schemas/validate schemas/SessionState :active)]
      (is (:valid? result))
      (is (= :active (:data result)))))

  (testing "Validate returns errors for invalid data"
    (let [result (schemas/validate schemas/SessionState :invalid)]
      (is (not (:valid? result)))
      (is (contains? result :errors)))))

(deftest explain-errors-test
  (testing "Explain errors returns nil for valid data"
    (is (nil? (schemas/explain-errors schemas/SessionState :active))))

  (testing "Explain errors returns error data for invalid data"
    (let [errors (schemas/explain-errors schemas/SessionState :invalid)]
      (is (some? errors))
      ;; Malli humanize can return a map, vector, or string depending on the error type
      (is (or (map? errors) (vector? errors) (string? errors))))))
