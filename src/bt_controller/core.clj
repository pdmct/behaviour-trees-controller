(ns bt-controller.core
  (:require
   [aido.compile :as ac]
   [aido.core :as ai]
   [aido.tick :as at]
   [bt-controller.charger :as charger]
   [bt-controller.config :as cfg]
   [bt-controller.select-live :as battery]
   [bt-controller.twilio :as twilio]
   [bt-controller.weather :as weather]
   [bt-controller.tree-utils :as tu]
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]
   [java-time.api :as time]
   [rhizome.viz :as viz]
   [rhizome.dot :as dot]))

(defn in-interval?
  "Returns true if the given LocalTime is within the specified interval [start-hour, end-hour]."
  [time start-hour end-hour]
  (and (>= (.getHour time) start-hour)
       (< (.getHour time) end-hour)))

(defn in-free-charge-time? [t]
  (in-interval? t 11 13))

(defn in-cheap-charge-time? [t]
  (in-interval? t 0 6))

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
        ;; _ (log/info (str "time-left-mins: " (* time-left-mins 1.0)))
        ;; _ (log/info (str "charge-rate-mins: " (* charge-rate-mins 1.0)))
        ;; _ (log/info (str "current-soc: " (* current-soc 1.0)))
        forecast-soc (+ current-soc (* time-left-mins charge-rate-mins))]
    (>= forecast-soc target-soc)))

(defn get-major-weather-events [location]
  (let [weather-data (weather/get-weather-forecast location)]
    (weather/parse-weather-data weather-data)))

(defn get-charger-ip []
  (or (get-in (cfg/get-config) [:charger :ip])
      "127.0.0.1"))

(defn fetch-charger-status []
  (try
    (charger/get-charger-status (get-charger-ip))
    (catch Exception e
      (log/error "Error fetching charger status:" (.getMessage e))
      {:status "unknown"})))

;; aido behaviors for the battery
#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :battery-charged?
  [db & children]
  (log/info "Checking battery charged")
  (if (>= (:soc (:state db)) 100)
    (ai/tick-success db)
    (ai/tick-failure db)))


