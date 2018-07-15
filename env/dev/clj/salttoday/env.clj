(ns salttoday.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [salttoday.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[salttoday started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[salttoday has shut down successfully]=-"))
   :middleware wrap-dev
   :database-url "datomic:mem://salttoday"})
