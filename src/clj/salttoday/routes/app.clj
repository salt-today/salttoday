(ns salttoday.routes.app
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]))

(defn app-page []
  (layout/render "app.html"))

(defroutes app-routes
  (GET "/" [sort-type day-range]
    (honeycomb/send-metrics {"page-view" "app"})
    (app-page)))
