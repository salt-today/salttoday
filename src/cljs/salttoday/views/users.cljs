(ns salttoday.views.users
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [accountant.core :as accountant]
            [reagent.core :as r]
            [clojure.core.async :as a]
            [cljs-http.client :as http]
            [salttoday.components.comment :refer [comment-component]]
            [salttoday.views.common :refer [days-dropdown get-selected-value make-navbar make-content make-right-offset
                                            sort-dropdown update-query-params-with-state]]))

(defn get-users [state]
  (go (let [options {:query-params {:offset    (:offset @state)
                                    :amount    (:amount @state)
                                    :sort-type (:sort-type @state)
                                    :days (:days @state)}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/users" options))]
        (swap! state assoc :users body))))

(defn filter-by-sort
  [event state]
  (let [sort (get-selected-value event)]
    (swap! state assoc :sort-type sort)
    (get-users state)))

(defn filter-by-days
  [event state]
  (let [sort (get-selected-value event)]
    (swap! state assoc :days sort)
    (get-users state)))

(defn display-user
  [user]
  [:div.row
   [:div.row.user-name-row
    [:span
     [:a {:on-click (fn [] (accountant/navigate! (str "/home?user=" (:name user))))} (:name user)]]]
   [:div.row.user-stats-row
    [:span.positive
     (:upvotes user)
     " "
     [:i.fas.fa-thumbs-up]]
    [:span.negative
     (:downvotes user)
     " "
     [:i.fas.fa-thumbs-down]]]])

(defn leaderboard-content [state]
  (list [:div.row.justify-center.header-wrapper.sort-bar
         [:div.column.sort-item.sort-dropdown
          (sort-dropdown state (partial get-users state))]
        ; TODO - Hide for meantime, this doesn't work :(
        ; [:div.column.sort-item.sort-dropdown
        ;  (days-dropdown state (partial get-users state))]
         ]
        [:div.column.justify-center.comments-wrapper
         (for [user (:users @state)]
           (display-user user))]))

(defn users-page [query-params]
  (let [state (r/atom {:users []
                       :offset (or (:offset query-params) 0)
                       :amount (or (:amount query-params) 10)
                       :days (or (:days query-params) "0")
                       :sort-type (or (:sort-type query-params) "score")})]
    (get-users state)
    (fn []
      [:div.page-wrapper
       (make-navbar :users)
       (make-content :users (leaderboard-content state))
       (make-right-offset)])))
