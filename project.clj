(defproject salttoday "1.0.0"

  :description "Scrapes Sootoday and displays statistics about users and comments."
  :url "http://www.salttoday.ca"

  :min-lein-version "2.0.0"

  :main ^:skip-aot salttoday.core

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"

  :repositories [["jitpack.io" "https://jitpack.io"]]

  :dependencies [; Clojure Dependencies
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 ; Backend Dependencies
                 [clj-time "0.15.1"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [com.github.humboldtdev/logback-logdna-bulk "1.2"]
                 [com.google.guava/guava "23.0"]
                 [compojure "1.6.1"]
                 [cprop "0.1.13"]
                 [enlive "1.1.6"]
                 [http-kit "2.3.0"]
                 [io.honeycomb.libhoney/libhoney-java "1.0.6"]
                 [luminus-immutant "0.2.5"]
                 [luminus-nrepl "0.1.6"]
                 [metosin/muuntaja "0.5.0"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.16"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0-alpha.1"]
                 [overtone/at-at "1.2.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.12"]
                 ; Frontend Depedencies
                 [cljs-http "0.1.46"]
                 [funcool/cuerdas "2.2.0"]
                 [markdown-clj "1.10.0"]
                 [metosin/komponentit "0.3.8"]
                 [reagent "0.8.1" :exclusions [com.google.guava/guava]]
                 [secretary "1.2.3"]
                 ; Web Jars
                 [org.webjars.bower/tether "1.4.4"]
                 [org.webjars/font-awesome "5.0.13"]
                 [org.webjars/webjars-locator "0.36"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-immutant "2.1.0"]
            [lein-cljfmt "0.6.4"]
            [jonase/eastwood "0.3.5"]
            [lein-ancient "0.6.15"]]

  :repl-options {:timeout 120000}

  :clean-targets ^{:protect false}

  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]

  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-dir "target/cljsbuild/public/js"
                 :output-to "target/cljsbuild/public/js/app.js"
                 :source-map "target/cljsbuild/public/js/app.js.map"
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs ["react/externs/react.js"]}}}}
             :aot :all
             :uberjar-name "salttoday.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev]

   :project/dev  {:dependencies [[binaryage/devtools "0.9.10"]
                                 [com.cemerick/piggieback "0.2.2"]
                                 [expound "0.7.2"]
                                 [figwheel-sidecar "0.5.18" :exclusions [com.google.guava/guava]]
                                 [pjstadig/humane-test-output "0.9.0"]
                                 [prone "1.6.3"]
                                 [ring/ring-devel "1.7.1"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.19.0"]
                                 [lein-figwheel "0.5.18"]
                                 [org.clojure/clojurescript "1.10.520"]]
                  :cljsbuild
                  {:builds
                   {:app
                    {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                     :figwheel {:on-jsload "salttoday.core/mount-components"}
                     :compiler
                     {:main "salttoday.app"
                      :asset-path "/js/out"
                      :output-to "target/cljsbuild/public/js/app.js"
                      :output-dir "target/cljsbuild/public/js/out"
                      :source-map true
                      :optimizations :none
                      :pretty-print true}}}}

                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}})
