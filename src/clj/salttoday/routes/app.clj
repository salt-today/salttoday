(ns salttoday.routes.app
  (:require [salttoday.layout :as layout]
            [salttoday.metrics.core :as honeycomb]
            [compojure.core :refer [defroutes GET PUT]]))

(defn app-page []
  (layout/render "app.html"))

; TODO - SSR for OpenGraph tags

; A catch all route here fixes issues on the homepage, but causes the page to hang...possibly re-rendering over and over...doesn't make much sense.
; however specific routes makes things easier for SSR, don't have to figure out what page it is
(defroutes app-routes
  (GET "/" [sort-type day-range :as request]
    (honeycomb/send-metrics {"page-view" "app"})
    (app-page))
  (GET "/home" [sort-type day-range :as request]
    (honeycomb/send-metrics {"page-view" "app"})
    (app-page))
  (GET "/users" [sort-type day-range :as request]
    (honeycomb/send-metrics {"page-view" "app"})
    (app-page))
  (GET "/about" [sort-type day-range :as request]
    (honeycomb/send-metrics {"page-view" "app"})
    (app-page)))
