(ns lasso.polling.engine-test
  "Tests for polling engine."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.polling.engine :as engine]
            [lasso.lastfm.client :as client]
            [lasso.lastfm.scrobble :as scrobble]
            [lasso.session.store :as store]
            [lasso.auth.session :as auth-session]))

;; Test fixtures
(defn reset-sessions-fixture [f]
  "Reset session store before each test."
  (store/clear-all-sessions!)
  (f))

(use-fixtures :each reset-sessions-fixture)

;; Test for track->cache-key
(deftest track-cache-key-test
  (testing "generates unique cache key"
    (let [track {:artist "The Beatles"
                :track "Hey Jude"
                :timestamp 1234567890}
          key (engine/track->cache-key track)]
      (is (= "The Beatles|Hey Jude|1234567890" key))))

  (testing "different tracks have different keys"
    (let [track1 {:artist "Artist 1" :track "Track 1" :timestamp 100}
          track2 {:artist "Artist 2" :track "Track 2" :timestamp 200}]
      (is (not= (engine/track->cache-key track1)
                (engine/track->cache-key track2))))))

;; Test for parse-lastfm-track
(deftest parse-lastfm-track-test
  (testing "parses track with nested artist structure"
    (let [lastfm-track {:artist {:#text "The Beatles"}
                       :name "Hey Jude"
                       :album {:#text "Past Masters"}
                       :date {:uts "1234567890"}}
          parsed (engine/parse-lastfm-track lastfm-track)]
      (is (= "The Beatles" (:artist parsed)))
      (is (= "Hey Jude" (:track parsed)))
      (is (= "Past Masters" (:album parsed)))
      (is (= 1234567890 (:timestamp parsed)))))

  (testing "skips currently playing tracks (no timestamp)"
    (let [lastfm-track {:artist {:#text "The Beatles"}
                       :name "Hey Jude"}
          parsed (engine/parse-lastfm-track lastfm-track)]
      (is (nil? parsed))))

  (testing "handles missing album"
    (let [lastfm-track {:artist {:#text "The Beatles"}
                       :name "Hey Jude"
                       :date {:uts "1234567890"}}
          parsed (engine/parse-lastfm-track lastfm-track)]
      (is (= "The Beatles" (:artist parsed)))
      (is (= "Hey Jude" (:track parsed)))
      (is (nil? (:album parsed))))))

;; Test for identify-new-tracks
(deftest identify-new-tracks-test
  (testing "identifies new tracks not in cache and after session start"
    (let [session-start (System/currentTimeMillis)
          session-start-sec (quot session-start 1000)
          tracks [{:artist {:#text "Artist 1"}
                  :name "Track 1"
                  :date {:uts (str session-start-sec)}}
                 {:artist {:#text "Artist 2"}
                  :name "Track 2"
                  :date {:uts (str (+ session-start-sec 10))}}]
          cache #{(str "Artist 1|Track 1|" session-start-sec)}
          new-tracks (engine/identify-new-tracks tracks cache session-start)]
      (is (= 1 (count new-tracks)))
      (is (= "Artist 2" (:artist (first new-tracks))))))

  (testing "returns empty when all tracks are cached"
    (let [session-start (System/currentTimeMillis)
          session-start-sec (quot session-start 1000)
          tracks [{:artist {:#text "Artist 1"}
                  :name "Track 1"
                  :date {:uts (str session-start-sec)}}]
          cache #{(str "Artist 1|Track 1|" session-start-sec)}
          new-tracks (engine/identify-new-tracks tracks cache session-start)]
      (is (empty? new-tracks))))

  (testing "filters out tracks before session start (no lookback buffer)"
    (let [session-start (System/currentTimeMillis)
          session-start-sec (quot session-start 1000)
          ;; Track from 10 minutes before session start (should be filtered out)
          old-track {:artist {:#text "Old Artist"}
                    :name "Old Track"
                    :date {:uts (str (- session-start-sec (* 10 60)))}}
          ;; Track from AFTER session start (should be included)
          recent-track {:artist {:#text "Recent Artist"}
                       :name "Recent Track"
                       :date {:uts (str (+ session-start-sec 10))}}
          tracks [old-track recent-track]
          cache #{}
          new-tracks (engine/identify-new-tracks tracks cache session-start)]
      (is (= 1 (count new-tracks)))
      (is (= "Recent Artist" (:artist (first new-tracks))))))

  (testing "sorts tracks by timestamp"
    (let [session-start (System/currentTimeMillis)
          session-start-sec (quot session-start 1000)
          tracks [{:artist {:#text "Artist 1"}
                  :name "Track 1"
                  :date {:uts (str (+ session-start-sec 100))}}
                 {:artist {:#text "Artist 2"}
                  :name "Track 2"
                  :date {:uts (str session-start-sec)}}]
          cache #{}
          new-tracks (engine/identify-new-tracks tracks cache session-start)]
      (is (= 2 (count new-tracks)))
      (is (<= (:timestamp (first new-tracks)) (:timestamp (second new-tracks)))))))

;; Test for update-session-after-poll
(deftest update-session-after-poll-test
  (testing "updates session with scrobble count and cache"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          ;; Initialize following session
          _ (store/update-session
             session-id
             (fn [session]
               (assoc session :following-session {:state :active
                                                  :target-username "targetuser"
                                                  :scrobble-count 0
                                                  :scrobble-cache #{}})))
          new-cache #{"track1" "track2"}
          new-tracks [{:artist "Artist 1" :track "Track 1" :timestamp 100}
                      {:artist "Artist 2" :track "Track 2" :timestamp 200}]
          updated (engine/update-session-after-poll session-id 2 new-cache new-tracks)]
      (is (= 2 (get-in updated [:following-session :scrobble-count])))
      (is (= new-cache (get-in updated [:following-session :scrobble-cache])))
      (is (number? (get-in updated [:following-session :last-poll])))))

  (testing "increments existing scrobble count"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          _ (store/update-session
             session-id
             (fn [session]
               (assoc session :following-session {:state :active
                                                  :target-username "targetuser"
                                                  :scrobble-count 5
                                                  :scrobble-cache #{}})))
          new-tracks [{:artist "New Artist" :track "New Track" :timestamp 300}]
          updated (engine/update-session-after-poll session-id 3 #{"new-track"} new-tracks)]
      (is (= 8 (get-in updated [:following-session :scrobble-count]))))))

;; Test for poll-and-scrobble
(deftest poll-and-scrobble-test
  (testing "returns error when session not active"
    (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
          result (engine/poll-and-scrobble session-id)]
      (is (= "session-not-active" (:error result)))))

  (testing "returns 0 scrobbled when no new tracks"
    (with-redefs [client/api-request (fn [_] {:recenttracks {:track []}})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            session-start (System/currentTimeMillis)
            _ (store/update-session
               session-id
               (fn [session]
                 (assoc session :following-session {:state :active
                                                    :target-username "targetuser"
                                                    :scrobble-count 0
                                                    :scrobble-cache #{}
                                                    :started-at session-start})))
            result (engine/poll-and-scrobble session-id)]
        (is (= 0 (:scrobbled result))))))

  (testing "scrobbles new tracks and updates cache"
    (with-redefs [client/api-request (fn [req]
                                      (if (= "user.getRecentTracks" (:method req))
                                        {:recenttracks {:track [{:artist {:#text "The Beatles"}
                                                                :name "Hey Jude"
                                                                :date {:uts "1234567890"}}]}}
                                        {:scrobbles {(keyword "@attr") {:accepted "1" :ignored "0"}}}))
                 scrobble/scrobble-track (fn [_ _] {:success true :accepted 1 :ignored 0})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            ;; Session started just before track timestamp (1234567890 seconds = ~2009)
            session-start (* 1234567000 1000)
            _ (store/update-session
               session-id
               (fn [session]
                 (assoc session :following-session {:state :active
                                                    :target-username "targetuser"
                                                    :scrobble-count 0
                                                    :scrobble-cache #{}
                                                    :started-at session-start})))
            result (engine/poll-and-scrobble session-id)]
        (is (= 1 (:scrobbled result)))
        (is (= #{"The Beatles|Hey Jude|1234567890"} (:new-cache result))))))

  (testing "does not cache failed scrobbles"
    (with-redefs [client/api-request (fn [req]
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
                                        {:scrobbles {(keyword "@attr") {:accepted "1" :ignored "0"}}}))
                 scrobble/scrobble-track (fn [track _]
                                          ;; Fail Track 1 and Track 3, succeed Track 2
                                          (cond
                                            (= "Track 1" (:track track))
                                            {:success false :error "API error"}
                                            (= "Track 3" (:track track))
                                            {:success false :error "Rate limit"}
                                            :else
                                            {:success true :accepted 1 :ignored 0}))]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            ;; Session started before all tracks (timestamp 0 = 1970)
            session-start 0
            _ (store/update-session
               session-id
               (fn [session]
                 (assoc session :following-session {:state :active
                                                    :target-username "targetuser"
                                                    :scrobble-count 0
                                                    :scrobble-cache #{}
                                                    :started-at session-start})))
            result (engine/poll-and-scrobble session-id)]
        ;; Only 1 successful scrobble (Track 2)
        (is (= 1 (:scrobbled result)))
        ;; Cache only contains successful track
        (is (= #{"Artist 2|Track 2|200"} (:new-cache result)))
        ;; Failed tracks NOT in cache
        (is (not (contains? (:new-cache result) "Artist 1|Track 1|100")))
        (is (not (contains? (:new-cache result) "Artist 3|Track 3|300"))))))

  (testing "failed tracks can be retried on next poll"
    (with-redefs [client/api-request (fn [req]
                                      (if (= "user.getRecentTracks" (:method req))
                                        {:recenttracks {:track [{:artist {:#text "Artist 1"}
                                                                :name "Track 1"
                                                                :date {:uts "100"}}]}}
                                        {:scrobbles {(keyword "@attr") {:accepted "1" :ignored "0"}}}))
                 scrobble/scrobble-track (fn [_ _] {:success false :error "API error"})]
      (let [{:keys [session-id]} (auth-session/create-session "testuser" "session-key")
            ;; Session started before track
            session-start 0
            _ (store/update-session
               session-id
               (fn [session]
                 (assoc session :following-session {:state :active
                                                    :target-username "targetuser"
                                                    :scrobble-count 0
                                                    :scrobble-cache #{}
                                                    :started-at session-start})))
            ;; First poll - scrobble fails
            result1 (engine/poll-and-scrobble session-id)]
        ;; No successful scrobbles
        (is (= 0 (:scrobbled result1)))
        ;; Cache is empty (failed track not cached)
        (is (empty? (:new-cache result1)))

        ;; Second poll with same track - should retry since not in cache
        (with-redefs [scrobble/scrobble-track (fn [_ _] {:success true :accepted 1 :ignored 0})]
          (let [result2 (engine/poll-and-scrobble session-id)]
            ;; Now succeeds
            (is (= 1 (:scrobbled result2)))
            ;; Now cached
            (is (= #{"Artist 1|Track 1|100"} (:new-cache result2)))))))))
