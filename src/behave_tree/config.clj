(ns behave-tree.config
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]))

(defn load-config
  "Loads the configuration from resources/config.edn using nomad."
  []
  (if-let [config-file (io/resource "config.edn")]
    (-> config-file
        read-config)
    (throw (ex-info "Configuration file config.edn not found in resources directory" {}))))

(def config
  "A delay that holds the loaded configuration."
  (delay (load-config)))