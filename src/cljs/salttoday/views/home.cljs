(ns salttoday.views.home
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.core.async :as a]
            [reagent.core :as r]
            [salttoday.components.comment :refer [comment-component]]
            [salttoday.views.common :refer [content get-selected-value jumbotron make-content make-navbar
                                            make-right-offset update-query-params-with-state]]))

(defn get-comments [state]
  (go (let [options {:query-params {:offset    (:offset @state)
                                    :amount    (:amount @state)
                                    :sort-type (:sort-type @state)
                                    :days      (:days @state)
                                    :id        (:id @state)
                                    :deleted   (:deleted @state)
                                    :user      (:user @state)}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/comments" options))]
        (swap! state assoc :comments body))))

; Get all of the users - this is needed for the users dropdown
(defn get-users [state]
  (go (let [options {:query-params {:amount    99999}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/users" options))]
        (swap! state assoc :users (for [user body] (:name user))))))

(defn filter-by-days
  [event state]
  (let [sort (get-selected-value event)]
    (swap! state assoc :days sort)
    (get-comments state)))

(defn filter-by-sort
  [event state]
  (let [sort (get-selected-value event)]
    (swap! state assoc :sort-type sort)
    (get-comments state)))

(defn filter-by-deleted
  [event state]
  (let [deleted (get-selected-value event)]
    (swap! state assoc :deleted deleted)
    (get-comments state)))

(defn filter-by-user
  [event state]
  (let [user (get-selected-value event)]
    (if (or (clojure.string/blank? user) (some #{user} (:users @state))) ; if the user is in the list, get them
      (do (swap! state assoc :user user)
          (get-comments state)))))

(defn home-content [state]
  (list
   [:div.row.justify-center.header-wrapper.sort-bar
    [:div.column.sort-item.sort-dropdown
     [:select {:value [(:sort-type @state)]
               :on-change (fn [e]
                            (filter-by-sort e state)
                            (update-query-params-with-state state :comments :users))}
      [:option {:value "score"} "Top"]
      [:option {:value "downvotes"} "Dislikes"]
      [:option {:value "upvotes"} "Likes"]]]
    [:div.column.sort-item.sort-dropdown
     [:select {:value [(:days @state)]
               :on-change (fn [e]
                            (filter-by-days e state)
                            (update-query-params-with-state state :comments :users))}
      [:option {:value 1} "Past Day"]
      [:option {:value 7} "Past Week"]
      [:option {:value 30} "Past Month"]
      [:option {:value 365} "Past Year"]
      [:option {:value 0} "Of All Time"]]]
    [:div.column.sort-item.sort-dropdown
     [:select {:value [(:deleted @state)]
               :on-change (fn [e]
                            (filter-by-deleted e state)
                            (update-query-params-with-state state :comments :users))}
      [:option {:value false} "All"]
      [:option {:value true} "Deleted"]]]
    [:div.column.sort-item.sort-textfield
     [:input {:list "usernames"
              :placeholder "All Users"
              :on-change (fn [e]
                           (filter-by-user e state)
                           (update-query-params-with-state state :comments :users))}]
     [:datalist {:id "usernames"}
      (for [user (:users @state)]
        [:option {:value user}])
      ]]]
   (list [:div.column.justify-center.comments-wrapper
          (for [comment (:comments @state)]
            (comment-component comment (:lorem @state)))])))

; Helpful Docs - https://purelyfunctional.tv/guide/reagent/#form-2
(defn home-page [query-params]
  (let [state (r/atom {:comments []
                       :lorem (= (:lorem query-params) "true")
                       :offset (or (:offset query-params) 0)
                       :amount (or (:amount query-params) 50)
                       :sort-type (or (:sort-type query-params) "score")
                       :days (or (:days query-params) 1)
                       :deleted (or (:deleted query-params) false)
                       :user (or (:user query-params) "")})]
    (get-comments state)
    (get-users state)
    (fn []
      [:div.page-wrapper
       (make-navbar :home)
       (make-content :home (home-content state))
       (make-right-offset)])))
