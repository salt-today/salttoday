(ns salttoday.db.util
  (:require [datomic.api :as d]
            [clojure.tools.logging :as log]))

(defn sort-by-specified
  "Given a sort-type, will return the comments/users sorted.
   Valid options include:
   - :upvotes
   - :downvotes
   - :score (default)"
  [sort-type comments]
  (case sort-type
    :upvotes (sort #(> (:upvotes %1)
                       (:upvotes %2)) comments)
    :downvotes (sort #(> (:downvotes %1)
                         (:downvotes %2)) comments)
    (sort #(> (+ (/ (:upvotes %1) 2) (:downvotes %1))
              (+ (/ (:upvotes %2) 2) (:downvotes %2)))
          comments)))

(defn remap-query
  "Used to generate conditional queries - https://grishaev.me/en/datomic-query"
  [{args :args :as m}]
  {:query (dissoc m :args)
   :args args})

; TODO eventually switch back to using `since`
; For example - using since / normal db in the same query
(comment (d/q ('[:find ?count
                 :in $ $since ?id
                 :where [$ ?e :item/id ?id]
                 [$since ?e :item/count ?count]])))

; TODO - this is still not amazing and changes with daylight savings time
(defn get-date
  "Returns a date time of the current date minus a number of days.
   If given a number less than 1, returns nil."
  [days-ago]
  (if (<= 1 days-ago)
    (-> (java.time.LocalDateTime/now)
        (.minusHours 5)
        (.minusDays days-ago)
        (.atZone (java.time.ZoneId/of "America/Toronto"))
        (.toInstant)
        (java.util.Date/from))))

(defn paginate-results
  "Select from a collection based on an offset and an amount"
  [offset amount results]
  (->> (drop offset results)
       (take amount)))

(defn tx-with-logging
  "Commits a transaction, logging the tx-data and the subsequent result"
  [& tx-data]
  (log/debug "Transaction arguments:" tx-data)
  (let [result @(apply d/transact tx-data)]
    (log/debug "Transaction result:" result)
    result))