(ns salttoday.routes.home
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" []
    (honeycomb/send-metrics {"page-view" "home"})
    (home-page))

  (GET "/top-comments" []
    (honeycomb/send-metrics {"page-view" "top-comments"})
    (-> (response/ok {:daily (db/get-daily-comments 3)
                      :all-time (db/get-top-x-comments 50)})
        (response/header "Content-Type"

                         "application/json"))))



