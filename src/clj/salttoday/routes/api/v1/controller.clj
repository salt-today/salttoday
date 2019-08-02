(ns salttoday.routes.api.v1.controller
  (:require [salttoday.db.comments :refer [get-comments]]
            [salttoday.db.users :refer [get-users]]
            [salttoday.routes.util :as routing-util]
            [ring.util.response :as response]
            [ring.util.http-response :as http-response]))

; TODO Once we avoid the whole - passing in 0 instead of nil / the lack of a key
; we should make the parsing functions not require strings
(defn fetch-comments [{:keys [offset amount sort-type days search-text user id deleted]
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
        user-str (if (not (clojure.string/blank? user)) user)   ; convert to nil if string is blank
        results (get-comments offset-num amount-num (keyword sort-type) days-num search-text user-str id-num deleted-bool)]
    (-> (http-response/ok results)
        (response/header "Content-Type"
                         "application/json"))))

(defn fetch-users [{:keys [offset amount days sort-type]
                    :or {offset "0"
                         amount "20"
                         days "0"
                         sort-type "score"}}]
  (let [offset-num (routing-util/string->number offset)
        amount-num (routing-util/string->number amount)
        days-num (routing-util/string->number days)]
    (-> (http-response/ok (get-users offset-num amount-num (keyword sort-type) days-num))
        (response/header "Content-Type"
                         "application/json"))))