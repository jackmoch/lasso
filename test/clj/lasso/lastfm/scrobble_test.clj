(ns lasso.lastfm.scrobble-test
  (:require [clojure.test :refer [deftest is testing]]
            [lasso.lastfm.scrobble :as scrobble]
            [lasso.lastfm.client :as client]))

(deftest format-scrobble-params-test
  (testing "Format track with all fields"
    (let [track {:artist "The Beatles"
                 :track "Hey Jude"
                 :album "The Beatles 1967-1970"
                 :timestamp 1234567890}
          params (scrobble/format-scrobble-params track "session-key")]
      (is (= "The Beatles" (:artist params)))
      (is (= "Hey Jude" (:track params)))
      (is (= "The Beatles 1967-1970" (:album params)))
      (is (= 1234567890 (:timestamp params)))
      (is (= "session-key" (:sk params)))))

  (testing "Format track without album"
    (let [track {:artist "Artist"
                 :track "Song"
                 :timestamp 1234567890}
          params (scrobble/format-scrobble-params track "session-key")]
      (is (= "Artist" (:artist params)))
      (is (= "Song" (:track params)))
      (is (nil? (:album params)))
      (is (= 1234567890 (:timestamp params))))))

(deftest validate-scrobble-response-test
  (testing "Validate successful scrobble"
    (let [response {:scrobbles {(keyword "@attr") {:accepted "1" :ignored "0"}
                               :scrobble {:track "Hey Jude"}}}
          result (scrobble/validate-scrobble-response response)]
      (is (true? (:success result)))
      (is (= 1 (:accepted result)))
      (is (= 0 (:ignored result)))))

  (testing "Validate error response"
    (let [response {:error 9 :message "Invalid session key"}
          result (scrobble/validate-scrobble-response response)]
      (is (false? (:success result)))
      (is (= 9 (:error result)))
      (is (= "Invalid session key" (:message result)))))

  (testing "Validate response with ignored scrobbles"
    (let [response {:scrobbles {(keyword "@attr") {:accepted "5" :ignored "2"}}}
          result (scrobble/validate-scrobble-response response)]
      (is (true? (:success result)))
      (is (= 5 (:accepted result)))
      (is (= 2 (:ignored result))))))

(deftest scrobble-track-test
  (testing "Scrobble single track"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:scrobbles {(keyword "@attr") {:accepted "1" :ignored "0"}}})]
        (let [track {:artist "Artist" :track "Song" :timestamp 1234567890}
              result (scrobble/scrobble-track track "session-key")]
          (is (= 1 (count @requests)))
          (let [req (first @requests)]
            (is (= "track.scrobble" (:method req)))
            (is (= "Artist" (get-in req [:params :artist])))
            (is (= "Song" (get-in req [:params :track])))
            (is (= 1234567890 (get-in req [:params :timestamp])))
            (is (= "session-key" (get-in req [:params :sk])))
            (is (true? (:signed req))))
          (is (true? (:success result)))))))

  (testing "Scrobble with error"
    (with-redefs [client/api-request
                  (fn [req]
                    {:error 9 :message "Invalid session key"})]
      (let [track {:artist "Artist" :track "Song" :timestamp 1234567890}
            result (scrobble/scrobble-track track "bad-key")]
        (is (false? (:success result)))
        (is (= 9 (:error result)))))))

(deftest scrobble-batch-test
  (testing "Batch scrobble multiple tracks"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {:scrobbles {(keyword "@attr") {:accepted "3" :ignored "0"}}})]
        (let [tracks [{:artist "Artist1" :track "Song1" :timestamp 1234567890 :album "Album1"}
                     {:artist "Artist2" :track "Song2" :timestamp 1234567891 :album "Album2"}
                     {:artist "Artist3" :track "Song3" :timestamp 1234567892 :album "Album3"}]
              result (scrobble/scrobble-batch tracks "session-key")]
          (is (= 1 (count @requests)))
          (let [req (first @requests)
                params (:params req)]
            (is (= "track.scrobble" (:method req)))
            ;; Check indexed parameters
            (is (= "Artist1" (get params (keyword "artist[0]"))))
            (is (= "Song1" (get params (keyword "track[0]"))))
            (is (= 1234567890 (get params (keyword "timestamp[0]"))))
            (is (= "Artist2" (get params (keyword "artist[1]"))))
            (is (= "Artist3" (get params (keyword "artist[2]"))))
            (is (= "session-key" (:sk params)))
            (is (true? (:signed req))))
          (is (true? (:success result)))
          (is (= 3 (:accepted result)))))))

  (testing "Batch scrobble empty list"
    (let [requests (atom [])]
      (with-redefs [client/api-request
                    (fn [req]
                      (swap! requests conj req)
                      {})]
        (let [result (scrobble/scrobble-batch [] "session-key")]
          (is (nil? result))
          (is (= 0 (count @requests))))))))
