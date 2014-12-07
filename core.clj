(ns web-crawler.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.string :as string]))

(defn -main
  [& args]
  (let
    [links (map :href
       (map :attrs
             (html/select (html/html-resource (java.net.URL. "http://clojure-doc.org/articles/tutorials/introduction.html")) #{[:a]})))
     ]
    (println (string/join "\n" links))))
