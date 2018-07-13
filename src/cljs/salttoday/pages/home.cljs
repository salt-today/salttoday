(ns salttoday.pages.home
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [jumbotron content]]))

(def state
  (r/atom {}))

(defn top-comments-handler [response]
  (js/console.log response)
  (reset! state {:daily-positive (get response "daily-positive")
                 :daily-negative (get response "daily-negative")
                 :all-time-positives (get response "all-time-positives")
                 :all-time-negatives (get response "all-time-negatives")}))

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
        [:div.panel-body.comment-background (display-comment (:daily-positive @state))]]]
      [:div.comments-type-header.container
       [:div.panel.panel-default
        [:div.panel-heading "Disliked"]
        [:div.panel-body.comment-background (display-comment (:daily-negative @state))]]]
     ]]]

   [:div.container.background-container
   [:div.panel.panel-default
    [:div.comments-time-header.panel-heading "All Time"]
    [:div.panel-body
     [:div.comments-type-header.container
      [:div.panel.panel-default
       [:div.panel-heading "Liked"]
       [:div.panel-body.comment-background
        (for [comment (:all-time-positives @state)]
          (display-comment comment))]]]
     [:div.comments-type-header.container
      [:div.panel.panel-default
       [:div.panel-heading "Disliked"]
       [:div.panel-body.comment-background
        (for [comment (:all-time-positives @state)]
          (display-comment comment))]]]]]]])