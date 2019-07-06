(ns salttoday.routes.api.v1.endpoints
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [salttoday.routes.util :as routing-util]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]
            [clojure.tools.logging :as log]))

(defroutes endpoints
  (GET "/api/v1/todays-stats" []
    (honeycomb/send-metrics {"api-hit" "todays-stats"})
    (-> (response/ok (db/get-todays-stats))
        (response/header "Content-Type"
                         "application/json")))

  ; TODO Would be better if we passed in nil instead of 0, see string->number function.
  (GET "/api/v1/comments" [offset amount sort-type days search-text user id deleted]
    (let [offset-num (routing-util/string->number offset)
          amount-num (routing-util/string->number amount)
          days-num (routing-util/string->number days)
          id-num (routing-util/string->number id)
          deleted-bool (routing-util/string->bool deleted)
          user-str (if (not (clojure.string/blank? user)) user)   ; convert to nil if string is blank
          results (db/get-top-x-comments offset-num amount-num sort-type days-num search-text user-str id-num deleted-bool)]
      (-> (response/ok results)
          (response/header "Content-Type"
                           "application/json"))))

  (GET "/api/v1/users" [offset amount sort-type days]
    (honeycomb/send-metrics {"api-hit" "top-users"})
    (let [offset-num (routing-util/string->number offset)
          amount-num (routing-util/string->number amount)
          days-num (routing-util/string->number days)]
      (-> (response/ok (db/get-top-x-users offset-num amount-num sort-type days-num))
          (response/header "Content-Type"
                           "application/json")))))