(ns salttoday.db.posts
  (:require [datomic.api :as d]
            [salttoday.db.connection :refer [conn]]
            [salttoday.db.comments :refer [add-comments]]
            [salttoday.db.util :refer [tx-with-logging]]
            [salttoday.metrics.core :as honeycomb]))

; TODO - we should make the post url unique in the schema so we can do upserts
; https://docs.datomic.com/cloud/transactions/transaction-processing.html#unique-identities
(defn add-post
  "Adds a post if it does not exist"
  [conn {:keys [url title]}]
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
        (-> (tx-with-logging conn [{:post/url url
                                    :post/title title}])
            :tempids first second))
      post-id)))

(defn update-posts-and-comments
  "Given a collection of posts, add them and their respective comments"
  [posts]
  (doseq [post posts]
    (let [post-id (add-post conn post)]
      (add-comments conn post-id (:title post) (:comments post)))))