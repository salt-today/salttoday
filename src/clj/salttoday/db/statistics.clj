(ns salttoday.db.statistics
  (:require [datomic.api :as d]
            [salttoday.db.comments :refer [get-comments]]
            [salttoday.db.connection :refer [conn]]))

(defn get-todays-stats []
  "General statistics about today on SooToday"
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
