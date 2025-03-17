(ns behave-tree.twilio
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [behave-tree.config :as cfg]))

(defn send-text-message
  "Sends a text message using the Twilio API."
  [to-number message]
  (let [{:keys [twilio-account-sid twilio-auth-token twilio-from-number]} (:twilio (cfg/get-config))
        url (str "https://api.twilio.com/2010-04-01/Accounts/" twilio-account-sid "/Messages.json")
        auth [twilio-account-sid twilio-auth-token]
        params {:from twilio-from-number
                :to to-number
                :body message}]
    (try
      (let [response (client/post url {:basic-auth auth
                                       :form-params params
                                       :as :json})]
        (log/info (str "Message sent: " (:body response)))
        (:body response))
      (catch Exception e
        (log/error e "Error sending text message")
        {:status "error"}))))
