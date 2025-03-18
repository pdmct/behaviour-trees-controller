(ns bt-controller.weather-test
  (:require [clojure.test :refer :all]
            [clojure.string :as st]
            [clj-http.client :as client]
            [bt-controller.weather :refer [get-weather-forecast parse-weather-data]]))

(deftest test-get-weather-forecast
  (testing "get-weather-forecast function"
    (let [dummy-response {:some "weather data"}
          dummy-ip "127.0.0.1"]
      (with-redefs [client/get (fn [url opts]
                                 (is (st/starts-with? url (str "http://" dummy-ip "/v1/forecast")))
                                 dummy-response)])
      (let [location {:longitude 0 :latitude 0}
            weather-data (get-weather-forecast location)]
        (is (map? weather-data))))))

  (deftest test-parse-weather-data
    (testing "parse-weather-data function"
      (let [weather-data {:alerts [{:event "Storm" :start 1622505600 :end 1622592000 :description "A severe storm is approaching."}
                                   {:event "Flood" :start 1622592000 :end 1622678400 :description "Heavy rainfall expected."}]}
            parsed-data (parse-weather-data weather-data)]
        (is (= parsed-data [{:event "Storm" :start 1622505600 :end 1622592000 :description "A severe storm is approaching."}
                            {:event "Flood" :start 1622592000 :end 1622678400 :description "Heavy rainfall expected."}])))))
