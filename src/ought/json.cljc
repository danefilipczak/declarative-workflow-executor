(ns ought.json
  "special-purpose json to edn de/encoder"
  (:require #?(:clj [clojure.data.json :as json])
            [hyperfiddle.rcf :refer [tests]]
            [clojure.walk :as walk]))

(defn edn->json-string [edn]
  #?(:clj (json/write-str edn)
     :cljs (.stringify js/JSON (clj->js edn) nil "  ")))

(tests 
 (edn->json-string {:entry_point :hello_world, :tasks {:hello_world {:output "hello world!"}}})
 :=
 "{\"entry_point\":\"hello_world\",\"tasks\":{\"hello_world\":{\"output\":\"hello world!\"}}}"
)

(defn json-string->edn [json-string]
  (let [keys-with-special-values #{:entry_point}]
    #?(:clj (json/read-str json-string
                           :key-fn
                           keyword
                           :value-fn (fn [k v]
                                       (cond-> v
                                         (keys-with-special-values k)
                                         keyword)))
       :cljs (->> (js->clj
                   (.parse js/JSON json-string)
                   :keywordize-keys true)
                  (walk/postwalk
                   (fn [x]
                     (if-not (map-entry? x)
                       x
                       (if (keys-with-special-values (key x))
                         [(key x) (-> x val keyword)]
                         x))))))))

(tests
 (json-string->edn "{
    \"entry_point\": \"hello_world\",
    \"tasks\": {
      \"hello_world\": {
        \"output\": \"hello world!\"
      }
    }
  }
")
 :=
 {:entry_point :hello_world, :tasks {:hello_world {:output "hello world!"}}})

;; roundtrips
(tests
 (let [edn {:entry_point :hello_world, :tasks {:hello_world {:output "hello world!"}}}]
   (-> edn edn->json-string json-string->edn) := edn )
 )