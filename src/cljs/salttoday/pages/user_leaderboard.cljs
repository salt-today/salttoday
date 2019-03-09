(ns salttoday.pages.user-leaderboard
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [make-navbar make-content make-right-offset]]))

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
  [:div.row
   [:div.row.user-name-row
    [:span
     (get user "name")]]
   [:div.row.user-stats-row
    [:span.positive
     (get user "upvotes")
     " "
     [:i.fas.fa-thumbs-up]]
    [:span.negative
     (get user "downvotes")
     " "
     [:i.fas.fa-thumbs-down]]]])

(defn leaderboard-content [snapshot]
  (list [:div.row.justify-center.header-wrapper
         [:span.heading "Top Voted Users"]]
        [:div.column.justify-center.comments-wrapper
         (for [comment (get snapshot "users")]
           (display-user comment))]))

(defn users-page
  []
  [:div.page-wrapper
   (make-navbar :home)
   (make-content :home (leaderboard-content @state))
   (make-right-offset)])
