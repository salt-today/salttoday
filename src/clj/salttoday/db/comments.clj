(ns salttoday.db.comments
  (:require [datomic.api :as d]
            [clojure.set :as set]
            [salttoday.db.util :refer [remap-query sort-by-specified get-date paginate-results tx-with-logging]]
            [salttoday.db.connection :refer [conn]]
            [salttoday.db.users :refer [add-or-get-user]]
            [salttoday.metrics.core :as honeycomb]
            [clojure.tools.logging :as log]))

(defn vote-delta
  "Determines the delta between the comments upvote/downvote stored and live state"
  [stored live]
  (if (nil? stored)
    live
    (- live stored)))

; Puts the raw query comment into a map
(defn db-comments->frontend-comments
  "Given a collection of comments from the database, rename their keys to that which the frontend expects"
  [comments]
  (for [comment comments]
    (-> (apply merge comment)
        ;; TODO This is currently done as it's what the frontend expects, update the frontend.
        (set/rename-keys {:db/id :comment-id
                          :comment/upvotes :upvotes
                          :comment/downvotes :downvotes
                          :comment/text :text
                          :user/name :user
                          :post/title :title
                          :post/url :url}))))

;; TODO: THIS REALLY SHOULD BE A TRANSACTION FUNCTION
(defn add-comment
  "Given a comments payload, transact it to the database"
  [conn post-id post-title {:keys [username text timestamp upvotes downvotes]}]
  (let [user-id (add-or-get-user conn username)
        user-stats (-> (d/q '[:find ?upvotes ?downvotes :in $ ?user-id :where
                              [?user-id :user/upvotes ?upvotes]
                              [?user-id :user/downvotes ?downvotes]]
                            (d/db conn)
                            user-id)
                       first)
        user-upvotes (first user-stats)
        user-downvotes (second user-stats)

        ;; TODO - I think we want to avoid this or, nil would be better
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

        upvote-increase (vote-delta comment-upvotes upvotes)
        downvote-increase (vote-delta comment-downvotes downvotes)]
    (if (zero? comment-id)
      (tx-with-logging conn [{:db/id "comment"
                              :comment/user user-id
                              :comment/text text
                              :comment/time timestamp
                              :comment/upvotes upvotes
                              :comment/downvotes downvotes}
                             {:db/id post-id
                              :post/comment "comment"}])
      (tx-with-logging conn [{:db/id comment-id
                              :comment/upvotes upvotes
                              :comment/downvotes downvotes}]))
    (tx-with-logging conn [{:db/id user-id
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

(defn add-comments
  "Given a collection of comments for a particular article, transact them to the database"
  [conn post-id post-title comments]
  (log/debug (format "Adding Comments from an Article - [post-id: %d] - [post-title: %s]", post-id, post-title))
  ; Determine if comments were deleted by sootoday, and mark them as such
  (let [db-comments-result (d/q '[:find (pull ?c [:db/id :comment/text :comment/time])
                                  (pull ?u [:user/name])
                                  :in $ ?pid
                                  :where [?pid :post/comment ?c]
                                  [?c :comment/user ?u]] (d/db conn) post-id)
        db-comments (for [comment db-comments-result] (apply merge comment))
        ; Create a map as such {["comment text" "username"] comment-id}
        db-text-name-vec-to-id-map (reduce #(assoc %1 [(:comment/text %2) (:user/name %2)] (:db/id %2)) {} db-comments)
        db-comments-set (set (keys db-text-name-vec-to-id-map)) ; get the keys as a set so we can do a diff with the new comments
        scraped-comments-set (set (for [comment comments] [(:text comment) (:username comment)])) ; create a set of text and usernames
        comment-difference (set/difference db-comments-set scraped-comments-set)] ; find which comments are in the db but not scraped
    (if (pos? (count comment-difference))                   ; if we found comments we didn't scrape, transact them
      (doseq [text-name-vec comment-difference]
        (tx-with-logging conn [{:db/id (get db-text-name-vec-to-id-map text-name-vec)
                                :comment/deleted true}]))))
  (doseq [comment comments]
    (add-comment conn post-id post-title comment)))

(def initial-get-all-comments-query
  "Gets every comment in the database"
  '{:find [(pull ?c [:db/id :comment/upvotes :comment/downvotes :comment/text :comment/deleted])
           (pull ?u [:user/name])
           (pull ?p [:post/title :post/url])]
    :in [$]
    :args []
    :where [[?c :comment/text ?text ?tx]
            [?c :comment/user ?u]
            [?p :post/comment ?c]]})

; TODO - probably better if this function took a map of keys instead of positional args
(defn create-get-comments-query
  "Adds any optional args/conditionals to the query"
  [db days-ago-date search-text name cid deleted]
  (cond-> initial-get-all-comments-query
    true
    (update :args conj db)

    (not= 0 cid)
    (-> (update :in conj '?c)
        (update :args conj cid))

    search-text
    (-> (update :in conj '?search-text)
        (update :args conj search-text)
        (update :where conj '[(.contains ^String ?text ?search-text)]))

    name
    (-> (update :in conj '?name)
        (update :args conj name)
        (update :where conj '[?u :user/name ?name]))

    deleted
    (-> (update :where conj '[?c :comment/deleted true]))

    days-ago-date
    (-> (update :in conj '?days-ago-date)
        (update :args conj days-ago-date)
        (update :where conj '[?tx :db/txInstant ?inst])
        (update :where conj '[(.before ^java.util.Date ?days-ago-date ?inst)]))

    true
    remap-query))

(defn get-comments
  "Retrieve comments from the database."
  ([db days-ago search-text name id deleted]
   (let [days-ago-date (get-date days-ago)
         query-map (create-get-comments-query db days-ago-date search-text name id deleted)]
     (let [results (apply (partial d/q (:query query-map)) (:args query-map))]
       (db-comments->frontend-comments results))))
  ([db]
   (get-comments db -1 nil nil nil false)))

; TODO - we must pass in a keyword for sorting!!!
(defn get-comments-paginated
  "Get comments paginated"
  [offset num sort-type days-ago search-text name id deleted]
  (let [comments (get-comments (d/db conn) days-ago search-text name id deleted)
        sorted-comments (sort-by-specified sort-type comments)]
    (paginate-results offset num sorted-comments)))