(ns salttoday.routes.home
  (:require [salttoday.layout :as layout]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" []
    (home-page))

  (GET "/top-comments" []
    (-> (response/ok {:daily-positive (db/get-daily-positive-comment)
                      :daily-negative (db/get-daily-negative-comment)
                      :all-time-positives (db/get-most-positive-comments 5)
                      :all-time-negatives (db/get-most-negative-comments 5)})
        (response/header "Content-Type"

                         "application/json"))))



