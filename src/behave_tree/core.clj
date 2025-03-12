(ns behave-tree.core
  (:require [aido.compile :as ac]
            [aido.options :as ao]
            [aido.core :as ai]
            [aido.tick :as at]))

;; Behavior Tree Nodes
(defn success [_] :success)
(defn failure [_] :failure)

;; Composite nodes
(defn sequence-node [& children]
  (fn [state]
    (loop [nodes children]
      (if (empty? nodes)
        :success
        (let [result ((first nodes) state)]
          (if (= result :success)
            (recur (rest nodes))
            result))))))

(defn selector-node [& children]
  (fn [state]
    (loop [nodes children]
      (if (empty? nodes)
        :failure
        (let [result ((first nodes) state)]
          (if (= result :failure)
            (recur (rest nodes))
            result))))))

;; Decorator node
(defn inverter [child]
  (fn [state]
    (let [result (child state)]
      (case result
        :success :failure
        :failure :success
        result))))

;; Leaf node (actions)
(defn action [f]
  (fn [state]
    (if (f state)
      :success
      :failure)))

;; Email Response Behavior Tree Actions
(defn understood-question? [state]
  (:question-understood state))

(defn generate-llm-response [state]
  (println "Generating response using LLM...")
  (if (:llm-response-clear state)
    (assoc state :response-generated true)
    state))

(defn response-sufficient? [state]
  (:response-generated state))

(defn send-response [state]
  (println "Sending generated response to customer.")
  (assoc state :response-sent true))

(defn escalate-to-human [_]
  (println "Escalating to human intervention.")
  true)

;; Constructing the behavior tree for customer email responses
(def email-response-tree
  (selector-node
   (sequence-node
    (action understood-question?)
    (action generate-llm-response)
    (action response-sufficient?)
    (action send-response))
   (action escalate-to-human)))


;; define aido behaviours using the functions above
(defmethod at/tick :understood-question?
  [db & _]
  (let [input (:state db)
        _ (println (str "return value " (understood-question? input)))
        understood (understood-question? input)
        _ (println (str "understood:" understood))
        _ (println (str "db: " db))]
    (if understood
      (do
        (println "Question understood")
        (ai/tick-success db))
      (do
        (println "Can't understand question")
        (ai/tick-failure db)))))

(defmethod at/tick :generate-llm-response!
  [db & _]
  (let [new-state (generate-llm-response (:state db))]
    (ai/tick-success (assoc db
                            :state new-state))))

(defmethod at/tick :response-sufficient?
  ([db & _]
   (if (response-sufficient? (:state db))
     (ai/tick-success db)
     (ai/tick-failure db))))

(defmethod at/tick :send-response!
  [db & _]
  (ai/tick-success (assoc db
                          :state
                          (send-response (:state db)))))

(defmethod at/tick :escalate-to-human!
  [db & _]
  (ai/tick-success (assoc db
                          :escalated
                           (escalate-to-human (:state db)))))

(defn main[args]
  ;; Running the behavior tree
  ;;(email-response-tree {:question-understood true :llm-response-clear true})  ;; Output: Generating response using LLM... Sending generated response to customer.
  ;;(email-response-tree {:question-understood false})                         ;; Output: Escalating to human intervention.
  (let [fns {}
        tree (ac/compile [:selector
                          [:sequence
                           [:understood-question?]
                           [:generate-llm-response!]
                           [:response-sufficient?]
                           [:send-response!]]
                          [:escalate-to-human!]]                         
                         fns)
        db {:state {:question-understood true
                    :llm-response-clear true}}]
    (let [{:keys [db status]} (ai/run-tick db tree)]
      (print (str "db:" db))
      (if (and (= ai/SUCCESS status)
               (get-in db [:state :response-sent]))
        (print (str "Email generated and sent!"))
        (print "Help!")))))  ;; Output: Escalating to human intervention.
 
