(ns lasso.lastfm.scrobble
  "Last.fm scrobble submission operations."
  (:require [lasso.lastfm.client :as client]
            [taoensso.timbre :as log]))

(defn format-scrobble-params
  "Format track data for scrobble submission.
   Track should have :artist, :track, :timestamp, and optional :album."
  [track session-key]
  (cond-> {:artist (:artist track)
           :track (:track track)
           :timestamp (:timestamp track)
           :sk session-key}
    (:album track) (assoc :album (:album track))))

(defn validate-scrobble-response
  "Validate Last.fm scrobble response.
   Returns {:success true} or {:success false :error ...}."
  [response]
  (if (contains? response :error)
    {:success false
     :error (:error response)
     :message (:message response)}
    (if (get-in response [:scrobbles (keyword "@attr") :accepted])
      {:success true
       :accepted (Integer/parseInt (get-in response [:scrobbles (keyword "@attr") :accepted]))
       :ignored (Integer/parseInt (get-in response [:scrobbles (keyword "@attr") :ignored]))}
      {:success false
       :error "no-accepted-field"
       :message "Response missing accepted field"})))

(defn scrobble-track
  "Submit a scrobble to Last.fm for the authenticated user.
   Track should have :artist, :track, :timestamp, and optional :album.
   Session-key is the authenticated user's Last.fm session key.
   Returns {:success true/false ...}."
  [track session-key]
  (log/info "Scrobbling track:" (:artist track) "-" (:track track))
  (let [params (format-scrobble-params track session-key)
        response (client/api-request {:method "track.scrobble"
                                     :params params
                                     :signed true})]
    (validate-scrobble-response response)))

(defn scrobble-batch
  "Submit multiple scrobbles at once (up to 50 tracks).
   Tracks is a sequence of track maps.
   Returns validation result."
  [tracks session-key]
  (when (seq tracks)
    (log/info "Batch scrobbling" (count tracks) "tracks")
    (let [indexed-params (apply merge
                               (map-indexed
                                (fn [idx track]
                                  {(keyword (str "artist[" idx "]")) (:artist track)
                                   (keyword (str "track[" idx "]")) (:track track)
                                   (keyword (str "timestamp[" idx "]")) (:timestamp track)
                                   (keyword (str "album[" idx "]")) (:album track)})
                                tracks))
          params (assoc indexed-params :sk session-key)
          response (client/api-request {:method "track.scrobble"
                                       :params params
                                       :signed true})]
      (validate-scrobble-response response))))
