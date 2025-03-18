(ns bt-controller.twilio-test
  (:require [clojure.test :refer :all]
            [bt-controller.twilio :as twilio]
            [clj-http.client :as client]))

(deftest test-send-text-message
  (let [dummy-response {:body {:status "queued"}}
        dummy-to-number "+1234567890"
        dummy-message "Test message"]
    (with-redefs [client/post (fn [url opts]
                                (is (= "https://api.twilio.com/2010-04-01/Accounts/your_account_sid/Messages.json" url))
                                (is (= {:basic-auth ["your_account_sid" "your_auth_token"]
                                        :form-params {:from "your_twilio_number"
                                                      :to dummy-to-number
                                                      :body dummy-message}
                                        :as :json} opts))
                                dummy-response)]
      (is (= {:status "queued"} (twilio/send-text-message dummy-to-number dummy-message))))))

(comment
  (clojure.test/run-tests))
