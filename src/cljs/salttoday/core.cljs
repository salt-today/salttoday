(ns salttoday.core
  (:require [reagent.core :as r]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [salttoday.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [salttoday.pages.home :as home]
            [salttoday.pages.user-leaderboard :as users])
  (:import goog.History))

(defonce session (r/atom {:page :home}))

(def pages
  {:home #'home/home-page
   :users #'users/users-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (swap! session assoc :page :home))

(secretary/defroute "/users" []
  (swap! session assoc :page :users))


;; -------------------------
;; History
;; must be called after routes have been defined


(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app

(defn mount-components []
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))
