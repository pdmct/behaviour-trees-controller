(ns bt-controller.config
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]))

(def config
  "An atom that holds the loaded configuration and a flag indicating if it has been loaded."
  (atom {:loaded? false}))

(defn load-config
  "Loads the configuration from resources/config.edn using nomad and stores it in an atom."
  []
  (if-let [config-file (io/resource "config.edn")] 
    (reset! config (assoc (read-config config-file) :loaded? true))
    (throw (ex-info "Configuration file config.edn not found in resources directory" {}))))

(defn get-config
  "Returns the current configuration stored in the atom. If the configuration has not been loaded, it loads it first."
  []
  (when-not (:loaded? @config)
    (load-config))
  @config)


(defn get-dashboard
  "Returns the dashboard name from the configuration."
  []
  (get-in (get-config) [:select-live :dashboard]))

(defn get-host
  "Returns the host name from the configuration."
  []
  (get-in (get-config) [:select-live :host]))

(defn get-serial
  "Returns the serial number from the configuration."
  []
  (get-in (get-config) [:select-live :serial]))
