(ns salttoday.views.about
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [clojure.core.async :as a]
            [cljs-http.client :as http]
            [goog.string :as gstring]
            [salttoday.views.common :refer [content make-content make-navbar make-right-offset jumbotron]]))

(defn get-users [state]
  (go (let [options {:query-params {:offset    0
                                    :amount    1
                                    :sort-type "score"
                                    :days      0}
                     :with-credentials? false
                     :headers {}}
            {:keys [status headers body error] :as resp} (a/<! (http/get "/api/v1/users" options))]
        (swap! state assoc :name (-> body
                                     first
                                     (:name))))))

(defn about-content [state]
  (list [:div.row.justify-center.header-wrapper
         [:span.heading "SALT TODAY"]]
        [:div.row.justify-center.header-wrapper
         [:span.heading.small-heading.about-heading "Definition"]]

        [:div.definition
         [:div {:style {:float "left"}}
          [:div.definition-title "salt·y"]
          [:div.definition-title "/ˈsôltē/"]]
         [:div.definition-text {:style {:float "right"}}
          [:a.about-link {:href "https://www.urbandictionary.com/define.php?term=salty"}
           "Being salty is when you are upset over something little."]
          ; TODO - link to this user once supported
          [:div [:i  (:name @state) " was so salty after reading a SooToday article."]]]]

        [:div.row.justify-center.header-wrapper
         [:span.heading.small-heading.about-heading "What is this?"]]
        [:div.about-text "A website that ranks both the comments and users on " [:a.about-link {:href "https://sootoday.com"} "SooToday"] "." (gstring/unescapeEntities "&nbsp;&nbsp;")
         "The ranking of both comments and users is based on the number of likes and dislikes they've accumulated. Likes count for one point, dislikes count for two."]

        [:div.row.justify-center.header-wrapper
         [:span.heading.small-heading.about-heading "My comment is missing!"]]
        [:div.about-text "It happens. Bear with me, theres a few kinks that need to be worked out yet."]))

(defn about-page []
  (let [state (r/atom {})]
    (get-users state)
    (fn []
      [:div.page-wrapper
       (make-navbar :about)
       (make-content :about (about-content state))
       (make-right-offset)])))
