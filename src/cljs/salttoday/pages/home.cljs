(ns salttoday.pages.home
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [jumbotron content]]))

(def state
  (r/atom {}))

(defn top-comments-handler [response]
  (js/console.log response)
  (reset! state response))

(GET "/top-comments"
  {:headers {"Accept" "application/transit"}
   :handler top-comments-handler})

(defn home-page []
  [:div
   (jumbotron "Top Comments")
   [:div.container.background-container
    [:div.panel.panel-default
     [:div.comments-time-header.panel-heading "Today"]
     [:div.panel-body
      [:div.comments-type-header.container
       [:div.panel.panel-default
        [:div.panel-heading "Liked"]
        [:div.panel-body.comment-background (display-comment (get @state "daily-positive"))]]]
      [:div.comments-type-header.container
       [:div.panel.panel-default
        [:div.panel-heading "Disliked"]
        [:div.panel-body.comment-background (display-comment (get @state "daily-negative"))]]]]]]

   [:div.container.background-container
    [:div.panel.panel-default
     [:div.comments-time-header.panel-heading "All Time"]
     [:div.panel-body
      [:div.comments-type-header.container
       [:div.panel.panel-default
        [:div.panel-heading "Liked"]
        [:div.panel-body.comment-background
         (for [comment (get @state "all-time-positives")]
           (display-comment comment))]]]
      [:div.comments-type-header.container
       [:div.panel.panel-default
        [:div.panel-heading "Disliked"]
        [:div.panel-body.comment-background
         (for [comment (get @state "all-time-negatives")]
           (display-comment comment))]]]]]]])