#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :battery-soc-80?
  [db & children]
  (log/info "Checking battery soc 80")
  (if (>= (:soc (:state db)) 80)
    (ai/tick-success db)
    (ai/tick-failure db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :battery-soc-20?
  [db & children]
  (log/info "Checking battery soc 20")
  (if (<= (:soc (:state db)) 20)
    (ai/tick-success db)
    (ai/tick-failure db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :free-charge-time?
  [db & children]
  (log/info "Checking free charge time")
  (if (in-free-charge-time? (time/local-time))
    (ai/tick-success db)
    (ai/tick-failure db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :cheap-charge-time?
  [db & children]
  (log/info "Checking cheap charge time")
  (if (in-cheap-charge-time? (time/local-time))
    (ai/tick-success db)
    (ai/tick-failure db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :car-charging?
  [db & children]
  (log/info "Checking car charging")
  (let [charger-active? (fetch-charger-status)
        db (assoc-in db [:state :car-charging?] (:status charger-active?))]
    (if charger-active?
      (ai/tick-success db)
      (ai/tick-failure db))))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :forecast-soc-45-at-6am?
  [db & children]
  (log/info "Checking forecast soc 45 at 6am")
  (let [current-time (time/local-time)]
    (if (forecast-soc? (:soc (:state db))
                       45
                       current-time
                       (time/local-time 6 0)
                       (/ 5 60))
      (ai/tick-success db)
      (ai/tick-failure db))))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :major-weather-event?
  [db & children]
  (let [location "your_location"
        major-events (get-major-weather-events location)]
    (if (seq major-events)
      (ai/tick-success db)
      (ai/tick-failure db))))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :charge-battery
  [db & children]
  (log/info "Charging battery")
  (ai/tick-success (assoc db :state {:soc 100})))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :wait-1-minute
  [db & children]
  (log/info "Waiting 1 minute")
  (do (Thread/sleep 60000)
      (ai/tick-success db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :update-soc
  [db & children]
  (let [current-data (battery/get-current-data)]
    (log/info (str "Updating SOC with " current-data))
    (ai/tick-success (assoc-in db 
                               [:state :soc] 
                               (:soc current-data)))))
  

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :charger-offline?
  [db & children]
  (log/info "Checking charger offline")
  (let [charger-status (fetch-charger-status)]
    (if (= (:status charger-status) "error")
      (ai/tick-success db)
      (ai/tick-failure db))))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :alert-sent?
  [db & children]
  (log/info "Checking alert sent")
  (if (:alert-sent? (:state db))
    (ai/tick-success db)
    (ai/tick-failure db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :send-txt-alert!
  [db & children]
  (log/info "Sending txt alert")
  (let [twilio-cfg (get-in (cfg/get-config) [:twilio])] 
    (log/info "Sending txt alert to:" (:twilio-alert-number twilio-cfg))
    (twilio/send-text-message (:twilio-alert-number twilio-cfg)
                              (:twilio-alert-message twilio-cfg))
    (let [db (update-in db [:state :alert-sent?] not)] 
      (ai/tick-success db))))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :send-txt-restored!
  [db & children]
  (log/info "Power is restored, sending txt restored mesg")
  (let [twilio-config (get-in (cfg/get-config) [:twilio])]
    (twilio/send-text-message (:twilio-alert-number twilio-config)
                              (:twilio-alert-message-restored twilio-config)))
  (let [db (update-in db [:state :alert-sent?] (constantly false))]
    (ai/tick-success db)))

#_{:clj-kondo/ignore [:unused-binding]}
(defmethod at/tick :charger-online?
  [db & children]
  (log/info "Checking charger online")
  (let [charger-status (fetch-charger-status)]
    (if (not= (:status charger-status) "error")
      (ai/tick-success db)
      (ai/tick-failure db))))

(def battery-behaviour-tree
  [:loop {:count 2} ;; for testing, -1 for infinite
   [:sequence
    [:selector
     [:selector
      [:battery-charged?]
      [:sequence
       [:free-charge-time?]
       [:selector
        [:car-charging?]
        [:charge-battery]]]
      [:sequence
       [:cheap-charge-time?]
       [:selector
        [:car-charging?]
        [:sequence
         [:forecast-soc-45-at-6am?]
         [:charge-battery]]]]
      [:sequence
       [:major-weather-event?]
       [:update-soc]]
      [:sequence
       [:charger-offline?]
       [:selector
        [:alert-sent?]
        [:send-txt-alert!]]]
      [:sequence
       [:charger-online?]
       [:sequence
        [:alert-sent?]
        [:send-txt-restored!]]]
      [:update-soc]]
    [:wait-1-minute]]]])


(defn generate-diagram [tree]
  (let [structure (tu/generate-tree-structure tree)
        nodes (:nodes structure)]
    (let [dot (dot/graph->dot nodes (tu/adjacent-nodes structure)
                              :node->descriptor tu/node->descriptor
                              :options {:rankdir "TB"   ; Top to bottom layout
                                        :ordering "out" ; Preserve child order
                                        :ranksep "0.8"
                                        :nodesep "0.5"})
          image (viz/dot->image dot)]
      (viz/save-image image "resources/behavior_tree.png"))))

(defn -main
  ;; Running the behavior tree
  [& args]
  (log/info "Starting...")
  (log/info "Loaded config:" (cfg/get-config))
  (log/info "Current time:" (time/local-time))
  (log/info "Major weather events:" (get-major-weather-events (:location @cfg/config)))
  (log/info "Forecast SOC 45 at 6am:" (forecast-soc? 80 45 (time/local-time) (time/local-time 6 0) (/ 5 60)))
  ;; (log/info "Charger status:" (fetch-charger-status))
  (if (some #{"--generate-diagram"} args)
    (generate-diagram battery-behaviour-tree)
    (let [fns {}
          _ (log/info "Tree compiling...")
          tree (ac/compile battery-behaviour-tree fns)
          _ (log/info  (str "Tree compiled successfully:" (count tree)))
          _ (pp/pprint tree)
          db {:state {:soc 80
                      :car-charging? false
                      :alert-sent? false}}
          _ (log/info "Running tree...")
          {:keys [db status]} (ai/run-tick db tree)]
      (log/info (str "db:" db))
      (if (= ai/SUCCESS status)
        (log/info "Tree finished successfully")
        (log/info "Tree finished with failure")))))
