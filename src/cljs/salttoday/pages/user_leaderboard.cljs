(ns salttoday.pages.user-leaderboard
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [make-layout]]))

(def state
  (r/atom {}))

(defn top-users-handler
  [response]
  (js/console.log response)
  (reset! state response))

(GET "/top-users"
  {:headers {"Accept" "application/transit"}
   :handler top-users-handler})

(defn display-user
  [user]
  [:div.user-container
   [:div.user-name
    (get user "name")]
   [:div.user-vote-container
    [:div.user-votes.liked
     (get user "upvotes")
     [:i.fas.fa-thumbs-up]]
    [:div.user-votes.disliked
     [:i.fas.fa-thumbs-down.fa-flip-horizontal]
     (get user "downvotes")]]])

(defn users-page
  []
  (make-layout :users
               [:div.container
                [:div.general-heading "Top Voted Users"]  [:div.general-line-break]
                (into [:div.users-list]
                      (for [user (get @state "users")]
                        (display-user user)))]))
