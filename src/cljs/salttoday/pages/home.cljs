(ns salttoday.pages.home
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [content make-content make-navbar make-right-offset jumbotron]]))

(def state
  (r/atom {:comments []
           :offset 0
           :amount 50
           :sort-type "score"
           :days "1"}))

(defn comments-handler [response]
  (js/console.log response)
  (swap! state assoc :comments response))

(defn get-comments []
  (GET "/comments" {:params {:offset    (:offset @state)
                             :amount    (:amount @state)
                             :sort-type (:sort-type @state)
                             :days      (:days @state)}
                    :headers {"Accept" "application/transit"}
                    :handler comments-handler}))

(get-comments)

(defn get-selected-value
  [event]
  (-> event .-target .-value))

(defn filter-by-sort
  [event]
  (let [sort (get-selected-value event)]
    (swap! state assoc :sort-type sort)
    (get-comments)))

(defn filter-by-days
  [event]
  (let [sort (get-selected-value event)]
    (swap! state assoc :days sort)
    (get-comments)))

(defn home-content [snapshot]
  (list
   [:div.row.justify-center.header-wrapper.sort-bar
    [:div.column.sort-item.sort-dropdown
     [:select {:on-change filter-by-sort}
      [:option {:value "score"} "Top"]
      [:option {:value "downvotes"} "Dislikes"]
      [:option {:value "upvotes"} "Likes"]]]
    [:div.column.sort-dropdown
     [:select {:on-change filter-by-days}
      [:option {:value 1} "Past Day"]
      [:option {:value 7} "Past Week"]
      [:option {:value 30} "Past Month"]
      [:option {:value 365} "Past Year"]
      [:option {:value 0} "Of All Time"]]]]

   (list [:div.column.justify-center.comments-wrapper
          (for [comment (:comments snapshot)]
            (display-comment comment))])))

(defn home-page []
  [:div.page-wrapper
   (make-navbar :home)
   (make-content :home (home-content @state))
   (make-right-offset)])
