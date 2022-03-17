(ns ought.server
   (:require [org.httpkit.server :as app-server]
             [ring.util.response :as util.response]
             [ring.util.request :as util.request]
             [ring.middleware.cors :as cors]
             [clojure.edn :as edn]))

(defn ->body [req]
  (-> req 
      util.request/body-string
      edn/read-string))

(defn with-headers [res]
  (-> res
      (util.response/header "Access-Control-Allow-Origin" "*")
      (util.response/header "Access-Control-Allow-Headers" "content-type")))

(defn ->response [data]
  (-> data
      pr-str
      util.response/response
      with-headers))

(defn handler [req]
  (def req req)
  (->
   (case (util.request/path-info req)
     "/sequence" (->response (->body req))
     (util.response/not-found "unknown route"))
   (util.response/header "Access-Control-Allow-Origin" "*")))

(defonce server
  (app-server/run-server #'handler {:port 8011}))