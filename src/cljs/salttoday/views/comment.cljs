(ns salttoday.views.comment
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.core.async :as a]
            [reagent.core :as r]
            [salttoday.components.comment :refer [comment-component]]
            [salttoday.views.common :refer [content get-selected-value jumbotron make-content make-navbar
                                            make-right-offset update-query-params-with-state]]))

(defn get-comment [state]
  (go (let [options {:query-params {:amount    1
                                    :id        (:id @state)}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/comments" options))]
        (swap! state assoc :comments body))))

(defn component-content [state]
  (list [:div.column.justify-center.comments-wrapper
         (for [comment (:comments @state)]
           (comment-component comment false))]))

; Helpful Docs - https://purelyfunctional.tv/guide/reagent/#form-2
(defn comment-page [query-params]
  (let [state (r/atom {:comments []
                       :id (or (:id query-params) nil)})]
    (get-comment state)
    (fn []
      [:div.page-wrapper
       (make-navbar :home)
       (make-content :home (component-content state))
       (make-right-offset)])))
