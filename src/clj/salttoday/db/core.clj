(ns salttoday.db.core
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [salttoday.config :refer [env]]
            [salttoday.metrics.core :as honeycomb]
            [salttoday.scraper :as scraper]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.set :refer [rename-keys]]))

(defn create-schema [conn]
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

(defstate conn
  :start (let [db-url (:database-url env)
               db (d/create-database db-url)
               conn (d/connect db-url)]
           (create-schema conn)
           conn)
  :stop (-> conn .release))

(defn ^:private transact-and-log [& args]
  (log/debug "transaction arguments:" args)
  (let [result @(apply d/transact args)]
    (log/debug "transaction result:" result)
    result))

(defn ^:private add-or-get-user [conn username]
  (log/debug "add-or-get-user")
  (let [user (-> (d/q '[:find ?e
                        :in $ ?name
                        :where [?e :user/name ?name]] (d/db conn) username)
                 ffirst)]
    (log/debug "was user found?" user)
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
  (log/debug "add-post")
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
  (log/debug "add-comment")
  (log/debug "post-id" post-id)
  (log/debug "comment" comment)
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
  (log/debug "add-comments")
  (log/debug "post-id:" post-id)
  (log/debug "post-title" post-title)
  (log/debug "comments:" comments)
  (doseq [comment comments]
    (add-comment conn post-id post-title comment)))

(defn update-stats [posts]
  (log/debug "update-stats")
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
(defn paginate-results
  [offset amount results]
  (->> (drop offset results)
       (take amount)))

; Puts the raw query comment into a map
(defn create-comment-maps
  [comments]
  (for [comment comments]
    (-> (apply merge comment)
        ;; TODO This is currently done as it's what the frontend expects, update the frontend.
        (clojure.set/rename-keys {:db/id :comment-id
                                  :comment/upvotes :upvotes
                                  :comment/downvotes :downvotes
                                  :comment/text :text
                                  :user/name :user
                                  :post/title :title
                                  :post/url :url}))))


; Below is how queries with optional conditions are created, taken from here: https://grishaev.me/en/datomic-query
(defn remap-query
  [{args :args :as m}]
  {:query (dissoc m :args)
   :args args})

; Gets literally every comment in the database.
(def initial-get-all-comments-query
  '{:find [(pull ?c [:db/id :comment/upvotes :comment/downvotes :comment/text])
           (pull ?u [:user/name])
           (pull ?p [:post/title :post/url])]
    :in [$]
    :args []
    :where [[?c :comment/text ?text ?tx]
            [?c :comment/user ?u]
            [?p :post/comment ?c]]})

(def initial-get-specific-comment-query
  '{:find [?upvotes ?downvotes ?text ?user-name ?title ?url]
    :in [$]
    :args []
    :where [[?cid :comment/upvotes ?upvotes]
            [?cid :comment/downvotes ?downvotes]
            [?cid :comment/text ?text]
            [?cid :comment/user ?u]
            [?u :user/name ?user-name]
            [?p :post/comment ?cid]
            [?p :post/title ?title]
            [?p :post/url ?url]]})

