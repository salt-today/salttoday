(ns salttoday.pages.home
  (:require [ajax.core :refer [GET PUT]]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [content make-content make-navbar make-right-offset jumbotron]]))

(def state
  (r/atom {:daily []
           :weekly []
           :all-time []}))

(defn comments-handler [days-range response]
  (js/console.log response)
  (swap! state assoc days-range response))

(defn get-comments []
  (if (empty? (:all-time @state))
    (do (GET "/comments" {:params {:offset    0
                                   :amount    3
                                   :sort-type "score"
                                   :days      1}
                          :headers {"Accept" "application/transit"}
                          :handler (partial comments-handler :daily)})
        (GET "/comments" {:params {:offset 0
                                   :amount 5
                                   :sort-type "score"
                                   :days 7}
                          :headers {"Accept" "application/transit"}
                          :handler (partial comments-handler :weekly)})
        (GET "/comments" {:params {:offset 0
                                   :amount 50
                                   :sort-type "score"}
                          :headers {"Accept" "application/transit"}
                          :handler (partial comments-handler :all-time)}))))

(defn home-content [snapshot]
  (list
        ; Temporary fix until Today functions properly
   (if (not (empty? (:daily snapshot)))
     (list [:div.row.justify-center.header-wrapper
            [:span.heading "Today"]]
           [:div.column.justify-center.comments-wrapper
            (for [comment (:daily snapshot)]
              (display-comment comment))]))
   (list [:div.row.justify-center.header-wrapper
          [:span.heading "Weekly"]]
         [:div.column.justify-center.comments-wrapper
          (for [comment (:weekly snapshot)]
            (display-comment comment))])
   [:div.row.justify-center.header-wrapper
    [:span.heading "All Time"]]
   [:div.column.justify-center.comments-wrapper
    (for [comment (:all-time snapshot)]
      (display-comment comment))]))

(defn home-page []
  (get-comments)
  [:div.page-wrapper
   (make-navbar :home)
   (make-content :home (home-content @state))
   (make-right-offset)])
