(ns salttoday.db.core
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [salttoday.config :refer [env]]
            [salttoday.metrics.core :as honeycomb]
            [salttoday.scraper :as scraper]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defstate conn
  :start (let [db-url (:database-url env)]
           (d/create-database db-url)
           (d/connect db-url))
  :stop (-> conn .release))

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
                {:db/ident              :post/title
                 :db/valueType          :db.type/string
                 :db/cardinality        :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                {:db/ident              :post/comment
                 :db/valueType          :db.type/ref
                 :db/cardinality        :db.cardinality/many
                 :db.install/_attribute :db.part/db}]]
    @(d/transact conn schema)))

(defn ^:private transact-and-log [& args]
  (log/info "transaction arguments:" args)
  (let [result @(apply d/transact args)]
    (log/info "transaction result:" result)
    result))

(defn ^:private add-or-get-user [conn username]
  (log/info "add-or-get-user")
  (let [user (-> (d/q '[:find ?e
                        :in $ ?name
                        :where [?e :user/name ?name]] (d/db conn) username)
                 ffirst)]
    (log/info "was user found?" user)
    (if (nil? user)
      (do
        (honeycomb/send-metrics {"db-operation" "add-or-get-user"
                                 "username" username})
        (-> (transact-and-log conn [{:user/name username
                                     :user/upvotes 0
                                     :user/downvotes 0}])
            :tempids first second))
      user)))

(defn ^:private add-post [conn {:keys [url title]}]
  (log/info "add-post")
  ; Check if the post exists, if it doesn't add it.
  (let [post-id (-> (d/q '[:find ?e
                           :in $ ?url
                           :where [?e :post/url ?url]] (d/db conn) url)
                    ffirst)]
    (if (nil? post-id)
      (do
        (honeycomb/send-metrics {"db-operation" "add-post"
                                 "post-id" post-id
                                 "post-url" url
                                 "post-title" title})
        (-> (transact-and-log conn [{:post/url url
                                     :post/title title}])
            :tempids first second))
      post-id)))

(defn ^:private vote-difference [old new]
  (if (nil? old)
    new
    (- new old)))

;; TODO: THIS REALLY SHOULD BE A TRANSACTION FUNCTION
(defn ^:private add-comment [conn post-id post-title {:keys [username comment timestamp upvotes downvotes]}]
  (log/info "add-comment")
  (log/info "post-id" post-id)
  (log/info "comment" comment)
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
      (transact-and-log conn [{:db/id "comment"
                               :comment/user user-id
                               :comment/text comment
                               :comment/time timestamp
                               :comment/upvotes upvotes
                               :comment/downvotes downvotes}
                              {:db/id post-id
                               :post/comment "comment"}])
      (transact-and-log conn [{:db/id comment-id
                               :comment/upvotes upvotes
                               :comment/downvotes downvotes}]))
    (transact-and-log conn [{:db/id user-id
                             :user/upvotes (+ user-upvotes upvote-increase)
                             :user/downvotes (+ user-downvotes downvote-increase)}])
    (honeycomb/send-metrics {"db-operation" "add-comment"
                             "post-id" post-id
                             "post-title" post-title
                             "user-id" user-id
                             "user-upvotes" user-upvotes
                             "user-downvotes" user-downvotes
                             "comment-id" comment-id
                             "comment-upvotes" comment-upvotes
                             "comment-downvotes" comment-downvotes})))

(defn ^:private add-comments [conn post-id post-title comments]
  (log/info "add-comments")
  (log/info "post-id:" post-id)
  (log/info "post-title" post-title)
  (log/info "comments:" comments)
  (doseq [comment comments]
    (add-comment conn post-id post-title comment)))

(defn update-stats [posts]
  (log/info "update-stats")
  (doseq [post posts]
    (let [post-id (add-post conn post)]
      (add-comments conn post-id (:title post) (:comments post)))))


; -----------------------------------------------------------------------------------
; Queries ---------------------------------------------------------------------------
; -----------------------------------------------------------------------------------


(defn db-since-days-ago
  "Given a number of days, returns a db from that many days ago.
  If given a number <= 0, return the current state of the db."
  [days]
  (if (<= days 0) (d/db conn)
      (let [midnight (-> (java.time.LocalDateTime/now)
                         (.minusHours 5)
                         (.minusDays days)
                         (.atZone (java.time.ZoneId/of "America/Toronto"))
                         (.toInstant)
                         (java.util.Date/from))]
        (d/since (d/db conn) midnight))))


