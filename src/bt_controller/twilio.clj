(ns bt-controller.twilio
  (:require [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [behave-tree.config :as cfg] 
            [clojure.string :as str]
            [behave-tree.twilio :as twilio])
  (:import [java.net URLEncoder]))

(def base "https://api.twilio.com/2010-04-01")

;; taken from:
;; https://github.com/uplift-inc/twilio/blob/master/src/twilio/core.clj

;; Authentication info

(def ^:dynamic *sid*   "")
(def ^:dynamic *token* "")

;; Helper macro

(defmacro with-auth
  [account_sid auth_token & body]
  `(binding [*sid* ~account_sid
             *token* ~auth_token]
     (do ~@body)))

(defn encode-url [url]
  (URLEncoder/encode url))

(defn make-request-url [endpoint]
  (format "%s/Accounts/%s/%s.json"
          base
          *sid*
          endpoint))

;; HTTP requests

(defn request
  "Make a simple POST request to the API"
  [method url & params]
  (assert ((complement every?) str/blank? [*sid* *token*]))
  (let [request-params (into {} params)]

    (try
      (http/request
       {:method method
        :url url
        :form-params request-params
        :basic-auth [*sid* *token*]
        :socket-timeout 3000
        :conn-timeout 3000}) ; Timeout the request in 3 seconds
      (catch Exception e
        {:error e}))))


(deftype SMS [from to body])

;; Utils

(def twilio-format-key
  (comp keyword str/capitalize name))

(defn as-twilio-map 
  "Twilio defines ugly uppercase keys for the api i.e :from becomes :From
   so this helper transforms clojure keys without making my eyes bleed"
  [m]
  (reduce
   (fn [acc [k v]]
     (conj acc {(twilio-format-key k) v})) {} m))

(defn sms
  "Create an SMS message"
  [from to body]
  (as-twilio-map
   {:body body
    :to to
    :from from}))

;; Send an SMS message via Twilio
;; *************************************************

(defn send-sms
  "Send an SMS message which is a map in the form {:from x :to x body x}"
  [params]
  (let [url (make-request-url "Messages")]
    (log/info (str "Message sent: " (:body params))
              (request :post url params))))

(defn send-text-message
  "Sends a text message using the Twilio API."
  [to-number message]
  (let [twilio-cfg (get-in (cfg/get-config) [:twilio])
        from-number (:twilio-from-number twilio-cfg)
        params (sms from-number to-number message)]
    (with-auth
      (:twilio-account-sid twilio-cfg)
      (:twilio-auth-token twilio-cfg)
      (send-sms params))))