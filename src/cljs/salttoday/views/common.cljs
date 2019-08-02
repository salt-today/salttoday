(ns salttoday.views.common
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [salttoday.routing.util :refer [update-query-parameters!]]
            [cljsjs.react-select]
            [cljs-http.client :as http]
            [clojure.core.async :as a]))

(defn jumbotron
  "Big name at the top?"
  [title]
  [:div {:class "jumbotron text-center"}
   [:h1 title]])

(defn content [x]
  [:div {:class "container"}
   x])

(defn nav-link [uri title current-page page icon]
  [:span.nav-link {:class (when (= page current-page) "active")}
   [:a {:href uri} title icon]])

(defn make-navbar
  [current-page]
  [:div.column {:style {:flex 15}}
   [:div.nav-row.navigation
    [nav-link "/home" "Home " current-page :home [:i.fas.fa-home]]]
   [:div.nav-row.navigation
    [nav-link "/users" "Users " current-page :users [:i.fas.fa-users]]]
   [:div.nav-row.navigation
    [nav-link "/about" "About " current-page :about [:i.fas.fa-question-circle]]]])

(defn make-content
  [current-page content]
  [:div.column {:style {:flex 70}}
   [:div.column.justify-center.main-content {:style {:flex-grow 1}}
    content]
   [:div.row.justify-center
    [:img.footer-pile {:src "/img/footer-pile.png"}]]])

(defn make-right-offset []
  [:div.column.right-offset {:style {:flex 15 :overflow "hidden"}}])

(defn get-selected-value
  [selected]
  (some-> selected .-value))

(defn update-query-params-with-state
  "Updates the query parameters with a state, except for the exclusions."
  [state & exclusions]
  (update-query-parameters! (apply dissoc @state exclusions)))

(defn create-select-options
  "Creates the options for a select dropdown"
  [options-map]
  (for [[value label] options-map] {:label label :value value}))

; https://gist.github.com/pesterhazy/4a4198a9cc040bf6fe13a476f25bac2c
(defn select
  [{:keys [state] :as props}]
  [:> js/Select
   (-> props
       (dissoc state)
       (assoc :class-name "sort-dropdown"
              :class-name-prefix "sort-dropdown"))])

(def days-options
  {"1" "Today"
   "7" "Past Week"
   "30" "Past Month"
   "365" "Past Year"
   "0" "All Time"})

(defn filter-by-days
  [selected state action]
  (let [days (get-selected-value selected)]
    (swap! state assoc :days days)
    (action)))

(defn days-dropdown
  "Creates a dropdown that sets the days in state and performs an action on change."
  [state action]
  [select {:state state
           :multi false
           :is-searchable false
           :value {:label (days-options (:days @state)) :value (:days @state)}
           :options (create-select-options days-options)
           :on-change (fn [selected]
                        (filter-by-days selected state action)
                        (update-query-params-with-state state :comments :users))}])

(def sort-options
  {"score" "Top"
   "upvotes" "Likes"
   "downvotes" "Dislikes"})

(defn filter-by-sort
  [selected state action]
  (let [sort (get-selected-value selected)]
    (swap! state assoc :sort-type sort)
    (action)))

(defn sort-dropdown
  "Creates a dropdown that sets how the comments are sorted in the state and performs an action on change."
  [state action]
  [select {:state state
           :multi false
           :is-searchable false
           :value {:label (sort-options (:sort-type @state)) :value (:sort-type @state)}
           :options (create-select-options sort-options)
           :on-change (fn [selected]
                        (filter-by-sort selected state action)
                        (update-query-params-with-state state :comments :users))}])

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