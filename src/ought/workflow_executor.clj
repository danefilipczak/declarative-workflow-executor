(ns ought.workflow-executor
  (:require [hyperfiddle.rcf :refer [tests]]
            [clojure.string :as string]))

(defn evaluate* [& [{:keys [tasks] :as workflow} {:keys [output] :as _task} :as args]]
  (let [pattern #"\$\{[a-zA-Z_-]*\}"
        substitutions (->> (re-seq pattern output)
                           (map #(->> % (drop 2) butlast (apply str) keyword)))
        evaluated-substitutions (map
                                 (fn [substitution]
                                   (if (= substitution :input)
                                     (:input workflow)
                                     (evaluate* workflow (get tasks substitution))))
                                 substitutions)]
    (apply format
           (string/replace output pattern "%s")
           evaluated-substitutions)))

(defn evaluate [& [{:keys [entry_point tasks] :as workflow} :as args]]
  (evaluate* workflow (get tasks entry_point)))

(tests
 ;;step 0
 (evaluate {:entry_point :x
            :tasks {:x {:output "y"}}})
  := "y"
 
 ;;step 1
 (evaluate {:entry_point :x
            :tasks {:x {:output "here is y: ${y}!"}
                    :y {:output "value of y"}}})
 := "here is y: value of y!"

 ;;step 2
 (evaluate {:input "user input"
            :entry_point :x
            :tasks {:x {:output "retreived from input: ${input}"}}})
 := "retreived from input: user input"
 )


