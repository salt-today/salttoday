(ns salttoday.core
  (:require [reagent.core :as r]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [salttoday.pages.home :as home]
            [salttoday.pages.user-leaderboard :as users]
            [salttoday.pages.about :as about])
  (:import goog.History))

(defonce session (r/atom {:page :home}))

(def pages
  {:home #'home/home-page
   :users #'users/users-page
   :about #'about/about-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (swap! session assoc :page :home))

(secretary/defroute "/users" []
  (swap! session assoc :page :users))

(secretary/defroute "/about" []
  (swap! session assoc :page :about))


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
  (js/console.log "Mounting Components")
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-components))
