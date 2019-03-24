(ns salttoday.common)

(defn display-comment [comment]
  (let [upvotes (get comment "upvotes")
        downvotes (get comment "downvotes")
        vote-total (max 1 (+ upvotes downvotes))
        pos-percent (if (zero? vote-total)
                      50
                      (* 100.0 (/ upvotes vote-total)))
        pos-gradient (if (= pos-percent 100)
                       100
                       (max 0 (- pos-percent 2.5)))
        neg-gradient (min 100 (+ pos-gradient 5))]

    [:div.row
     ; Title
     [:div.article-title
      [:a.article-link {:href (get comment "url") :target "_blank"}
       (get comment "title")]]
     [:div.row.comment-metadata-row



      ; Comment Body / Link to Article


      [:div.column.comment-body {:style {:flex 70 :border-image (str "linear-gradient(90deg, #0072bc " pos-gradient "%, #ed1c24 " neg-gradient "%) 2 / 4px")}}

       ; Likes
       [:span.counter.like-counter
        [:span.fa-stack.fa-1x.counter-icon
         [:i.fas.fa-thumbs-up.fa-stack-2x]
         [:i.fas.fa-stack-1x.vote-counter-text (str upvotes " ")]]]

       ; Comment text
       [:span.comment-text (get comment "text")]
       ; Dislikes
       [:span.counter.dislike-counter
        [:span.fa-stack.fa-1x.counter-icon
         [:i.fas.fa-thumbs-down.fa-stack-2x]
         [:i.fas.fa-stack-1x.vote-counter-text (str downvotes " ")]]]]]
     ; Author Information


     [:div.row
      ; Empty Offset
      [:div.column.comment-author {:style {:flex 70}}
       [:a.author-link {:href "/#/user"} "- "
        (get comment "user")]]
      ; Empty Offset
      [:div.column {:style {:flex 15}}]]]))