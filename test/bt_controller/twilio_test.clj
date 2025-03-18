(ns bt-controller.twilio-test
  (:require [clojure.test :refer :all]
            [bt-controller.twilio :as twilio]
            [clj-http.client :as http]
            [bt-controller.config :as config]))

(deftest test-send-text-message
  (let [dummy-response {:body {:status "queued"}}
        dummy-to-number "+1234567890"
        dummy-message "Test message"]
    (with-redefs [http/request (fn [params]
                                (is (= "https://api.twilio.com/2010-04-01/Accounts/your_account_sid/Messages.json" (:url params)))
                                (is (= {:basic-auth ["your_account_sid" "your_auth_token"]
                                        :form-params {:From "your_twilio_number"
                                                      :To dummy-to-number
                                                      :Body dummy-message}}  
                                       (select-keys params [:basic-auth :form-params])))
                                dummy-response)
                  config/get-config (fn []
                                      (assoc {}
                                             :twilio
                                             {:twilio-account-sid "your_account_sid"
                                              :twilio-auth-token "your_auth_token"
                                              :twilio-from-number "your_twilio_number"}))]
      (is (= {:status "queued"} (twilio/send-text-message dummy-to-number dummy-message))))))

(comment
  (clojure.test/run-tests))
