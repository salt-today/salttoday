(ns salttoday.routes.users
  (:require [salttoday.layout :as layout]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

(defn home-page []
  (layout/render "home.html"))

(defroutes users-routes
  (GET "/#/users" []
    (home-page))

  (GET "/top-users" []
    (-> (response/ok {:users (db/get-top-rated-users 10)})
        (response/header "Content-Type"

                         "application/json"))))



