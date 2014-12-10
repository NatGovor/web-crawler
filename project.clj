(defproject web-crawler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [enlive "1.1.5"]
                 [clj-http "1.0.1"]
                 [slingshot "0.12.1"]
                 [clojurewerkz/urly "1.0.0"]
                 [org.clojure/tools.cli "0.3.1"]]
  :main ^:skip-aot web-crawler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
