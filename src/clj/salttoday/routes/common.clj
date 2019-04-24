(ns salttoday.routes.common
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [salttoday.db.core :as db]))

(defroutes common-routes
  (GET "/todays-stats" []
    (honeycomb/send-metrics {"page-view" "todays-stats"})
    (-> (response/ok (db/get-todays-stats))
        (response/header "Content-Type"
                         "application/json"))))



