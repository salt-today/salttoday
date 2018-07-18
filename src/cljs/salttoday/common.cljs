(ns salttoday.common
  (:require
            [salttoday.core :refer [nav-link]]))
(defn make-layout
  [content]
  [:div.outer[:div.navigation [nav-link "#/" "Home " :home "icon-home3"][nav-link "#/users" "Users " :users "icon-group"]]
   [:div [:img.title-image {:src "/img/soo-salty.png"}] content]
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