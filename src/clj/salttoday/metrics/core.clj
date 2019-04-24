(ns salttoday.metrics.core
  (:require [mount.core :refer [defstate]])
  (:import [io.honeycomb.libhoney HoneyClient]
           [io.honeycomb.libhoney LibHoney]))

(defn create-honey-client [key]
  (if (nil? key)
    nil
    (let [optionsBuilder (LibHoney/options)]
      (doto optionsBuilder
        (.setWriteKey key)
        (.setDataset "SaltToday-Prod"))
      (LibHoney/create (.build optionsBuilder)))))

(defstate honey-client
  :start (let [honeycomb-key (System/getenv "HONEYCOMB_KEY")]
           (create-honey-client honeycomb-key))
  :stop (if-not (nil? honey-client)
          (.close honey-client)))

(defn send-metrics [map]
  (if-not (nil? honey-client)
    (.send honey-client map)))
