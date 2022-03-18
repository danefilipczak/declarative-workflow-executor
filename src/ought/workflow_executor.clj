(ns ought.workflow-executor
  (:require [hyperfiddle.rcf :refer [tests]]
            [clojure.string :as string]))

(defn evaluate* [& [tasks {:keys [output] :as _task} :as args]]
  (let [pattern #"\$\{[a-zA-Z_-]*\}"
        sub-tasks (->> (re-seq pattern output)
                       (map #(->> % (drop 2) butlast (apply str) keyword))
                       (map #(get tasks %)))
        evaluated-subtasks (map (partial evaluate* tasks) sub-tasks)]
    (apply format 
           (string/replace output pattern "%s")
           evaluated-subtasks)))

(defn evaluate [& [{:keys [entry_point tasks] :as _workflow} :as args]]
  (evaluate* tasks (get tasks entry_point)))

(tests
 (evaluate {:entry_point :x
            :tasks {:x {:output "y"}}})
  := "y"
 
 (evaluate {:entry_point :x
            :tasks {:x {:output "here is y: ${y}!"}
                    :y {:output "value of y"}}})
 := "here is y: value of y!"
 )


