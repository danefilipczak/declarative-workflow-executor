(ns ought.frontend.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.ratom :as r]
   [reagent.dom :as r.dom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [ought.json :refer [edn->json-string json-string->edn]]))

(def workflows
  [{:entry_point :hello_world
    :tasks {:hello_world {:output "hello world!"}}}
   {:entry_point :hello_name
    :tasks {:name {:output "Alan"}
            :hello_name {:output "hello ${name}!"}}}
   {:input true
    :entry_point :hello_input
    :tasks {:hello_input {:output "hello ${input}!"}}};; note: this is assuming a typo in the instructions - the declared output there is "hello ${name}", but I'm assuming the correct output to be "hello ${input}"
   {:entry_point :slow_goodbye
    :tasks {:slow_goodbye {:steps [{:wait 5}] ;; note: assuming a typo in the instructions, the instructions have slow_hello as the task name
                           :output "goodbye!"}}}
   {:entry_point :join
    :tasks {:join {:output "${slow_goodbye} ${slow_name}!"}
            :slow_goodbye {:steps [{:wait 5}]
                           :output "goodbye"}
            :slow_name {:steps [{:wait 5}]
                        :output "Ada"}}}])

(def address "http://localhost:8011")

(defn post-workflow [workflow]
  (go
    (let [response (<! (http/post (str address "/workflow")
                                  {:with-credentials? false
                                   :body (edn->json-string workflow)}))]
      (-> response :body json-string->edn))))

(defn dispatch-workflow! [app-state workflow]
  (go (swap! app-state assoc :workflow-response (<! (post-workflow workflow)))))

(defn app []
  (let [app-state (r/atom {:selected-workflow 0})]
    (fn []
      (let [selected-workflow (get workflows (-> app-state deref :selected-workflow))
            user-input (:input @app-state)]
        [:div
         [:select
          {:on-change #(swap! app-state assoc 
                              :selected-workflow 
                              (-> % .-target .-value js/parseInt)
                              :input nil)
           :value (-> app-state deref :selected-workflow)}
          (->> workflows
               (map-indexed (fn [i _workflow]
                              [:option {:value i} (str "workflow " i)])))]

         [:pre (edn->json-string (get workflows (-> app-state deref :selected-workflow)))]

         [:div
          (when (:input selected-workflow)
            [:input {:type :text
                     :on-change #(swap! app-state assoc :input (-> % .-target .-value))
                     :value user-input}])

          [:button
           {:disabled (-> app-state deref :pending-request?)
            :on-click (fn []
                        (swap! app-state assoc :pending-request? true)
                        (go (<! (dispatch-workflow! 
                                 app-state 
                                 (cond-> selected-workflow
                                   user-input
                                   (assoc :input user-input))))
                            (swap! app-state assoc :pending-request? false)))}
           "Execute workflow"]]

         [:pre "Result:"]
         (when-let [result (:workflow-response @app-state)]
           [:pre result])]))))

(defn mount []
  (r.dom/render [app] (js/document.getElementById "root")))

(defn init []
  (mount))