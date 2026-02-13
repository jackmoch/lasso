(ns lasso.polling.engine
  "Polling engine for tracking target user's scrobbles."
  (:require [lasso.lastfm.client :as client]
            [lasso.lastfm.scrobble :as scrobble]
            [lasso.session.store :as store]
            [lasso.auth.session :as auth-session]
            [taoensso.timbre :as log]))

(defn fetch-recent-tracks
  "Fetch recent tracks for a Last.fm user.
   Returns {:tracks [...]} or {:error ...}."
  [username limit]
  (try
    (let [response (client/api-request {:method "user.getRecentTracks"
                                        :params {:user username
                                                :limit limit}})]
      (if (:error response)
        {:error (:error response)}
        (let [tracks (get-in response [:recenttracks :track])]
          {:tracks (if (map? tracks) [tracks] (vec tracks))})))
    (catch Exception e
      (log/error e "Error fetching recent tracks" {:username username})
      {:error "fetch-failed"})))

(defn track->cache-key
  "Generate a unique cache key for a track.
   Format: 'artist|track|timestamp'"
  [track]
  (str (:artist track) "|" (:track track) "|" (:timestamp track)))

(defn parse-lastfm-track
  "Parse a Last.fm track response into our track format.
   Handles both the API's nested structure and 'now playing' tracks."
  [lastfm-track]
  (let [;; Handle nested artist structure
        artist (if (map? (:artist lastfm-track))
                 (get (:artist lastfm-track) :#text)
                 (:artist lastfm-track))
        ;; Handle nested album structure
        album (when-let [album-data (:album lastfm-track)]
                (if (map? album-data)
                  (get album-data :#text)
                  album-data))
        ;; Track name
        track-name (get lastfm-track :#text (:name lastfm-track))
        ;; Timestamp - skip if currently playing
        timestamp-data (get lastfm-track :date)
        timestamp (when timestamp-data
                   (if (map? timestamp-data)
                     (Integer/parseInt (get timestamp-data :uts))
                     timestamp-data))]

    (when (and artist track-name timestamp)
      {:artist artist
       :track track-name
       :album album
       :timestamp timestamp})))

(defn identify-new-tracks
  "Identify tracks that haven't been scrobbled yet.
   Compares against scrobble-cache to find new tracks.
   Filters by session-start-time to only include tracks after session started
   (with 5-minute lookback buffer to catch recent activity).
   Returns sequence of new tracks."
  [tracks scrobble-cache session-start-time]
  (let [parsed-tracks (keep parse-lastfm-track tracks)
        ;; 5-minute lookback buffer (in seconds)
        lookback-buffer (* 5 60)
        ;; Convert session start from milliseconds to seconds
        cutoff-time (- (quot session-start-time 1000) lookback-buffer)]
    (->> parsed-tracks
         ;; Filter by timestamp - only tracks after cutoff
         (filter (fn [track]
                   (>= (:timestamp track) cutoff-time)))
         ;; Filter by cache - only tracks not already scrobbled
         (filter (fn [track]
                   (not (contains? scrobble-cache (track->cache-key track)))))
         (sort-by :timestamp))))

(defn poll-and-scrobble
  "Poll target user's recent tracks and scrobble new ones.
   Returns {:scrobbled N, :new-cache ...} or {:error ...}."
  [session-id]
  (try
    (let [session (store/get-session session-id)
          following (:following-session session)]

      (if-not (and following (= :active (:state following)))
        {:error "session-not-active"}

        (let [target-username (:target-username following)
              scrobble-cache (:scrobble-cache following)
              session-start-time (:started-at following)
              session-key (:session-key session)
              decrypted-key (auth-session/decrypt-session-key session-key)

              ;; Fetch recent tracks (limit 10)
              fetch-result (fetch-recent-tracks target-username 10)]

          (if (:error fetch-result)
            {:error (:error fetch-result)}

            (let [tracks (:tracks fetch-result)
                  new-tracks (identify-new-tracks tracks scrobble-cache session-start-time)]

              (if (empty? new-tracks)
                {:scrobbled 0
                 :new-cache scrobble-cache}

                (do
                  (log/info "Found" (count new-tracks) "new tracks to scrobble"
                           {:session-id session-id
                            :target-username target-username})

                  ;; Scrobble each new track and track results
                  (let [scrobble-results (for [track new-tracks]
                                          (let [result (scrobble/scrobble-track track decrypted-key)]
                                            {:track track
                                             :success (:success result)
                                             :result result}))
                        successful-tracks (filter :success scrobble-results)
                        failed-tracks (remove :success scrobble-results)]

                    ;; Log failures
                    (doseq [{:keys [track result]} failed-tracks]
                      (log/warn "Failed to scrobble track" {:track track
                                                            :error result}))

                    ;; Only cache successful scrobbles
                    (let [successful-keys (map (comp track->cache-key :track) successful-tracks)
                          updated-cache (into scrobble-cache successful-keys)]
                      {:scrobbled (count successful-tracks)
                       :new-cache updated-cache})))))))))
    (catch Exception e
      (log/error e "Error in poll-and-scrobble" {:session-id session-id})
      {:error "poll-failed"})))

(defn update-session-after-poll
  "Update session with polling results.
   Increments scrobble count and updates cache."
  [session-id scrobbled-count new-cache]
  (store/update-session
   session-id
   (fn [session]
     (-> session
         (update-in [:following-session :scrobble-count] + scrobbled-count)
         (assoc-in [:following-session :scrobble-cache] new-cache)
         (assoc-in [:following-session :last-poll] (System/currentTimeMillis))))))

(defn execute-poll
  "Execute a single polling cycle for a session.
   Returns updated session or nil if error."
  [session-id]
  (log/debug "Executing poll cycle" {:session-id session-id})

  (let [result (poll-and-scrobble session-id)]
    (if (:error result)
      (do
        (log/error "Poll cycle failed" {:session-id session-id
                                       :error (:error result)})
        nil)
      (do
        (when (> (:scrobbled result) 0)
          (log/info "Poll cycle complete" {:session-id session-id
                                          :scrobbled (:scrobbled result)}))
        (update-session-after-poll session-id
                                  (:scrobbled result)
                                  (:new-cache result))))))
