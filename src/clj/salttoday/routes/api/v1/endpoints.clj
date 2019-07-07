(ns salttoday.routes.api.v1.endpoints
  (:require [clojure.walk :refer [keywordize-keys]]
            [salttoday.routes.api.v1.controller :refer [fetch-comments fetch-users]]
            [salttoday.db.statistics :refer [get-todays-stats]]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]))

; TODO - add params to API event
(defroutes endpoints
  (GET "/api/v1/todays-stats" []
    (honeycomb/send-metrics {"api-hit" "todays-stats"})
    (-> (response/ok (get-todays-stats))
        (response/header "Content-Type"
                         "application/json")))

  ; TODO Would be better if we passed in nil instead of 0, see string->number function.
  (GET "/api/v1/comments" [_ :as req]
    (let [params (keywordize-keys (:query-params req))]
      (honeycomb/send-metrics {"api-hit" "comments"})
      (fetch-comments params)))

  (GET "/api/v1/users" [_ :as req]
    (let [params (keywordize-keys (:query-params req))]
      (honeycomb/send-metrics {"api-hit" "users"})
      (fetch-users params))))