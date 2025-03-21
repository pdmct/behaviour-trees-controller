(ns bt-controller.select-live-test
  (:require
   [bt-controller.config :as cfg]
   [bt-controller.select-live :as select-live :refer [*local-device-url*]]
   [clj-http.client :as client]
   [clojure.test :refer [deftest is testing]]))

(deftest test-get-current-data
  (testing "get-current-data returns parsed JSON data from device URL"
    (let [mock-response {:body "{\"key1\": \"value1\", \"key2\": 42}"}
          expected-result {:key1 "value1" :key2 42}]
      
      ;; Mock both the config and HTTP client functions
      (with-redefs [cfg/get-host (constantly "mock-host")
                    cfg/get-serial (constantly "mock-serial")
                    *local-device-url* "https://mock-host/cgi-bin/solarmonweb/devices/mock-serial/point"
                    client/get (fn [url options]
                                 (is (= (str "https://mock-host/cgi-bin/solarmonweb/devices/mock-serial/point") url))
                                 (is (:insecure? options))
                                 mock-response)]
        
        ;; Test the function with our mocks
        (is (= expected-result (select-live/get-current-data)))))))

(comment
  (clojure.test/run-tests))