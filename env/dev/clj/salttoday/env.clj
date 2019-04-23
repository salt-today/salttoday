(ns salttoday.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [salttoday.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[SaltToday [DEV] started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[SaltToday [DEV] has shut down successfully]=-"))
   :middleware wrap-dev})
