(ns bt-controller.charger
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [diehard.core :as dh]))


(defn parse-charger-status
  "Parses the charger status from the OpenEvse 4.x API response."
  [response]
  (let [body (:body response)]
    (if (nil? body)
      {:status "unknown"}
      {:status (:status body)})))

(defn- ip-range
  "Returns a lazy sequence of IP strings from low to high (inclusive),
   where low and high are dotted-decimal strings sharing the same /24 prefix."
  [low high]
  (let [parse-last #(Integer/parseInt (last (str/split % #"\.")))
        prefix     (str/join "." (butlast (str/split low #"\.")))
        start      (parse-last low)
        end        (parse-last high)]
    (map #(str prefix "." %) (range start (inc end)))))

(defn discover-charger
  "Scans the IP range defined by env vars OPENEVSE_CHARGER_IP_LOW and
   OPENEVSE_CHARGER_IP_HIGH and returns the first IP that responds with
   a valid charger status, or nil if none is found."
  []
  (let [low  (System/getenv "OPENEVSE_CHARGER_IP_LOW")
        high (System/getenv "OPENEVSE_CHARGER_IP_HIGH")]
    (when (or (nil? low) (nil? high))
      (throw (ex-info "OPENEVSE_CHARGER_IP_LOW and OPENEVSE_CHARGER_IP_HIGH must be set"
                      {:low low :high high})))
    (log/info (str "Scanning for OpenEVSE charger from " low " to " high))
    (some (fn [ip]
            (try
              (let [url      (str "http://" ip "/status")
                    response (client/get url {:as :json :conn-timeout 1000 :socket-timeout 1000})
                    status   (:status (:body response))]
                (when status
                  (log/info (str "Found charger at " ip " with status: " status))
                  ip))
              (catch Exception _
                nil)))
          (ip-range low high))))

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
