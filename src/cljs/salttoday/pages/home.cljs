(ns salttoday.pages.home
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [content make-layout jumbotron]]))

(def state
  (r/atom {}))

(defn top-comments-handler [response]
  (js/console.log response)
  (reset! state response))

(GET "/top-comments"
  {:headers {"Accept" "application/transit"}
   :handler top-comments-handler})

(defn home-page []
  (make-layout :home
               [:div
                [:div
                 [:div [:div.general-heading "Today"] [:div.general-line-break]
                  [:div.comments-type-header.container
                   (for [comment (get @state "daily")]
                     (display-comment comment))]]]

                [:div
                 [:div
                  [:div.general-heading "All Time"] [:div.general-line-break]
                  [:div.panel-body
                   [:div.comments-type-header.container
                    (for [comment (get @state "all-time")]
                      (display-comment comment))]]]]]))