(ns salttoday.pages.home.comment
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [clojure.core.async :as a]
            [reagent.core :as r]
            [salttoday.common :refer [display-comment]]
            [salttoday.pages.common :refer [content get-selected-value jumbotron make-content make-navbar
                                            make-right-offset update-query-params-with-state]]))