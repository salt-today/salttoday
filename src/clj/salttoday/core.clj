(ns salttoday.core
  (:require [salttoday.routes.handler :as handler]
            [salttoday.db.comments :refer [refresh-memoization]]
            [salttoday.db.posts :refer [update-posts-and-comments]]
            [salttoday.scraper.core :refer [scrape-sootoday]]
            [overtone.at-at :as at-at]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [salttoday.config :refer [env]]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:gen-class))

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (merge
    {:handler #'handler/app
     :io-threads (* 2 (.availableProcessors (Runtime/getRuntime)))
     :port (-> env :port)}
    (if (-> env :enable-ssl)
      {:ssl-port (-> env :ssl-port)
       :keystore (System/getenv "KEYSTORE_PATH")
       :key-password (System/getenv "SSL_PASSWORD")})))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (repl/start {:bind (env :nrepl-bind)
                 :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (repl/stop repl-server)))

(defn start-app
  "Mounts all stateful components"
  []
  (doseq [component (:started (mount/start))]
    (log/info component "started")))

(defn stop-app
  "Gracefully closes all components"
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

; TODO - scraper needs to be put into a defstate so we re-scrape when reloading state.
(defn -main [& args]
  ; Init Application
  (start-app)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. stop-app))
  (let [seconds 1000
        minutes (* seconds 60)
        interval (* 15 minutes)
        pool (at-at/mk-pool)]
    (at-at/every interval #(update-posts-and-comments (scrape-sootoday)) pool)
    (at-at/every interval #(refresh-memoization) pool)))
