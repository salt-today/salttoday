(ns salttoday.pages.about
  (:require [reagent.core :as r]
            [goog.string :as gstring]
            [salttoday.pages.common :refer [content make-content make-navbar make-right-offset jumbotron]]))

(def state
  (r/atom {}))

(defn top-users-handler
  [response]
  (js/console.log response)
  (reset! state {:name (-> response (get "users") first (get "name"))}))

(defn get-users []
  [])
  ; (if (empty? @state)
  ;   (GET "/top-users"
  ;     {:headers {"Accept" "application/transit"}
  ;      :handler top-users-handler})))

(defn about-content []
  (list [:div.row.justify-center.header-wrapper
         [:span.heading "SALT TODAY"]]
        [:div.row.justify-center.header-wrapper
         [:span.heading.small-heading "Definition"]]

        [:div.definition
         [:div {:style {:float "left"}}
          [:div.definition-title "salt·y"]
          [:div.definition-title "/ˈsôltē/"]]
         [:div.definition-text {:style {:float "right"}}
          [:a.about-link {:href "https://www.urbandictionary.com/define.php?term=salty"}
           "Being salty is when you are upset over something little."]
          [:div [:i  (:name @state) " was so salty after reading a SooToday article."]]]]

        [:br] [:br]
        [:div.row.justify-center.header-wrapper
         [:span.heading.small-heading "What is this?"]]
        [:div.about-text "A website that ranks both the comments and users on " [:a.about-link {:href "https://sootoday.com"} "SooToday"] "." (gstring/unescapeEntities "&nbsp;&nbsp;")
         "The ranking of both comments and users is based on the number of likes and dislikes they've accumulated. Likes count for one point, dislikes count for two."]

        [:br] [:br]
        [:div.row.justify-center.header-wrapper
         [:span.heading.small-heading "My comment is missing!"]]
        [:div.about-text "It happens. Bear with me, theres a few kinks that need to be worked out yet."]))

(defn about-page []
  (get-users)
  [:div.page-wrapper
   (make-navbar :about)
   (make-content :about (about-content))
   (make-right-offset)])
