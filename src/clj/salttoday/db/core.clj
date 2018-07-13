(ns salttoday.db.core
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [salttoday.config :refer [env]]
            [salttoday.scraper :as scraper]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

;; TODO this should be uncommented

(d/create-database "datomic:mem://sample-database")
;(defstate conn
;  :start (-> env :database-url d/connect)
;  :stop (-> conn .release))

(def conn (d/connect "datomic:mem://sample-database"))

;; TODO this should eventually be removed


(defn create-schema []
  (let [schema [{:db/ident              :user/name
                 :db/valueType          :db.type/string
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :user/upvotes
                 :db/valueType          :db.type/long
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :user/downvotes
                 :db/valueType          :db.type/long
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}

                {:db/ident              :comment/text
                 :db/valueType          :db.type/string
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :comment/time
                 :db/valueType          :db.type/string
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :comment/upvotes
                 :db/valueType          :db.type/long
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :comment/downvotes
                 :db/valueType          :db.type/long
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :comment/user
                 :db/valueType          :db.type/ref
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}

                {:db/ident              :post/url
                 :db/valueType          :db.type/string
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                ;{:db/ident              :post/title
                ; :db/valueType          :db.type/string
                ; :db/cardinality        :db.cardinality/one
                ; :db.install/_attribute :db.part/db}
                {:db/ident              :post/comment
                 :db/valueType          :db.type/ref
                 :db/cardinality        :db.cardinality/many
                 :db.install/_attribute :db.part/db}]]
    @(d/transact conn schema)))

(defn ^:private add-or-get-user [conn username]
  (let [user (-> (d/q '[:find ?e :in $ ?name :where [?e :user/name ?name]] (d/db conn) username)
                 ffirst)]
    (if (nil? user)
      (-> @(d/transact conn [{:user/name username
                              :user/upvotes 0
                              :user/downvotes 0}])
          :tempids first second)
      user)))

;; add title to keys eventually once that is also scraped
(defn ^:private add-post [conn {:keys [url]}]
  ; Check if the post exists, if it doesn't add it.
  (let [post-id (-> (d/q '[:find ?e :in $ ?url :where [?e :post/url ?url]] (d/db conn) url)
                 ffirst)]
    (if (nil? post-id)
      (-> @(d/transact conn [{:post/url url}])
          :tempids first second)
      post-id)))

(defn ^:private vote-difference [old new]
  (if (nil? old)
    new
    (- new old)))

;; TODO: THIS REALLY SHOULD BE A TRANSACTION FUNCTION
(defn ^:private add-comment [conn post-id {:keys [username comment timestamp upvotes downvotes]}]
  (let [user-id (add-or-get-user conn username)
        user-stats (-> (d/q '[:find ?upvotes ?downvotes :in $ ?user-id :where
                             [?user-id :user/upvotes ?upvotes]
                             [?user-id :user/downvotes ?downvotes]]
                            (d/db conn)
                            user-id)
                       first)
        user-upvotes (first user-stats)
        user-downvotes (second user-stats)

        comment-id (or (-> (d/q '[:find ?e :in $ ?user-id ?timestamp :where
                                  [?e :comment/user ?user-id]
                                  [?e :comment/time ?timestamp]]
                                (d/db conn)
                                user-id timestamp)
                           ffirst)
                       0)
        comment-stats (-> (d/q '[:find ?upvotes ?downvotes :in $ ?comment-id :where
                                 [?comment-id :comment/upvotes ?upvotes]
                                 [?comment-id :comment/downvotes ?downvotes]]
                               (d/db conn)
                               comment-id)
                          first)
        comment-upvotes (first comment-stats)
        comment-downvotes (second comment-stats)

        upvote-increase (vote-difference comment-upvotes upvotes)
        downvote-increase (vote-difference comment-downvotes downvotes)]
    (if (zero? comment-id)
      @(d/transact conn [{:db/id "comment"
                          :comment/user user-id
                          :comment/text comment
                          :comment/time timestamp
                          :comment/upvotes upvotes
                          :comment/downvotes downvotes}
                         {:db/id post-id
                          :post/comment "comment"}])
      @(d/transact conn [{:db/id comment-id
                          :comment/upvotes upvotes
                          :comment/downvotes downvotes}]))
    @(d/transact conn [{:db/id user-id
                       :user/upvotes (+ user-upvotes upvote-increase)
                       :user/downvotes (+ user-downvotes downvote-increase)}])))


(defn ^:private add-comments [conn post-id comments]
  (doseq [comment comments]
    (add-comment conn post-id comment)))



(defn update-stats [conn posts]
  (doseq [post posts]
    (let [post-id (add-post conn post)]
      (add-comments conn post-id (:comments post)))))


; -----------------------------------------------------------------------------------
; Queries ---------------------------------------------------------------------------
; -----------------------------------------------------------------------------------

; ---------
; Comments
; ---------
(defn get-most-x-comments
  ([vote-type num db]
   (let [comments (d/q '[:find ?votes ?name ?text  ?url :in $ ?vote-type :where
                         [?c :comment/text ?text]
                         [?c ?vote-type ?votes]
                         [?c :comment/user ?u]
                         [?u :user/name ?name]
                         [?p :post/comment ?c]
                         [?p :post/url ?url]] db vote-type)
         sorted-comments (sort-by first > comments)
         top-x (take num sorted-comments)]
     (for [comment top-x]
       (apply assoc {}
              (interleave [:votes :user :text :url] comment)))))
  ([vote-type num]
   (get-most-x-comments vote-type num (d/db conn))))

(defn get-most-positive-comments
  ([num db]
   (get-most-x-comments :comment/upvotes num db))
  ([num]
   (get-most-positive-comments num (d/db conn))))

(defn get-most-negative-comments
  ([num db]
   (get-most-x-comments :comment/downvotes num db))
  ([num]
   (get-most-negative-comments num (d/db conn))))

(defn db-since-yesterday []
  (let [yesterday-time (-> (t/now)
                           (t/minus (t/days 1))
                           (c/to-date))]
    (d/since (d/db conn) yesterday-time)))

(defn get-daily-positive-comment []
    (first (get-most-positive-comments 1 (db-since-yesterday))))

(defn get-daily-negative-comment []
  (first (get-most-negative-comments 1 (db-since-yesterday))))



; -----------
; User stats
; -----------

(defn get-user-stats
  [name]
  (d/q '[:find ?upvotes ?downvotes :in $ ?name :where
         [?u :user/name ?name]
         [?u :user/upvotes ?upvotes]
         [?u :user/downvotes ?downvotes]]
       (d/db conn) name))

(defn get-most-negative-users
  []
  (d/q '[:find ?name ?upvotes ?downvotes :where
         [?u :user/name ?name
          ?c :comment/user ?u
          ?c :comment/upvotes
          ?c]]))

(defn get-most-x-users
  [vote-type num]
  (let [users (d/q '[:find ?name ?vote :in $ ?vote-type :where
         [?u :user/name ?name]
         [?u ?vote-type ?vote]]
                   (d/db conn) vote-type)
        sorted-users (sort-by second > users)
        top-users (take num sorted-users)]
    (for [user top-users]
      (apply assoc {}
             (interleave [:name :votes] user)))))

(defn get-most-negative-users
  [num]
  (get-most-x-users :user/downvotes num))

(defn get-most-positive-users
  [num]
  (get-most-x-users :user/upvotes num))


