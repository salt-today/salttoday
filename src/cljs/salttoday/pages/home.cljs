(ns salttoday.pages.home
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.core.async :as a]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [content get-selected-value make-content make-navbar make-right-offset jumbotron]]))

(defn get-comments [state]
  (go (let [options {:query-params {:offset    (:offset @state)
                                    :amount    (:amount @state)
                                    :sort-type (:sort-type @state)
                                    :days      (:days @state)}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/comments" options))]
        (swap! state assoc :comments body))))

(defn filter-by-days
  [event state]
  (let [sort (get-selected-value event state)]
    (swap! state assoc :days sort)
    (get-comments state)))

(defn filter-by-sort
  [event state]
  (let [sort (get-selected-value event state)]
    (swap! state assoc :sort-type sort)
    (get-comments state)))

(defn home-content [state]
  (list
   [:div.row.justify-center.header-wrapper.sort-bar
    [:div.column.sort-item.sort-dropdown
     [:select {:on-change (fn [e]
                            (filter-by-sort e state))}
      [:option {:value "score"} "Top"]
      [:option {:value "downvotes"} "Dislikes"]
      [:option {:value "upvotes"} "Likes"]]]
    [:div.column.sort-dropdown
     [:select {:on-change (fn [e]
                            (filter-by-days e state))}
      [:option {:value 1} "Past Day"]
      [:option {:value 7} "Past Week"]
      [:option {:value 30} "Past Month"]
      [:option {:value 365} "Past Year"]
      [:option {:value 0} "Of All Time"]]]]
   (list [:div.column.justify-center.comments-wrapper
          (for [comment (:comments @state)]
            (display-comment comment))])))

; Helpful Docs - https://purelyfunctional.tv/guide/reagent/#form-2
(defn home-page []
  (let [state (r/atom {:comments []
                       :offset 0
                       :amount 50
                       :sort-type "score"
                       :days "1"})]
    (get-comments state)
    (fn []
      [:div.page-wrapper
       (make-navbar :home)
       (make-content :home (home-content state))
       (make-right-offset)])))
