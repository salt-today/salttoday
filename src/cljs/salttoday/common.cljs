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

    [:div.comment-container
     [:div.votes.liked upvotes [:i.fas.fa-thumbs-up]]

     [:div.comment-text-and-name
       [:div.comment-text-border
        {:style {"background" (str "linear-gradient(to right, #0072bc " pos-gradient "%, #ed1c24 " neg-gradient "%)")}}
        [:div.comment-text
         [:a {:href (get comment "url")}
          (get comment "text")]]
        ]
      [:div.comment-user
       [:a {:href "/#/user"} "- "
        (get comment "user")]]]
     [:div.votes.disliked
      [:i.fas.fa-thumbs-down] downvotes
      ]
     ]))