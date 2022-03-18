(ns ought.workflow-executor
  (:require [hyperfiddle.rcf :refer [tests]]
            [clojure.string :as string]))

(defmulti evaluate-step (fn [step] (-> step keys first)))

(defmethod evaluate-step :wait
  [step]
  (Thread/sleep (* 1000 (:wait step))))

(defn evaluate-steps [steps _workflow]
  (doseq [step steps]
    (evaluate-step step)))

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
  (doall (pmap (fn [[_task-key task]]
                 (evaluate-steps (:steps task) workflow))
               tasks))
  (evaluate* workflow (get tasks entry_point)))

;; TESTING HELPERS

(defmacro time
  "Evaluates expr and returns a type of val of expr and time it took."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     [ret# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]))

(defn fuzzy= [x y tolerance]
  (and (> x (- y tolerance))
       (< x (+ y tolerance))))

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

 ;;step 3
 (let [[val time] (time (evaluate {:entry_point :a
                                   :tasks {:a {:steps [{:wait 0.1}]
                                               :output "output"}}}))]
   val := "output"
   (fuzzy= time 100 15) := true
   )
 
 ;; step 4
 (let [[val time] (time (evaluate {:entry_point :c
                                   :tasks {:a {:steps [{:wait 0.2}]
                                               :output "x"}
                                           :b {:steps [{:wait 0.2}]
                                               :output "y"}
                                           :c {:output "${a}+${b}"}}}))]
   val := "x+y"
   (fuzzy= time 200 15) := true
   )
 
 )


