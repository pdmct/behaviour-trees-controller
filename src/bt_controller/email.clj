(ns bt-controller.email
  (:require [clojure.tools.logging :as log]
            [diehard.core :as dh]
            [clj-http.client :as client]
            [cheshire.core :as json]))

(defn send-email
  "Sends an email message using a configured email service.
   
   Parameters:
   - config: Map containing email configuration:
     {:service-url \"https://your-email-service.com/send\"
      :api-key \"your-api-key\"
      :from \"sender@example.com\"}
   - to: Recipient email address
   - subject: Email subject
   - body: Email body content
   
   Returns:
   - Map with :success boolean and :message string"
  [{:keys [service-url api-key from] :as config} to subject body]
  (try
    (dh/with-retry {:retry-on [Exception]
                    :max-retries 3
                    :delay-ms 1000}
      (let [payload {:from from
                     :to to
                     :subject subject
                     :body body}
            response (client/post service-url
                                  {:body (json/generate-string payload)
                                   :content-type :json
                                   :headers {"Authorization" (str "Bearer " api-key)}
                                   :throw-exceptions true})]
        (log/info "Email sent successfully to" to)
        {:success true
         :message "Email sent successfully"}))
    (catch Exception e
      (log/error e "Error sending email to" to)
      {:success false
       :message (str "Failed to send email: " (.getMessage e))})))

(defn send-notification-email
  "Convenience function to send notification emails with standard formatting.
   
   Parameters:
   - config: Email configuration map (see send-email function)
   - to: Recipient email address
   - subject: Email subject
   - message: Notification message content
   
   Returns:
   - Result from send-email function"
  [config to subject message]
  (let [formatted-body (str message "\n\nSent from BT Controller at "
                            (java.time.LocalDateTime/now))]
    (send-email config to subject formatted-body)))