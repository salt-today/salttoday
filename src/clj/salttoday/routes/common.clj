(ns salttoday.routes.common
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

(defn string->number [str]
  "Converts a string to a number, if nil or not a number, returns 0."
  (if (nil? str)
    0
    (let [n (read-string str)]
      (if (number? n) n 0))))

(defroutes common-routes
  (GET "/todays-stats" []
    (honeycomb/send-metrics {"page-view" "todays-stats"})
    (-> (response/ok (db/get-todays-stats))
        (response/header "Content-Type"
                         "application/json"))))



