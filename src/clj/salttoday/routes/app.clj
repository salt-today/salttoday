(ns salttoday.routes.app
  (:require [salttoday.db.core :as db]
            [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]))

(def default-opengraph-tags {:og-image "/img/favicon.png"
                             :og-site-name "SaltToday"
                             :og-type "object"
                             :og-title "SaltToday.ca"
                             :og-url "https://www.salttoday.ca"
                             :og-description "A Leaderboard for SooToday's users and comments."})

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
    (clojure.pprint/pprint id)
    ; TODO - end the passing of '0'
    (let [comment (first (db/get-top-x-comments 0 1 nil 0 nil nil (salttoday.routes.api.v1.endpoints/string->number id)))]
      (clojure.pprint/pprint comment)
      (if (nil? comment)
        (app-page)
        (app-page {:og-image "/img/favicon.png"
                   :og-site-name "SaltToday"
                   :og-type "object"
                   :og-title (format "%s's Comment on \"%s\"" (:user comment) (:title comment))
                   :og-url (format "https://www.salttoday.com/comment?id=%s" id)
                   :og-description (:text comment)})))))