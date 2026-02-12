(ns lasso.db
  "Application state schema and default database.")

(def default-db
  "Default app state structure."
  {:auth {:authenticated? false
          :username nil
          :checking? false}
   :session {:state :not-started  ; :not-started, :active, :paused
             :target-username nil
             :scrobble-count 0
             :recent-scrobbles []
             :started-at nil
             :last-poll nil}
   :ui {:loading? false
        :error nil
        :session-control-loading? false
        :polling? false}})
