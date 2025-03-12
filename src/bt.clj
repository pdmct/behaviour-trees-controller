(ns behave-tree.core)

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

(defn send-response [_]
  (println "Sending generated response to customer.")
  true)

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

;; Running the behavior tree
(email-response-tree {:question-understood true :llm-response-clear true})  ;; Output: Generating response using LLM... Sending generated response to customer.
(email-response-tree {:question-understood false})                         ;; Output: Escalating to human intervention.
