(ns salttoday.routes.home
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]))

(defn home-page []
  (layout/render "app.html"))

(defroutes home-routes
  (GET "/" [sort-type day-range]
    (honeycomb/send-metrics {"page-view" "home"})
    (home-page)))
