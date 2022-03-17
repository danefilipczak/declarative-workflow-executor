(ns ought.server
   (:require [org.httpkit.server :as app-server]
             [ring.util.response :as util.response]
             [ring.util.request :as util.request]
             [ought.json :refer [json-string->edn edn->json-string]]
             [ought.workflow-executor :as workflow-executor]
             [ring.middleware.cors :as cors]))

(defn ->body [req]
  (-> req 
      util.request/body-string
      json-string->edn))

(defn with-headers [res]
  (-> res
      (util.response/header "Access-Control-Allow-Origin" "*")
      (util.response/header "Access-Control-Allow-Headers" "content-type")))

(defn ->response [data]
  (-> data
      edn->json-string
      util.response/response
      with-headers))

(defn handler [req]
  (def req req)
  (->
   (case (util.request/path-info req)
     "/workflow" (-> req 
                     ->body
                     workflow-executor/evaluate
                     ->response)
     (util.response/not-found "unknown route"))))

(defonce server
  (app-server/run-server #'handler {:port 8011}))