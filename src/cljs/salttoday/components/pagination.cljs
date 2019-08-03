(ns salttoday.components.pagination
  (:require [cuerdas.core :as string]))

(defn lower-bound [current-page buffer]
  (let [result (- current-page buffer)]
    (if (< result 1)
      1
      result)))

(defn upper-bound [current-page buffer total-pages]
  (let [result (+ current-page buffer)]
    (if (> result total-pages)
      total-pages
      result)))

(defn pagination-component [current-page total-pages buffer state]
  [:div.row.justify-center
   [:div.column [:a "First"]]
   [:div.column [:a "Previous"]]

   ; Previous Pages
   (for [page (range (lower-bound current-page buffer) current-page)]
     [:div.column [:a (str page)]])

   ; Current Page
   ; TODO - stylin
   [:div.column [:a (str current-page)]]

   ; Next Pages
   (for [page (range (inc current-page) (upper-bound current-page buffer total-pages))]
     [:div.column [:a (str page)]])

   [:div.column [:a "Next"]]
   [:div.column [:a "Last"]]])