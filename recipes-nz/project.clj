(defproject recipes-nz "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-enlive-template "0.0.1"]
                 [noir "1.3.0"]
                 [clj-json "0.5.3"]
                 [enlive "1.1.1"]
                 [jayq "0.1.0-alpha4"]
                 [crate "0.2.0-alpha4"]
                 [fetch "0.1.0-alpha2"]
                 [org.clojure/data.json "0.2.1"]
                 [com.novemberain/monger "1.4.2"]]
  :plugins [[lein-cljsbuild "0.2.5"]]
  :cljsbuild {:builds
              [{:builds nil,
                :jar true
                :source-path "src/cljs"
                :compiler {:output-dir "resources/public/js/"
                           :output-to "resources/public/js/main.js"
                           :optimization :simple
                           :pretty-print true}}]}
  :main server.server
  ;:main extraction.recipe-extractor
)
