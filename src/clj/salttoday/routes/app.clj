(ns salttoday.routes.app
  (:require [salttoday.db.comments :refer [get-comments]]
            [salttoday.layout :as layout]
            [salttoday.routes.util :as routing-util]
            [compojure.core :refer [defroutes GET PUT]]))

(def default-opengraph-tags {:og-image "/img/logo/white-with-stroke.png"
                             :og-site-name "SaltToday"
                             :og-type "website"
                             :og-title "SaltToday.ca"
                             :og-url "https://www.salttoday.ca"
                             :og-description "A Leaderboard for SooToday's users and comments."
                             :twitter-card "summary"
                             :twitter-site "@SaltToday"
                             :twitter-title "SaltToday"
                             :twitter-description "A Leaderboard for SooToday's users and comments."
                             :twitter-image "/img/logo/white-with-stroke.png"})

(defn app-page
  ([]
   (layout/render "app.html" default-opengraph-tags))
  ([opengraph-tags]
   (layout/render "app.html" opengraph-tags)))

(defroutes app-routes
  (GET "/" []
    (app-page))
  (GET "/home" []
    (app-page))
  (GET "/users" []
    (app-page))
  (GET "/about" []
    (app-page))
  (GET "/comment" [id]
    ; TODO - end the passing of '0'
    (let [comment (first (get-comments 0 1 nil 0 nil nil (routing-util/string->number id) false))]
      (if (nil? comment)
        (app-page)
        (let [img-path (if (> (:upvotes comment) (:downvotes comment))
                         "/img/icons/upvote.png"
                         "/img/icons/downvote.png")
              title (format "%s's Comment on \"%s\"" (:user comment) (:title comment))
              description (:text comment)]
          (app-page (assoc default-opengraph-tags
                           :og-image img-path
                           :og-title title
                           :og-url (format "https://www.salttoday.com/comment?id=%s" id)
                           :og-description description
                           :twitter-title title
                           :twitter-description description
                           :twitter-image img-path)))))))