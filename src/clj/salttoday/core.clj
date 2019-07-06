(ns salttoday.core
  (:require [salttoday.handler :as handler]
            [salttoday.db.posts :refer [update-posts-and-comments]]
            [salttoday.scraper :refer [scrape-sootoday]]
            [overtone.at-at :as at-at]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [salttoday.config :refer [env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

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

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

; TODO - scraper needs to be put into a defstate so we re-scrape when reloading state.
(defn -main [& args]
  (start-app args)
  (let [seconds 1000
        minutes (* seconds 60)
        interval (* 15 minutes)
        pool (at-at/mk-pool)]
    (at-at/every interval #(update-posts-and-comments (scrape-sootoday)) pool)))
