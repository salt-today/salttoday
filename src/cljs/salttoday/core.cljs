(ns salttoday.core
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.history.Html5History)
  (:require [accountant.core :as accountant]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as r]
            [salttoday.views.home :as home]
            [salttoday.views.comment :as comment]
            [salttoday.views.users :as users]
            [salttoday.views.about :as about]))

(defonce app-state (r/atom {:page :home
                            :query-params {}}))

(defmulti current-page #(@app-state :page))
;; TODO - add filters on query parameters, get rid of those that aren't relevant to the page
(defmethod current-page :home []
  [home/home-page (:query-params @app-state)])
(defmethod current-page :comment []
  [comment/comment-page (:query-params @app-state)])
(defmethod current-page :users []
  [users/users-page (:query-params @app-state)])
(defmethod current-page :about []
  [about/about-page (:query-params @app-state)])

;; -------------------------
;; Routes
;; Helpful Article - https://github.com/reagent-project/reagent-cookbook/tree/master/recipes/add-routing
;; - https://pupeno.com/2015/08/26/no-hashes-bidirectional-routing-in-re-frame-with-bidi-and-pushy/
;; - https://pez.github.io/2016/03/01/Reagent-clientside-routing-with-Bidi-and-Accountant.html

(defroute "/" [query-params]
  (swap! app-state assoc :page :home)
  (swap! app-state assoc :query-params query-params))

(defroute "/home" [query-params]
  (swap! app-state assoc :page :home)
  (swap! app-state assoc :query-params query-params))

(defroute "/comment" [query-params]
  (swap! app-state assoc :page :comment)
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
  (accountant/configure-navigation!
   {:nav-handler   (fn [path]
                     (js/console.log path)
                     (secretary/dispatch! path))
    :path-exists?  (fn [path]
                     (secretary/locate-route path))
    :reload-same-path? true}))

;; -------------------------
;; Initialize app

(defn mount-components []
  (r/render [current-page]
            (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  ; Route to the current URL (User hasn't made an action yet!)
  (accountant/dispatch-current!)
  (mount-components))
