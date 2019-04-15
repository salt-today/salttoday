(ns salttoday.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[SaltToday [PROD] started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[SaltToday [PROD] has shut down successfully]=-"))
   :middleware identity})
