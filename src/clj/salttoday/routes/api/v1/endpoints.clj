(ns salttoday.routes.api.v1.endpoints
  (:require [salttoday.db.comments :refer [get-comments]]
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
  (GET "/api/v1/comments" [offset amount sort-type days search-text user id deleted]
    (let [offset-num (routing-util/string->number offset)
          amount-num (routing-util/string->number amount)
          days-num (routing-util/string->number days)
          id-num (routing-util/string->number id)
          deleted-bool (routing-util/string->bool deleted)
          results (get-comments offset-num amount-num sort-type days-num search-text user id-num deleted-bool)]
      (-> (response/ok results)
          (response/header "Content-Type"
                           "application/json"))))

  (GET "/api/v1/users" [offset amount sort-type days]
    (honeycomb/send-metrics {"api-hit" "top-users"})
    (let [offset-num (routing-util/string->number offset)
          amount-num (routing-util/string->number amount)
          days-num (routing-util/string->number days)]
      (-> (response/ok (get-users offset-num amount-num sort-type days-num))
          (response/header "Content-Type"
                           "application/json")))))