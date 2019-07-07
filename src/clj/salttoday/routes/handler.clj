(ns salttoday.routes.handler
  (:require
   [salttoday.layout :refer [error-page]]
   [salttoday.routes.api.v1.endpoints :as v1-api]
   [salttoday.routes.app :refer [app-routes]]
   [compojure.core :refer [routes wrap-routes]]
   [compojure.route :as route]
   [salttoday.env :refer [defaults]]
   [mount.core :as mount]
   [salttoday.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
   (routes
    (-> #'app-routes
        (wrap-routes middleware/wrap-formats))
    (-> #'v1-api/endpoints
        (wrap-routes middleware/wrap-formats))
    (route/not-found
     (:body
      (error-page {:status 404
                   :message "All out of Salt"
                   :reason "Page Not Found"}))))))

