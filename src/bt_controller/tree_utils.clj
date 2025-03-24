(ns bt-controller.tree-utils
  (:require [clojure.string :as str]))

(defn generate-tree-structure
  "Generate a list of nodes and edges from a behavior tree.
   Returns a map with :nodes (set of unique node IDs) and :edges (set of [parent-id child-id] pairs)."
  [tree]
  (let [nodes (atom #{})  ; Set of node IDs
        edges (atom #{})  ; Set of [parent-id child-id] pairs
        ordered-children (atom {})  ; Map of parent-id to ordered list of child-ids
        id-counter (atom 0)  ; Counter for generating unique IDs
        visited (atom #{})]    ; Track visited nodes to avoid duplicates
    (letfn [(traverse [node parent-id]
              (when (vector? node)
                (let [node-type (first node)
                      current-id (str node-type "-" (swap! id-counter inc))

                      ; Add to nodes vector if not already visited
                      _ (when-not (contains? @visited current-id)
                          (swap! nodes conj current-id)
                          (swap! visited conj current-id))


                      ; Add edge if we have a parent
                      _ (when parent-id
                          (swap! edges conj [parent-id current-id])
                          ; tracked ordered children
                          (swap! ordered-children update parent-id
                                 (fn [children] (conj (or children []) current-id))))

                      ; Skip options map if present
                      children-start-idx (if (and (> (count node) 1)
                                                  (map? (second node)))
                                           2  ; Skip node type and options map
                                           1)] ; Skip just node type

                  ; Process all children
                  (doseq [child (subvec node children-start-idx)]
                    (when (vector? child)  ; Ensure child is a node
                      (traverse child current-id)))

                  ; Return this node's ID
                  current-id)))]

      ; Start traversal with the root node
      (traverse tree nil)

      ; Return the results
      {:nodes @nodes
       :edges @edges
       :ordered-children @ordered-children})))

(defn adjacent-nodes
  "Returns a function that returns set of node IDs that are directly connected to the given node."
  [{:keys [edges ordered-children]}]
  (fn [node-id]
    (or (get ordered-children node-id) [])))

(defn node->descriptor
  "Creates a descriptor map for visualization, with special icons for sequence and selector nodes."
  [node-id]
  (let [node-type (first (str/split node-id #"-"))
        clean-id (str/join "-" (butlast (str/split node-id #"-")))]
    (case node-type
      ":sequence" {:label "→" :shape "circle"}
      ":selector" {:label "?" :shape "circle"}
      ":loop" {:label "↻" :shape "circle"}
      (let [is-condition? (str/ends-with? clean-id "?")]
        {:label clean-id
         :shape (if is-condition? "ellipse" "rectangle")}))))