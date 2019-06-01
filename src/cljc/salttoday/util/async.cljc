(ns salttoday.util.async
  (:require [clojure.core.async :as a]))

; I'm no Macro Wizard, cant get these to work YET
; in clojurescript, macros must be defined in a namespace different than where they are consumed
(defmacro async [& body]
  `(a/go ~@body))

(defmacro await [channel]
  `(a/<! ~channel))
