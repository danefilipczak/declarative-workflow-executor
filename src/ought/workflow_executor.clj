(ns ought.workflow-executor
  (:require [hyperfiddle.rcf :refer [tests]]))

(defn evaluate* [data]
  (:output data))

(defn evaluate [& [{:keys [entry_point tasks] :as _workflow} :as args]]
  (let [evaluated-tasks (->> (for [[task data] tasks]
                               [task (evaluate* data)])
                             (into {}))]
    (get evaluated-tasks entry_point)))

(tests
 (evaluate {:entry_point "x"
            :tasks {"x" {:output "y"}}})
  := "y"
 )


