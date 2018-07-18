(ns salttoday.pages.home
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [jumbotron content]]
            [salttoday.common :refer [make-layout]]))

(def state
  (r/atom {}))

(defn top-comments-handler [response]
  (js/console.log response)
  (reset! state response))

(GET "/top-comments"
     {:headers {"Accept" "application/transit"}
      :handler top-comments-handler})

(defn home-page []
  (make-layout
    [:div
     [:img.title-image {:src "/img/soo-salty.png"}]
     [:div.container.background-container
      [:div
       [:div.comments-type-header.container
        [:div
         [:div.liked-heading "Liked"][:div.line-break-positive]
         [:div (display-comment (get @state "daily-positive") "positive")]]]

       [:div.comments-type-header.container
        [:div
         [:div.disliked-heading "Disliked"][:div.line-break-negative]
         [:div (display-comment (get @state "daily-negative") "negative")]]]
       ]
      ]

     [:div.container.background-container
      [:div
       [:div.general-heading "All Time"]
       [:div.panel-body
        [:div.comments-type-header.container
          [:div.liked-heading "Liked"][:div.line-break-positive]
           (for [comment (get @state "all-time-positives")]
             (display-comment comment "positive"))]
        [:div.comments-type-header.container
          [:div.disliked-heading "Disliked"][:div.line-break-negative]
           (for [comment (get @state "all-time-negatives")]
             (display-comment comment "negative"))]]]]
     ]))