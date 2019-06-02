(ns salttoday.pages.home
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.core.async :as a]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [content make-content make-navbar make-right-offset jumbotron]]
            [salttoday.routing.util :refer [update-query-parameters!]]))

(defn get-comments [state]
  (go (let [options {:query-params {:offset    (:offset @state)
                                    :amount    (:amount @state)
                                    :sort-type (:sort-type @state)
                                    :days      (:days @state)}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/comments" options))]
        (swap! state assoc :comments body))))

(defn get-selected-value
  [event]
  (-> event .-target .-value))

(defn filter-by-sort
  [event state]
  (let [sort (get-selected-value event)]
    (swap! state assoc :sort-type sort)
    (get-comments state)))

(defn filter-by-days
  [event state]
  (let [sort (get-selected-value event)]
    (swap! state assoc :days sort)
    (get-comments state)))

(defn home-content [state]
  (list
   [:div.row.justify-center.header-wrapper.sort-bar
    [:div.column.sort-item.sort-dropdown
     [:select {:value [(:sort-type @state)]
               :on-change (fn [e]
                            (filter-by-sort e state)
                            (update-query-parameters! {:sort-type (get-selected-value e)}))}
      [:option {:value "score"} "Top"]
      [:option {:value "downvotes"} "Dislikes"]
      [:option {:value "upvotes"} "Likes"]]]
    [:div.column.sort-dropdown
     [:select {:value [(:days @state)]
               :on-change (fn [e]
                            (filter-by-days e state)
                            (update-query-parameters! {:days (get-selected-value e)}))}
      [:option {:value 1} "Past Day"]
      [:option {:value 7} "Past Week"]
      [:option {:value 30} "Past Month"]
      [:option {:value 365} "Past Year"]
      [:option {:value 0} "Of All Time"]]]]
   (list [:div.column.justify-center.comments-wrapper
          (for [comment (:comments @state)]
            (display-comment comment))])))

; Helpful Docs - https://purelyfunctional.tv/guide/reagent/#form-2
(defn home-page [query-params]
  (cljs.pprint/pprint query-params)
  (let [state (r/atom {:comments []
                       :offset 0
                       :amount 50
                       :sort-type (or (:sort-type query-params) "score")
                       :days (or (:days query-params) "1")})]
    (get-comments state)
    (fn []
      [:div.page-wrapper
       (make-navbar :home)
       (make-content :home (home-content state))
       (make-right-offset)])))
