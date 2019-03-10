(ns salttoday.routes.common
    (:require [salttoday.layout :as layout]
      [compojure.core :refer [defroutes GET PUT]]
      [ring.util.http-response :as response]
      [clojure.java.io :as io]
      [salttoday.db.core :as db]))

(defroutes common-routes
           (GET "/todays-stats" []
                (-> (response/ok (db/get-todays-stats))
                    (response/header "Content-Type"
                                     "application/json"))))



