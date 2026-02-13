(ns logging
  "Development logging configuration for cleaner output."
  (:require [taoensso.timbre :as timbre]))

(defn configure-dev-logging!
  "Configure timbre for development with clean output."
  []
  (timbre/merge-config!
   {:min-level :info
    :appenders
    {:println
     {:enabled? true
      :output-fn (fn [data]
                   (let [{:keys [?ns-str level ?msg-fmt vargs]} data
                         msg (if ?msg-fmt
                               (try
                                 (apply format ?msg-fmt vargs)
                                 (catch Exception _
                                   (pr-str vargs)))
                               (pr-str vargs))]
                     (str (name level) " " (or ?ns-str "") " " msg)))}}}))

(defn banner
  "Print a banner with title."
  [title]
  (let [width 60
        padding (/ (- width (count title) 2) 2)
        line (apply str (repeat width "─"))]
    (println)
    (println (str "┌" line "┐"))
    (println (str "│" (apply str (repeat padding " ")) title (apply str (repeat padding " ")) "│"))
    (println (str "└" line "┘"))
    (println)))

(defn section
  "Print a section header."
  [title]
  (println)
  (println (str "▸ " title))
  (println))
