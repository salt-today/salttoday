(ns salttoday.routes.about
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]))

(defn home-page []
  (layout/render "home.html"))

(defroutes about-routes
  (GET "/#/about" []
    (honeycomb/send-metrics {"page-view" "about"})
    (home-page)))



