(ns salttoday.core
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.history.Html5History)
  (:require [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as r]
            [salttoday.pages.home :as home]
            [salttoday.pages.users :as users]
            [salttoday.pages.about :as about]))

(defonce app-state (r/atom {:page :home
                            :query-params {}}))

(defmulti current-page #(@app-state :page))
;; TODO - add filters on query parameters, get rid of those that aren't relevant to the page
(defmethod current-page :home []
  [home/home-page (:query-params @app-state)])
(defmethod current-page :users []
  [users/users-page (:query-params @app-state)])
(defmethod current-page :about []
  [about/about-page (:query-params @app-state)])

;; -------------------------
;; Routes
;; Helpful Article - https://github.com/reagent-project/reagent-cookbook/tree/master/recipes/add-routing
;;
;; This is required so we can separate our frontend routes
;; from our backend routes and create a SPA.
(secretary/set-config! :prefix "#")

(defroute "/" [query-params]
  (swap! app-state assoc :page :home)
  (swap! app-state assoc :query-params query-params))

(defroute "/home" [query-params]
  (swap! app-state assoc :page :home)
  (swap! app-state assoc :query-params query-params))

(defroute "/users" [query-params]
  (swap! app-state assoc :page :users)
  (swap! app-state assoc :query-params query-params))

(defroute "/about" []
  (swap! app-state assoc :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (Html5History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app

(defn mount-components []
  (js/console.log "Mounting Components")
  (r/render [current-page]
            (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-components))
