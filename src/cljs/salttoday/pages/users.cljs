(ns salttoday.pages.users
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [clojure.core.async :as a]
            [cljs-http.client :as http]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [make-navbar make-content make-right-offset]]))

(defn get-users [state]
  (go (let [options {:query-params {}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/top-users" options))]
        (swap! state :users body))))

(defn display-user
  [user]
  [:div.row
   [:div.row.user-name-row
    [:span
     (:name user)]]
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
  (list [:div.row.justify-center.header-wrapper
         [:span.heading "Top Voted Users"]]
        [:div.column.justify-center.comments-wrapper
         (for [user (:users @state)]
           (display-user user))]))

(defn users-page []
  (let [state (r/atom {})]
    (get-users state)
    (fn []
      [:div.page-wrapper
       (make-navbar :users)
       (make-content :users (leaderboard-content state))
       (make-right-offset)])))
