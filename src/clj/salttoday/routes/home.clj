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
    (-> (response/ok {:daily (db/get-daily-comments 3)
                      :weekly (db/get-weekly-comments 5)
                      :all-time (db/get-top-x-comments 50)})
        (response/header "Content-Type"

                         "application/json"))))



