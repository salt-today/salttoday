(ns salttoday.common)
(defn make-layout
  [content]
  [:div.outer[:div.navigation [:div"Home "[:span.icon-home3]][:div"Users "[:span.icon-group]]]
   content
   [:img.footer-pile {:src "/img/footer-pile.png"}]])

(defn display-comment [comment comment-type]
  [:div.comment-container {:class comment-type}
   [:div.comment-text
    [:a {:href (get comment "url")}
     (get comment "text")]]
    [:div.comment-info
     [:a {:href "/#/user"} "- "
      (get comment "user")]
     "\t"
     (get comment "votes")]])