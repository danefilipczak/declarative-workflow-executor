(ns ought.frontend.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.ratom :as r]
   [reagent.dom :as r.dom]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [cljs.reader :refer [read-string]]))

(defn app []
  [:div "hello world"])

(defn test-get []
  (go
    (let [response (<! (http/post "http://localhost:8011/sequence" {:with-credentials? false
                                                                   :edn-params {:hello :structure}}))]
      (println (-> response :body read-string keys)))))

(defn mount []
  (r.dom/render [app] (js/document.getElementById "root")))

(defn init []
  (test-get)
  (println "Hello World")
  (mount))