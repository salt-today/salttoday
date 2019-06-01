(ns salttoday.routes.home
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]
            [salttoday.routes.common :refer [string->number]]))

(defn home-page []
  (layout/render "home.html"))

(defn string->number [str]
  "Converts a string to a number, if nil or not a number, returns 0."
  (if (nil? str)
    0
    (let [n (read-string str)]
      (if (number? n) n 0))))

(defroutes home-routes
  (GET "/" []
    (honeycomb/send-metrics {"page-view" "home"})
    (home-page))

  (GET "/comments" [offset amount sort-type days search-text user]
    (let [offset-num (string->number offset)
          amount-num (string->number amount)
          days-num (string->number days)]
      (-> (response/ok (db/get-top-x-comments offset-num amount-num sort-type days-num search-text user))
          (response/header "Content-Type"
                           "application/json")))))



