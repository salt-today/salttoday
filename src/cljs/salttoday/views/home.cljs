(ns salttoday.views.home
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.core.async :as a]
            [cljsjs.react-select]
            [reagent.core :as r]
            [salttoday.components.comment :refer [comment-component]]
            [salttoday.components.pagination :refer [pagination-component]]
            [salttoday.views.common :refer [content create-select-options days-dropdown get-comments get-selected-value jumbotron make-content make-navbar
                                            make-right-offset select sort-dropdown update-query-params-with-state]]))

; Get all of the users - this is needed for the users dropdown
(defn get-users [state]
  (go (let [options {:query-params {:amount 99999}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/users" options))]
        (swap! state assoc :users (sort (for [user body] (:name user)))))))

(def deleted-options
  {"false" "All"
   "true" "Deleted"})

(defn filter-by-deleted
  [selected state]
  (let [deleted (get-selected-value selected)]
    (swap! state assoc :deleted deleted)
    (get-comments state)))

(defn filter-by-user
  [user state]
  (do (swap! state assoc :user user)
      (get-comments state)))

(defn home-content [state]
  (list
   [:div.row.justify-center.header-wrapper.sort-bar
    [:div.column.sort-item.sort-dropdown
     (sort-dropdown state (partial get-comments state))]

     ; Days dropdown
    [:div.column.sort-item.sort-dropdown
     (days-dropdown state (partial get-comments state))]

     ; Deleted dropdown
    [:div.column.sort-item.sort-dropdown
     [select {:state state
              :multi false
              :is-searchable false
              :value {:label (deleted-options (:deleted @state)) :value (:deleted @state)}
              :options (create-select-options deleted-options)
              :on-change (fn [selected]
                           (filter-by-deleted selected state)
                           (update-query-params-with-state state :comments :users))}]]

     ; Users dropdown
    [:div.column.sort-item.sort-dropdown
     [select {:state state
              :multi false
              :options (conj
                        (for [user (:users @state)] {:label user :value user})
                        {:label "All Users" :value ""})
              :value {:label (if (clojure.string/blank? (:user @state)) "All Users" (:user @state))
                      :value (if (clojure.string/blank? (:user @state)) "" (:user @state))}
              :on-change (fn [selected] (let [user (get-selected-value selected)]
                                          (filter-by-user user state)
                                          (update-query-params-with-state state :comments :users)))}]]]

   (list [:div.column.justify-center.comments-wrapper
          (for [comment (:comments @state)]
            (comment-component comment state))])

   (let [num-comments (:total-comments @state)
         page-size (dec (:amount @state))
         page-buffer 5
         current-page (Math/ceil (/ (:offset @state) page-size))]
     (pagination-component state current-page num-comments page-size page-buffer))))

; Helpful Docs - https://purelyfunctional.tv/guide/reagent/#form-2
(defn home-page [query-params]
  (let [state (r/atom {:comments []
                       :total-comments 0
                       :lorem (= (:lorem query-params) "true")
                       :offset (or (:offset query-params) 0)
                       :amount (or (:amount query-params) 20)
                       :sort-type (or (:sort-type query-params) "score")
                       :days (or (:days query-params) "1")
                       :id nil
                       :deleted (or (:deleted query-params) "false")
                       :user (or (:user query-params) "")})]
    (get-comments state)
    (get-users state)
    (fn []
      [:div.page-wrapper
       (make-navbar :home)
       (make-content :home (home-content state))
       (make-right-offset)])))
