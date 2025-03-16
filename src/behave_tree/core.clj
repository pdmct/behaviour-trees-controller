(ns behave-tree.core
  (:require
   [aido.compile :as ac]
   [aido.core :as ai]
   [aido.tick :as at]
   [java-time.api :as time]))


(defn in-interval?
  "Returns true if the given LocalTime is within the specified interval [start-hour, end-hour]."
  [time start-hour end-hour]
  (and (>= (.getHour time) start-hour)
       (< (.getHour time) end-hour)))

(defn in-free-charge-time? [time]
  (in-interval? time 11 13))

(defn in-cheap-charge-time? [time]
  (in-interval? time 0 6))

(defn minutes-until-target [current-time target-time-of-day]
  (let [target-time (time/local-time target-time-of-day)
        current-local-time (time/local-time current-time) ; Extract LocalTime
        ;; Check if current time is after the target time
        target-time (if (.isAfter current-local-time target-time)
                      (-> target-time
                          (.plusHours 24)) ; Add 24 hours to target time
                      target-time)
        difference (.toMillis (java.time.Duration/between current-local-time target-time))]
    (if (< difference 0)
      (+ 1440 (/ difference 60000))
      (/ difference 60000))))

(defn forecast-soc?
  [current-soc target-soc  current-time target-time-of-day charge-rate-mins]
  (let [time-left-mins (minutes-until-target current-time target-time-of-day)
        ;; _ (println (str "time-left-mins: " (* time-left-mins 1.0)))
        ;; _ (println (str "charge-rate-mins: " (* charge-rate-mins 1.0)))
        ;; _ (println (str "current-soc: " (* current-soc 1.0)))
        forecast-soc (+ current-soc (* time-left-mins charge-rate-mins))]
    (>= forecast-soc target-soc)))

;; aido behaviors for the battery
(defmethod at/tick :battery-charged?
  [db]
  (if (>= (:soc (:state db)) 100)
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :battery-soc-80?
  [db]
  (if (>= (:soc (:state db)) 80)
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :battery-soc-20?
  [db]
  (if (<= (:soc (:state db)) 20)
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :free-charge-time?
  [db]
  (if (in-free-charge-time? (time/local-time))
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :cheap-charge-time?
  [db]
  (if (in-cheap-charge-time? (time/local-time))
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :car-charging?
  [db]
  (if (:car-charging? (:state db))
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :forecast-soc-45-at-6am?
  [db]
  (if (forecast-soc? (:soc (:state db))
                     45
                     (time/local-time 6 0)
                     (/ 5 60))
    (ai/tick-success db)
    (ai/tick-failure db)))

(defmethod at/tick :charge-battery
  [db]
  (ai/tick-success (assoc db :state {:soc 100})))

(defmethod at/tick :wait-1-minute
  [db]
  (do (Thread/sleep 60000)
      (ai/tick-success db)))

(def battery-behaviour-tree
  [:loop {:count 3}
   [:selector
    [:battery-charged?]
    [:sequence
     [:free-charge-time?]
     [:selector
      [:car-charging?
       :charge-battery]]]
    [:sequence
     [:cheap-charge-time?]
     [:selector
      [:car-charging?]
      [:sequence
       [:forecast-soc-45-at-6am?]
       [:charge-battery]]]]]
   [:wait-1-minute]])

(defn main
  ;; Running the behavior tree
  []
  (let [fns {}
        tree (ac/compile battery-behaviour-tree fns)
        db {:state {:soc 80
                    :car-charging? false}}]
    (let [{:keys [db status]} (ai/run-tick db tree)]
      (print (str "db:" db))
      (if (and (= ai/SUCCESS status)
               (get-in db [:state :response-sent]))
        (print (str "Email generated and sent!"))
        (print "Help!")))))
