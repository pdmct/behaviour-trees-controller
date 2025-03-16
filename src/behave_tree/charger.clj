(ns behave-tree.charger
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [diehard.core :as dh]))


(defn parse-charger-status
  "Parses the charger status from the OpenEvse 4.x API response."
  [response]
  (let [status (:body response)]
    (if (nil? status)
      {:status "unknown"}
      {:status status})))

(defn get-charger-status
  "Fetches the charger status from the OpenEvse 4.x API."
  [ip-address]
  (try
    (dh/with-retry {:retry-on [Exception]
                    :max-retries 3
                    :delay-ms 1000}
      (let [url (str "http://" ip-address "/status")
            response (client/get url {:as :json})
            _ (println (str "Charger status: " (:body response)))]
        (parse-charger-status response)))
    (catch Exception e
      (log/error e "Error fetching charger status")
      {:status "error"})))
