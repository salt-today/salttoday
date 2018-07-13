(ns salttoday.common)

(defn display-comment [comment]
  [:div.comment-container
   [:div.comment-text
    [:a {:href (get comment "url")}
     (get comment "text")]
    [:div.comment-info
     [:a {:href "/#/user"}
      (get comment "user")]
     "\t"
     (get comment "votes")]]])