; Adds any optional args/conditionals to the query
(defn create-get-comments-query
  ; TODO - probably better if this function took a map of keys instead of positional args
  [db days-ago-date search-text name cid]
  (let [initial-query (if (not= 0 cid)
                        initial-get-specific-comment-query
                        initial-get-all-comments-query)]
    (cond-> initial-query
      true
      (update :args conj db)

      cid
      (-> (update :in conj '?cid)
          (update :args conj cid))

      search-text
      (-> (update :in conj '?search-text)
          (update :args conj search-text)
          (update :where conj '[(.contains ^String ?text ?search-text)]))

      name
      (-> (update :in conj '?name)
          (update :args conj name)
          (update :where conj '[?u :user/name ?name]))

      days-ago-date
      (-> (update :in conj '?days-ago-date)
          (update :args conj days-ago-date)
          (update :where conj '[?tx :db/txInstant ?inst])
          (update :where conj '[(.before ^java.util.Date ?days-ago-date ?inst)]))

      true
      remap-query)))

; Returns a date time of the current date minus a number of days.
; If given a number less than 1, returns nil.
(defn get-date [days-ago]
  (if (<= 1 days-ago)
    (-> (java.time.LocalDateTime/now)
        (.minusHours 5)
        (.minusDays days-ago)
        (.atZone (java.time.ZoneId/of "America/Toronto"))
        (.toInstant)
        (java.util.Date/from))))

; (get-comments db 0 nil nil 17592186045650)

; TODO this should be able to return a single item, but later code depends on it being a list!
(defn single-comment-map [id results]
  (let [comment (first results)]
    (if (nil? comment)
      '()
      (list {:comment-id id
             :upvotes (get (first results) 0)
             :downvotes (get (first results) 1)
             :text (get (first results) 2)
             :user (get (first results) 3)
             :title (get (first results) 4)
             :url (get (first results) 5)}))))

(defn get-comments
  ([db days-ago search-text name id]
   (let [days-ago-date (get-date days-ago)
         query-map (create-get-comments-query db days-ago-date search-text name id)]
     (let [results (apply (partial d/q (:query query-map)) (:args query-map))]
       (if (not= id 0)
         (let [formatted (single-comment-map id results)]
           (println formatted)
           formatted)
         (create-comment-maps results)))))
  ([db]
   (get-comments db -1 nil nil)))

(defn get-top-x-comments
  [offset num sort-type days-ago search-text name id]
  (let [comments (get-comments (d/db conn) days-ago search-text name id)
        sorted-comments (sort-by-specified comments sort-type)]
    (paginate-results offset num sorted-comments)))

; -----------
; Users
; -----------
(defn get-user-stats
  [name]
  (d/q '[:find ?upvotes ?downvotes :in $ ?name :where
         [?u :user/name ?name]
         [?u :user/upvotes ?upvotes]
         [?u :user/downvotes ?downvotes]]
       (d/db conn) name))

(defn create-user-maps
  [users]
  (for [user users]
    (-> user first
        (rename-keys {:user/upvotes :upvotes :user/downvotes :downvotes :user/name :name}))))

(def get-all-users-query '[:find (pull ?u [:user/upvotes :user/downvotes :user/name])
                           :in $
                           :where [?u :user/name ?name]])

(defn extract-name-to-outer-map
  [user-maps]
  (apply merge
         (for [user-map user-maps]
           {(:name user-map) user-map})))

; Used as a default if user can't be found in past
(def empty-user-map {:upvotes 0 :downvotes 0})

(defn get-user-scores-for-time-range
  [db current-users-map days-ago-date]
  (let [past-users (d/q get-all-users-query (d/as-of db days-ago-date))
        past-users-maps (create-user-maps past-users)
        new-past-users-map (extract-name-to-outer-map past-users-maps)
        new-current-users-map (extract-name-to-outer-map current-users-map)]
    (for [user-key (keys new-current-users-map)]
      (let [current-user-map (get new-current-users-map user-key)
            past-user-map (get new-past-users-map user-key empty-user-map)]
        (assoc current-user-map
               :upvotes (- (:upvotes current-user-map) (:upvotes past-user-map))
               :downvotes (- (:downvotes current-user-map) (:downvotes past-user-map)))))))

(defn get-top-x-users
  ([db offset amount sort-type days-ago]
   (let [users (d/q get-all-users-query db)
         user-maps (create-user-maps users)
         days-ago-date (get-date days-ago)
         user-maps (if days-ago-date (get-user-scores-for-time-range db user-maps days-ago) user-maps)
         sorted-users (sort-by-specified user-maps sort-type)]
     (paginate-results offset amount sorted-users)))
  ([offset amount sort-type days-ago]
   (get-top-x-users (d/db conn) offset amount sort-type days-ago)))

;; Stats


(defn get-todays-stats []
  (let [comments (get-comments (d/db conn))
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
