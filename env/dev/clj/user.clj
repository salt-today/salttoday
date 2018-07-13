(ns user
  (:require [salttoday.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [salttoday.figwheel :refer [start-fw stop-fw cljs]]
            [salttoday.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'salttoday.core/repl-server))

(defn stop []
  (mount/stop-except #'salttoday.core/repl-server))

(defn restart []
  (stop)
  (start))


