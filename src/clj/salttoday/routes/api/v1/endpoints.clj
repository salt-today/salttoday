(ns salttoday.routes.api.v1.endpoints
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]
            [clojure.tools.logging :as log]
            [clojure.string :refer [blank?]]))

; TODO - return nil and move to a common namespace
(defn string->number
  "Converts a string to a number, if nil or not a number, returns 0."
  [str]
  (if (blank? str)
    0
    (let [n (read-string str)]
      (if (number? n) n 0))))

(defroutes endpoints
  (GET "/api/v1/todays-stats" []
    (honeycomb/send-metrics {"api-hit" "todays-stats"})
    (-> (response/ok (db/get-todays-stats))
        (response/header "Content-Type"
                         "application/json")))

  ; TODO Would be better if we passed in nil instead of 0, see string->number function.
  (GET "/api/v1/comments" [offset amount sort-type days search-text user id]
    (let [offset-num (string->number offset)
          amount-num (string->number amount)
          days-num (string->number days)
          id-num (string->number id)
          results (db/get-top-x-comments offset-num amount-num sort-type days-num search-text user id-num)]
      (-> (response/ok results)
          (response/header "Content-Type"
                           "application/json"))))

  (GET "/api/v1/users" [offset amount sort-type days]
    (honeycomb/send-metrics {"api-hit" "top-users"})
    (let [offset-num (string->number offset)
          amount-num (string->number amount)
          days-num (string->number days)]
      (-> (response/ok (db/get-top-x-users offset-num amount-num sort-type days-num))
          (response/header "Content-Type"
                           "application/json")))))