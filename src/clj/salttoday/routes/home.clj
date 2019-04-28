(ns salttoday.routes.home
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

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

  (GET "/top-comments" []
    (honeycomb/send-metrics {"page-view" "top-comments"})
    (-> (response/ok {:daily (db/get-top-x-comments 0 3 "score" 1)
                      :weekly (db/get-top-x-comments 0 5 "score" 7)
                      :all-time (db/get-top-x-comments 0 50 "score" -1)})
        (response/header "Content-Type"
                         "application/json")))

  (GET "/comments" [offset amount sort-type days]
    (let [offset-num (string->number offset)
          amount-num (string->number amount)
          days-num (string->number days)]
      (-> (response/ok (db/get-top-x-comments offset-num amount-num sort-type days-num))
          (response/header "Content-Type"
                           "application/json")))))



