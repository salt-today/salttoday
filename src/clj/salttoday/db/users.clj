(ns salttoday.db.users
  (:require [datomic.api :as d]
            [clojure.set :as set]
            [salttoday.db.util :refer [remap-query sort-by-specified get-date paginate-results tx-with-logging]]
            [salttoday.db.connection :refer [conn]]
            [salttoday.metrics.core :as honeycomb]
            [clojure.tools.logging :as log]))

(defn db-users->frontend-users
  "Given a collection of users from the database, rename their keys to that which the frontend expects"
  [users]
  (for [user users]
    (-> user first
        (set/rename-keys {:user/upvotes :upvotes
                          :user/downvotes :downvotes
                          :user/name :name}))))

; TODO - we should make the user's name unique in the schema so we can do upserts
; https://docs.datomic.com/cloud/transactions/transaction-processing.html#unique-identities
(defn add-or-get-user
  "Attempts to find the user, will transact a new user if nothing is found"
  [conn username]
  (let [user (-> (d/q '[:find ?e
                        :in $ ?name
                        :where [?e :user/name ?name]] (d/db conn) username)
                 ffirst)]
    (log/debug "was user found?" user)
    (if (nil? user)
      (do
        (honeycomb/send-metrics {"db-operation" "add-or-get-user"
                                 "username" username})
        (-> (tx-with-logging conn [{:user/name username
                                    :user/upvotes 0
                                    :user/downvotes 0}])
            :tempids first second))
      user)))

(defn get-user-stats
  "Given a user's name, retrieve their upvote and downvote stats."
  [name]
  (d/q '[:find ?upvotes ?downvotes :in $ ?name :where
         [?u :user/name ?name]
         [?u :user/upvotes ?upvotes]
         [?u :user/downvotes ?downvotes]]
       (d/db conn) name))

(def get-all-users-query
  '[:find (pull ?u [:user/upvotes :user/downvotes :user/name])
    :in $
    :where [?u :user/name ?name]])

(defn assoc-name-with-user-map
  "Given a collection of user maps, associate each map with the user's name."
  [user-maps]
  (apply merge
         (for [user-map user-maps]
           {(:name user-map) user-map})))

; Used as a default if user can't be found in past
(def empty-user-map {:upvotes 0 :downvotes 0})

(defn get-user-scores-for-time-range
  "Retrieve user's scores in a particular time-range. For example, top user that week."
  [db current-users-map days-ago-date]
  (let [past-users (d/q get-all-users-query (d/as-of db days-ago-date))
        past-users-maps (db-users->frontend-users past-users)
        new-past-users-map (assoc-name-with-user-map past-users-maps)
        new-current-users-map (assoc-name-with-user-map current-users-map)]
    (for [user-key (keys new-current-users-map)]
      (let [current-user-map (get new-current-users-map user-key)
            past-user-map (get new-past-users-map user-key empty-user-map)]
        (assoc current-user-map
               :upvotes (- (:upvotes current-user-map)
                           (:upvotes past-user-map))
               :downvotes (- (:downvotes current-user-map)
                             (:downvotes past-user-map)))))))

; TODO - we must pass in a keyword for sorting!!!
(defn get-users-paginated
  "Get users paginated"
  ([db offset amount sort-type days-ago]
   (let [users (d/q get-all-users-query db)
         user-maps (db-users->frontend-users users)
         days-ago-date (get-date days-ago)
         user-maps (if days-ago-date
                     (get-user-scores-for-time-range db user-maps days-ago)
                     user-maps)
         sorted-users (sort-by-specified user-maps sort-type)]
     (paginate-results offset amount sorted-users)))
  ([offset amount sort-type days-ago]
   (get-users-paginated (d/db conn) offset amount sort-type days-ago)))