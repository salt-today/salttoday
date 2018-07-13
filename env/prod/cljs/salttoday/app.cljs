(ns ^:figwheel-no-load salttoday.app
  (:require [salttoday.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
