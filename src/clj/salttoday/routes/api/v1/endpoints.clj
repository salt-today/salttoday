(ns salttoday.routes.api.v1.endpoints
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [salttoday.routes.util :as routing-util]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]
            [clojure.tools.logging :as log]))

; TODO Once we avoid the whole - passing in 0 instead of nil / the lack of a key
; we should make the parsing functions not require strings
(defn maybe [{:keys [offset amount sort-type days search-text user id deleted]
              :or {offset "0"
                   amount "50"
                   sort-type "score"
                   days "1"
                   id nil
                   deleted "false"}}]
  (let [offset-num (routing-util/string->number offset)
        amount-num (routing-util/string->number amount)
        days-num (routing-util/string->number days)
        id-num (routing-util/string->number id)
        deleted-bool (routing-util/string->bool deleted)
        results (db/get-top-x-comments offset-num amount-num sort-type days-num search-text user id-num deleted-bool)]
    (-> (response/ok results)
        (response/header "Content-Type"
                         "application/json"))))

(defroutes endpoints
  (GET "/api/v1/todays-stats" []
    (honeycomb/send-metrics {"api-hit" "todays-stats"})
    (-> (response/ok (db/get-todays-stats))
        (response/header "Content-Type"
                         "application/json")))

  (GET "/api/v1/comments" [params]
    (maybe params))

  (GET "/api/v1/users" [offset amount sort-type days]
    (honeycomb/send-metrics {"api-hit" "top-users"})
    (let [offset-num (routing-util/string->number offset)
          amount-num (routing-util/string->number amount)
          days-num (routing-util/string->number days)]
      (-> (response/ok (db/get-top-x-users offset-num amount-num sort-type days-num))
          (response/header "Content-Type"
                           "application/json")))))