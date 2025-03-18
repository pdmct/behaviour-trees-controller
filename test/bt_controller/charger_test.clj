(ns (ns bt-controller.core-test
.charger-test
  (:require [clojure.test :refer :all]
            [bt-controller.charger :as charger]
            [clj-http.client :as client]))

(deftest test-get-charger-status-mocked
  (let [dummy-response {:body {:status "active"}}
        dummy-ip "127.0.0.1"]
    (with-redefs [client/get (fn [url opts]
                               (is (= (str "http://" dummy-ip "/status") url))
                               dummy-response)]
      (is (= {:status "active"} (charger/get-charger-status dummy-ip))))))
      
;; Run tests automatically when the namespace is loaded (optional)
(comment
  (clojure.test/run-tests))