; -----------------------------------------------------------------------------------
; Comments --------------------------------------------------------------------------
; -----------------------------------------------------------------------------------

; Sorting


(defn sort-by-upvotes
  [comments]
  (sort #(> (:upvotes %1) (:upvotes %2)) comments))

(defn sort-by-downvotes
  [comments]
  (sort #(> (:downvotes %1) (:downvotes %2)) comments))

(defn sort-by-score
  [comments]
  (sort #(> (+ (/ (:upvotes %1) 2) (:downvotes %1))
            (+ (/ (:upvotes %2) 2) (:downvotes %2)))
        comments))

(defn sort-by-specified
  [comments sort-type]
  (case sort-type
    "score" (sort-by-score comments)
    "downvotes" (sort-by-downvotes comments)
    "upvotes" (sort-by-upvotes comments)
    (sort-by-score comments)))

; Pagination
(defn paginate-comments
  ([amount comments]
   (paginate-comments 0 amount comments))
  ([offset amount comments]
   (->> (drop offset comments)
        (take amount))))

; Puts the raw query comment into a map
(defn create-comment-maps
  [comments]
  (for [comment comments]
    (apply assoc {}
           (interleave [:upvotes :downvotes :user :text :title :url] comment))))


; Below is how queries with optional conditions are created, taken from here: https://grishaev.me/en/datomic-query


(defn remap-query
  [{args :args :as m}]
  {:query (dissoc m :args)
   :args args})

; Gets literally every comment in the database.
(def initial-get-all-comments-query
  '{:find [?upvotes ?downvotes ?name ?text ?title ?url]
    :in [$]
    :args []
    :where [[?c :comment/text ?text]
            [?c :comment/upvotes ?upvotes]
            [?c :comment/downvotes ?downvotes]
            [?c :comment/user ?u]
            [?u :user/name ?name]
            [?p :post/comment ?c]
            [?p :post/title ?title]
            [?p :post/url ?url]]})

; Adds any optional args/conditionals to the query
(defn create-get-comments-query
  [db search-text name]
  (cond-> initial-get-all-comments-query
    true
    (update :args conj db)

    search-text
    (-> (update :in conj '?search-text)
        (update :args conj search-text)
        (update :where conj '[(.contains ^String ?text ?search-text)]))

    name
    (-> (update :in conj '?name)
        (update :args conj name))

    true
    remap-query))

(defn get-comments
  ([db search-text name]
   (let [query-map (create-get-comments-query db search-text name)]
     (-> (apply (partial d/q (:query query-map)) (:args query-map))
         (create-comment-maps))))
  ([db]
   (get-comments db nil nil)))

(defn get-top-x-comments
  [offset num sort-type days-ago search-text name]
  (let [db (db-since-days-ago days-ago)
        comments (get-comments db search-text name)
        sorted-comments (sort-by-specified comments sort-type)]
    (paginate-comments offset num sorted-comments)))

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

(defn get-top-rated-users
  ([num db]
   (let [users (d/q '[:find ?upvotes ?downvotes ?name :in $ :where
                      [?u :user/name ?name]
                      [?u :user/upvotes ?upvotes]
                      [?u :user/downvotes ?downvotes]] db)
         sorted-users (sort #(> (+ (first %1) (second %1)) (+ (first %2) (second %2))) users)
         top-x (take num sorted-users)]
     (for [user top-x]
       (apply assoc {}
              (interleave [:upvotes :downvotes :name] user)))))
  ([num] (get-top-rated-users num (d/db conn))))

(defn get-most-negative-users
  [num]
  (get-most-x-users :user/downvotes num))

(defn get-most-positive-users
  [num]
  (get-most-x-users :user/upvotes num))


;; Stats


(defn get-todays-stats []
  (let [comments (get-comments (db-since-days-ago 1))
        comment-count (count comments)
        upvote-count (reduce #(+ %1 (:upvotes %2)) 0 comments)
        downvote-count (reduce #(+ %1 (:downvotes %2)) 0 comments)
        article-count (-> (map :title comments)
                          distinct
                          count)]
    {:comment-count comment-count
     :upvote-count upvote-count
     :downvote-count downvote-count
     :article-count article-count}))
