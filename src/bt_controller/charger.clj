(ns bt-controller.charger
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [diehard.core :as dh]))


(defn parse-charger-status
  "Parses the charger status from the OpenEvse 4.x API response."
  [response]
  (let [body (:body response)]
    (if (nil? body)
      {:status "unknown"}
      {:status (:status body)})))

(defn get-charger-status
  "Fetches the charger status from the OpenEvse 4.x API."
  [ip-address]
  (try
    (dh/with-retry {:retry-on [Exception]
                    :max-retries 3
                    :delay-ms 1000}
      (let [url (str "http://" ip-address "/status")
            response (client/get url {:as :json})
            _ (log/info (str "Charger status: " (:body response)))]
        (parse-charger-status response)))
    (catch Exception e
      (log/error e "Error fetching charger status")
      {:status "error"})))
