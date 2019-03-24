(ns salttoday.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[salttoday started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[salttoday has shut down successfully]=-"))
   :middleware identity
   :database-url "datomic:free://localhost:4334/salttoday"
   :port 80})
