(ns salttoday.routes.api.v1.endpoints
  (:require [clojure.walk :refer [keywordize-keys]]
            [salttoday.routes.api.v1.controller :refer [fetch-comments]]
            [salttoday.db.users :refer [get-users]]
            [salttoday.db.statistics :refer [get-todays-stats]]
            [salttoday.metrics.core :as honeycomb]
            [salttoday.routes.util :as routing-util]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]))



(defroutes endpoints
  (GET "/api/v1/todays-stats" []
    (honeycomb/send-metrics {"api-hit" "todays-stats"})
    (-> (response/ok (get-todays-stats))
        (response/header "Content-Type"
                         "application/json")))

  ; TODO Would be better if we passed in nil instead of 0, see string->number function.
  (GET "/api/v1/comments" [_ :as req]
    (let [params (keywordize-keys (:query-params req))]
      (fetch-comments params)))

  (GET "/api/v1/users" [offset amount sort-type days]
    (honeycomb/send-metrics {"api-hit" "top-users"})
    (let [offset-num (routing-util/string->number offset)
          amount-num (routing-util/string->number amount)
          days-num (routing-util/string->number days)]
      (-> (response/ok (get-users offset-num amount-num (keyword sort-type) days-num))
          (response/header "Content-Type"
                           "application/json")))))