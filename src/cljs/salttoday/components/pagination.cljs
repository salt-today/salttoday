(ns salttoday.components.pagination
  (:require [cuerdas.core :as string]
            [salttoday.views.common :refer [get-comments update-query-params-with-state]]))

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

(defn page->offset [page-number page-size]
  ; this should never happen since buttons should be accordingly disabled
  (let [adjusted-page (if (zero? page-number)
                        page-number
                        (dec page-number))]
    (* adjusted-page (inc page-size))))

(defn pagination-component [state current-page number-of-elements page-size page-buffer]
  ; page-size is inclusive. ie. a page size of 20 is actually 19 since we work with inclusive offsets
  (let [total-pages (Math/ceil (/ number-of-elements page-size))
        current-page (if (zero? current-page)
                       1
                       current-page)]
    ; TODO these event handlers ruin the generic-ness of the component because now its only for comments
    ; but i have to think about the solution and for now it's fine
    ; it would probably be better to pass in some sort of callback
    ; TODO - scroll to top
    [:div.row.justify-center
     (if (= current-page 1)
       [:div.column [:a.page-button.disabled "First"]]
       [:div.column [:a.page-button {:on-click (fn []
                                                 (do
                                                   (swap! state assoc :offset 0)
                                                   (update-query-params-with-state state :comments :users)
                                                   (get-comments state)))}
                     "First"]])
     (if (= current-page 1)
       [:div.column [:a.page-button.disabled "Previous"]]
       [:div.column [:a.page-button {:on-click (fn []
                                                 (do
                                                   (swap! state assoc :offset (page->offset (dec current-page) page-size))
                                                   (update-query-params-with-state state :comments :users)
                                                   (get-comments state)))}
                     "Previous"]])

     ; Previous Pages
     (for [page (range (lower-bound current-page page-buffer) current-page)]
       [:div.column [:a.page-button {:on-click (fn []
                                                 (do
                                                   (swap! state assoc :offset (page->offset page page-size))
                                                   (update-query-params-with-state state :comments :users)
                                                   (get-comments state)))}
                     (str page)]])

     ; Current Page
     [:div.column [:a.page-button.active (str current-page)]]

     ; Next Pages
     (for [page (range (inc current-page) (upper-bound current-page page-buffer total-pages))]
       [:div.column [:a.page-button {:on-click (fn []
                                                 (do
                                                   (swap! state assoc :offset (page->offset page page-size))
                                                   (update-query-params-with-state state :comments :users)
                                                   (get-comments state)))}
                     (str page)]])

     (if (= current-page total-pages)
       [:div.column [:a.page-button.disabled "Next"]]
       [:div.column [:a.page-button {:on-click (fn []
                                                 (do
                                                   (swap! state assoc :offset (page->offset (inc current-page) page-size))
                                                   (update-query-params-with-state state :comments :users)
                                                   (get-comments state)))}
                     "Next"]])
     (if (= current-page total-pages)
       [:div.column [:a.page-button.disabled "Last"]]
       [:div.column [:a.page-button {:on-click (fn []
                                                 (do
                                                   (swap! state assoc :offset (page->offset total-pages page-size))
                                                   (update-query-params-with-state state :comments :users)
                                                   (get-comments state)))}
                     "Last"]])]))