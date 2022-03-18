(ns ought.frontend.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.ratom :as r]
   [reagent.dom :as r.dom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [ought.json :refer [edn->json-string json-string->edn]]))

(def workflow-0 {:entry_point :hello_world
                 :tasks {:hello_world {:output "hello world!"}}})

(def workflow-1 {:entry_point :hello_name
                 :tasks {:name {:output "Alan"}
                         :hello_name {:output "hello ${name}!"}}})

(def workflow-2 {:entry_point :hello_input
                 :tasks {:hello_input {:output "hello ${input}!"}}}) ;; note: this is assuming a typo in the instructions - the declared output there is "hello ${name}", but I'm assuming the correct output to be "hello ${input}"

(def workflow-3 {:entry_point :slow_goodbye
                 :tasks {:slow_goodbye {:steps [{:wait 5}] ;; note: assuming a typo in the instructions, the instructions have slow_hello as the task name
                                        :output "goodbye!"}}})

(defn post-workflow [workflow]
  (go
    (let [response (<! (http/post "http://localhost:8011/workflow"
                                  {:with-credentials? false
                                   :body (edn->json-string workflow)}))]
      (-> response :body json-string->edn))))

(defn dispatch-workflow! [app-state workflow]
  (go (swap! app-state assoc :workflow-response (<! (post-workflow workflow)))))

(defn app []
  (let [app-state (r/atom {})]
    (fn []
      [:div
       [:div [:button
              {:on-click (fn []
                           (go (<! (dispatch-workflow! app-state workflow-0))))}
              "Hello World"]]
       
       [:div [:button
              {:on-click (fn []
                           (go (<! (dispatch-workflow! app-state workflow-1))))}
              "Hello Name"]]
       
       [:div [:input {:type :text
                      :on-change #(swap! app-state assoc :input (-> % .-target .-value))
                      :value (:input @app-state)}]
        [:button
         {:on-click (fn []
                      (go (<! (dispatch-workflow! 
                               app-state 
                               (assoc workflow-2 :input (:input @app-state))))))}
         "Hello Input"]]
       
       [:div 
        [:button
         {:disabled (-> app-state deref :pending-request?)
          :on-click (fn []
                      (swap! app-state assoc :pending-request? true)
                      (go (<! (dispatch-workflow! app-state workflow-3))
                          (swap! app-state assoc :pending-request? false)))}
         "Slow Goodbye"]]
       
       [:pre "Result:"]
       (when-let [result (:workflow-response @app-state)]
         [:pre result])])))

(defn mount []
  (r.dom/render [app] (js/document.getElementById "root")))

(defn init []
  (mount))