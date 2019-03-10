(ns salttoday.db.core
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [salttoday.config :refer [env]]
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
(defn ^:private add-post [conn {:keys [url title]}]
  ; Check if the post exists, if it doesn't add it.
  (let [post-id (-> (d/q '[:find ?e :in $ ?url :where [?e :post/url ?url]] (d/db conn) url)
                    ffirst)]
    (if (nil? post-id)
      (-> @(d/transact conn [{:post/url url
                              :post/title title}])
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

(defn update-stats [posts]
  (doseq [post posts]
    (let [post-id (add-post conn post)]
      (add-comments conn post-id (:comments post)))))


; -----------------------------------------------------------------------------------
; Queries ---------------------------------------------------------------------------
; -----------------------------------------------------------------------------------

; -----------------------------------------------------------------------------------
; Comments --------------------------------------------------------------------------
; -----------------------------------------------------------------------------------

; Sorting


(defn sort-by-upvotes
  [comments]
  (sort #(> (:comment/upvotes %1) (:comment/upvotes %2)) comments))

(defn sort-by-downvotes
  [comments]
  (sort #(> (:comment/downvotes %1) (:comment/downvotes %2)) comments))

(defn sort-by-score
  [comments]
  (sort #(> (+ (/ (:comment/upvotes %1) 2) (:comment/downvotes %1))
            (+ (/ (:comment/upvotes %2) 2) (:comment/downvotes %2)))
        comments))

; Pagination
(defn paginate-comments
  ([amount comments]
   (paginate-comments 0 amount comments))
  ([offset amount comments]
   (->> (drop offset comments)
        (take amount))))

; Gets literally every comment in the database.
(defn get-all-comments
  [db]
  (let [comments (d/q '[:find
                        (pull ?c [:comment/upvotes :comment/downvotes :comment/text])
                        (pull ?u [:user/name])
                        (pull ?p [:post/title :post/url])
                        :in $
                        :where [?p :post/comment ?c] [?c :comment/user ?u]] db)]
    (for [comment comments]
      (apply merge comment))))

(defn get-top-x-comments
  ([num db]
   (let [comments (get-all-comments db)
         sorted-comments (sort-by-score comments)]
     (paginate-comments num sorted-comments)))
  ([num]
   (get-top-x-comments num (d/db conn))))

(defn db-since-midnight []
  (let [midnight (-> (java.time.LocalDateTime/now)
                     (.toLocalDate)
                     (.atStartOfDay)
                     (.atZone (java.time.ZoneId/of "America/Toronto"))
                     (.toInstant)
                     (java.util.Date/from))]
    (d/since (d/db conn) midnight)))

(defn get-daily-comments
  [num]
  (get-top-x-comments num (db-since-midnight)))

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
  (let [comments (get-all-comments (db-since-midnight))
        comment-count (count comments)
        upvote-count (reduce #(+ %1 (:comment/upvotes %2)) 0 comments)
        downvote-count (reduce #(+ %1 (:comment/downvotes %2)) 0 comments)
        article-count (-> (map :title comments)
                          distinct
                          count)]
    {:comment-count comment-count
     :upvote-count upvote-count
     :downvote-count downvote-count
     :article-count article-count}))
