(ns behave-tree.weather
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.pprint :as pp]))

(defn get-weather-forecast [location]
  (let [url (str "https://api.open-meteo.com/v1/forecast?latitude=" (:latitude location) "&longitude=" (:longitude location) "&hourly=temperature_2m")
        response (client/get url {:as :json})]
    (:body response)))

(defn parse-weather-data [weather-data]
  (let [_ (pp/pprint weather-data)
        alerts (-> weather-data :alerts)]
    (map #(select-keys % [:event :start :end :description]) alerts)))
