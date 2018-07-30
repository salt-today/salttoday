(ns salttoday.pages.user-leaderboard
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]))

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
   [:div.user-votes
    (get user "votes")]])

(defn users-page
  []
  [:div.container
   [:h3 "Most Liked Users"]
   (into [:div.users-list]
         (for [user (get @state "positive-users")]
           (display-user user)))
   [:h3 "Most Disliked Users"]
   (into [:div.users-list]
         (for [user (get @state "negative-users")]
           (display-user user)))])