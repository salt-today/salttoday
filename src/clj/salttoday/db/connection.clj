(ns salttoday.db.connection
  (:require [datomic.api :as d]
            [mount.core :refer [defstate]]
            [salttoday.config :refer [env]]
            [salttoday.db.schema :refer [schema]]))

(defstate conn
  "Connection management, init the databse on startup"
  :start (let [db-url (:database-url env)
               db (d/create-database db-url)
               conn (d/connect db-url)]
           @(d/transact conn schema)
           conn)
  :stop (-> conn .release))