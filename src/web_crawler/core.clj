(ns web-crawler.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as string])
  (:require [clj-http.client :as client])
  (:use [slingshot.slingshot :only [try+]]))

(defn read-file
  [file-name]
  (with-open [rdr (io/reader file-name)]
    (doall (line-seq rdr))))

(defn fetch-html
  [url]
  (println "try fetch")
  (try+
   (client/get url {:throw-exceptions false})
   (catch Object _ {:status 404 :headers nil})))

(defn parse-html-page
  [url]
  (let [html (fetch-html url)]
    (println html)))

(defn visit-node
  [urls depth]
  (let [new-depth (dec depth)]
    ;(if (< depth 1)
    ;  (println new-depth)
    ;  (println new-depth))))
    ;(map #(parse-html-page url) urls)))
    (parse-html-page (first urls))))

(defn crawling
  [file-name depth]
  (let [urls (read-file file-name)]
    (visit-node urls depth)))

(defn -main
  [& args]
  (let [file-name "resources/urls.txt"
        depth 1]
    (crawling file-name depth)))
  ;(let
  ;  [links (map :href
  ;     (map :attrs
  ;           (html/select (html/html-resource (java.net.URL. "https://github.com/")) #{[:a]})))
  ;   ]
  ;  (println (string/join "\n" links))))

