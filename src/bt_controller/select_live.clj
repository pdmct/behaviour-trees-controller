(ns bt-controller.select-live
  (:require
   [clojure.data.json :as json] 
   [clj-http.client :as client] 
   [diehard.core :as dh]
   [bt-controller.config :as cfg]))


;; (def select-live-url (str "https://select.live/dashboard/hfdata/" (cfg/get-dashboard)))

(def ^:dynamic *local-device-url* (str "https://" (cfg/get-host) "/cgi-bin/solarmonweb/devices/" (cfg/get-serial) "/point"))

(defn get-current-data
  " note; this only works on local network 
   
   Example output: 
   {:device {:name \"Selectronic SP-PRO\"}, 
             :item_count 22, :items 
            {:battery_out_wh_total 15622.5234375, 
   :battery_w 948.486328125, 
   :grid_w 4.74554443359375, 
   :battery_in_wh_total 16475.9765625, 
   :gen_status 0, 
   :solar_wh_total 0.0, 
   :grid_out_wh_today 0.0, 
   :grid_out_wh_total 2312.029248046875, 
   :grid_in_wh_total 20358.61340625, 
   :load_wh_total 22076.576419921876, 
   :fault_ts 0, :fault_code 0, 
   :battery_in_wh_today 27.5625, 
   :battery_out_wh_today 28.669921875, 
   :shunt_w 0.0, 
   :solar_wh_today 0.0, 
   :load_w 839.9613647460938, 
   :timestamp 1742463313, 
   :battery_soc 56.0, 
   :solarinverter_w 0.0, 
   :load_wh_today 18.222890625, 
   :grid_in_wh_today 21.29800341796875}, 
   :now 1742463313}
   "
  []
  (dh/with-retry {:retry-on Exception
                  :max-retries 5
                  :delay-ms 10000
                  :on-retry (fn [val ex] (prn (str "retrying..." val " : " ex)))
                  :on-failed-attempt (fn [val ex] (prn (str "failed attempt..." val " : " ex)))}
    (:items (json/read-str (:body (client/get *local-device-url* {:insecure? true})) :key-fn keyword))))