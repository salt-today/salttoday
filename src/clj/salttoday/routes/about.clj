(ns salttoday.routes.about
  (:require [salttoday.layout :as layout]
            [compojure.core :refer [defroutes GET PUT]]))

(defn home-page []
  (layout/render "home.html"))

(defroutes about-routes
  (GET "/#/about" []
    (home-page)))



