(ns bt-controller.select-live
  (:require
   [clojure.data.json :as json] 
   [clj-http.client :as client] 
   [diehard.core :as dh]
   [bt-controller.config :as cfg]))


;; (def select-live-url (str "https://select.live/dashboard/hfdata/" (cfg/get-dashboard)))

(def ^:dynamic *local-device-url* (str "https://" (cfg/get-host) "/cgi-bin/solarmonweb/devices/" (cfg/get-serial) "/point"))

(defn get-current-data
  " note; this only works on local network "
  []
  (dh/with-retry {:retry-on Exception
                  :max-retries 5
                  :delay-ms 10000
                  :on-retry (fn [val ex] (prn (str "retrying..." val " : " ex)))
                  :on-failed-attempt (fn [val ex] (prn (str "failed attempt..." val " : " ex)))}
    (json/read-str (:body (client/get *local-device-url* {:insecure? true})) :key-fn keyword)))