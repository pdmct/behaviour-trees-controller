(ns behave-tree.weather-test
  (:require [clojure.test :refer [deftest is testing]]
            [behave-tree.weather :refer [get-weather-forecast parse-weather-data]]))

(deftest test-get-weather-forecast
  (testing "get-weather-forecast function"
    (let [location "your_location"
          weather-data (get-weather-forecast location)]
      (is (map? weather-data)))))

(deftest test-parse-weather-data
  (testing "parse-weather-data function"
    (let [weather-data {:alerts [{:event "Storm" :start 1622505600 :end 1622592000 :description "A severe storm is approaching."}
                                 {:event "Flood" :start 1622592000 :end 1622678400 :description "Heavy rainfall expected."}]}
          parsed-data (parse-weather-data weather-data)]
      (is (= parsed-data [{:event "Storm" :start 1622505600 :end 1622592000 :description "A severe storm is approaching."}
                          {:event "Flood" :start 1622592000 :end 1622678400 :description "Heavy rainfall expected."}])))))
