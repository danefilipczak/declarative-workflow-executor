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

(defn dispatch-workflow! [state workflow]
  (go (swap! state assoc :workflow-response (<! (post-workflow workflow)))))

(defn app []
  (let [state (r/atom {})]
    (fn []
      [:div
       [:button
        {:on-click (fn []
                     (go (<! (dispatch-workflow!
                              state
                              {:entry_point :hello_world
                               :tasks {:hello_world {:output "hello world!"}}}))))} "Hello World"]
       [:pre "Result:"]
       (when-let [result (:workflow-response @state)]
         [:pre result])])))

(defn mount []
  (r.dom/render [app] (js/document.getElementById "root")))

(defn init []
  (mount))