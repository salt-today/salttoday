(ns salttoday.routing.util
  (:require [accountant.core :as accountant]))

(def history
  (.-history js/window))

; Modified from - https://gist.github.com/kordano/56a16e1b28d706557f54
(defn url->map
  "Parse URL parameters into a hashmap"
  [url]
  (if (clojure.string/includes? url "?")
    (let [param-strs (-> url (clojure.string/split #"\?") last (clojure.string/split #"\&"))]
      (into {} (for [[k v] (map #(clojure.string/split % #"=") param-strs)]
                 [(keyword k) v])))
    {}))

(defn map->search-string [map]
  (if (empty? map)
    ""
    (let [params (for [[k v] map]
                   (str (name k) "=" v))]
      (str "?" (clojure.string/join "&" params)))))

;; NOTE - this is my best attempt at updating query parameters
;; I can't find any documentation, or examples around this.
;; This may need to be changed in the future.
;; TODO - possibly could be simplified / replaced with https://github.com/clj-commons/secretary#named-routes
(defn update-query-parameters! [new-params]
  (let [current-url (.-href (.-location js/window))
        path (-> current-url
                 (clojure.string/split #"\?")
                 first)
        current-params (url->map current-url)
        updated-params (merge current-params new-params)
        filtered-params (into {} (remove (comp nil? second) updated-params))]
    (accountant/navigate! (str path (map->search-string filtered-params)))))