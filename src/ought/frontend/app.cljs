(ns ought.frontend.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.ratom :as r]
   [reagent.dom :as r.dom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [ought.json :refer [edn->json-string json-string->edn]]))

(defn post-workflow [workflow]
  (go
    (let [response (<! (http/post "http://localhost:8011/workflow"
                                  {:with-credentials? false
                                   :body (edn->json-string workflow)}))]
      (-> response :body json-string->edn))))

(defn dispatch-workflow! [app-state workflow]
  (go (swap! app-state assoc :workflow-response (<! (post-workflow workflow)))))

(def workflow-0 {:entry_point :hello_world
                 :tasks {:hello_world {:output "hello world!"}}})

(def workflow-1 {:entry_point :hello_name
                 :tasks {:name {:output "Alan"}
                         :hello_name {:output "hello ${name}!"}}})

(defn app []
  (let [app-state (r/atom {})]
    (fn []
      [:div
       [:button
        {:on-click (fn []
                     (go (<! (dispatch-workflow! app-state workflow-0))))}
        "Hello World"]
       [:button
        {:on-click (fn []
                     (go (<! (dispatch-workflow! app-state workflow-1))))}
        "Hello Name"]
       [:pre "Result:"]
       (when-let [result (:workflow-response @app-state)]
         [:pre result])])))

(defn mount []
  (r.dom/render [app] (js/document.getElementById "root")))

(defn init []
  (mount))