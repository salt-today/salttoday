(ns salttoday.routes.util
  (:require [clojure.string :refer [blank?]]))

; TODO - return nil
(defn string->number
  "Converts a string to a number, if nil or not a number, returns 0."
  [str]
  (if (blank? str)
    0
    (let [n (read-string str)]
      (if (number? n) n 0))))

; TODO - return nil if nothing is probably better
(defn string->bool
  "Converts a string to a number, if nil or not a number, returns 0."
  [str]
  (if (blank? str)
    false
    (let [b (read-string str)]
      (if (boolean? b) b false))))

(defn blank-str->nil
  "If a string is blank, returns nil, otherwise returns the string"
  [str]
  (if (not (blank? str)) str))