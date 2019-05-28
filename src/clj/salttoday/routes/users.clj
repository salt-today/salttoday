(ns salttoday.routes.users
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

(defn home-page []
  (layout/render "app.html"))

(defroutes users-routes
  (GET "/users" []
    (honeycomb/send-metrics {"page-view" "users"})
    (home-page)))
