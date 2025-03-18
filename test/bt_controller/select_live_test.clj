(ns bt-controller.select-live-test
  (:require [clojure.test :refer :all]
            [bt-controller.select-live :as select-live]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [bt-controller.config :as cfg]))

(deftest test-get-current-data
  (testing "get-current-data returns parsed JSON data from device URL"
    (let [mock-response {:body "{\"key1\": \"value1\", \"key2\": 42}"}
          expected-result {:key1 "value1" :key2 42}]
      
      ;; Mock both the config and HTTP client functions
      (with-redefs [cfg/get-host (constantly "mock-host")
                    cfg/get-serial (constantly "mock-serial")
                    client/get (fn [url options]
                                 (is (= (str "https://mock-host/cgi-bin/solarmonweb/devices/mock-serial/point") url))
                                 (is (:insecure? options))
                                 mock-response)]
        
        ;; Test the function with our mocks
        (is (= expected-result (select-live/get-current-data)))))))

(comment
  (clojure.test/run-tests))(ns bt-controller.select-live-test)