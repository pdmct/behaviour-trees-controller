(ns behave-tree.weather
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defn get-weather-forecast [location]
  (let [url (str "https://api.open-meteo.com/v1/forecast?latitude=" (:latitude location) "&longitude=" (:longitude location) "&hourly=temperature_2m")
        response (client/get url {:as :json})]
    (:body response)))

(defn parse-weather-data [weather-data]
  (let [alerts (-> weather-data :alerts)]
    (map #(select-keys % [:event :start :end :description]) alerts)))
