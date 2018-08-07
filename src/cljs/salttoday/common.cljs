(ns salttoday.common)

(defn display-comment [comment]
  (let [comment-type (if (> (get comment "upvotes") (get comment "downvotes"))
                       "positive"
                       "negative")]
    [:div.comment-container {:class comment-type}
     [:div.comment-text
      [:a {:href (get comment "url")}
       (get comment "text")]]
     [:div.comment-info
      [:a {:href "/#/user"} "- "
       (get comment "user")]
      "\t"
      (get comment "votes")]]))