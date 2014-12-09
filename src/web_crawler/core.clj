(ns web-crawler.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as string])
  (:require [clj-http.client :as client])
  (:use [slingshot.slingshot :only [try+]])
  (:refer-clojure :exclude [resolve])
  (:use clojurewerkz.urly.core)
  (:import [java.net URI URL]))

(defn read-file
  [file-name]
  (with-open [rdr (io/reader file-name)]
    (doall (line-seq rdr))))

(defn resolve-relative-links
  [url relative-links]
  (map #(resolve url %) relative-links))

(defn get-relative-links
  [url links]
  (resolve-relative-links url (filter #(and (= (.indexOf % "/") 0) (= (.indexOf % "#") -1)) links)))

(defn get-absolute-links
  [links]
  (filter #(or (= (.indexOf % "http://") 0) (= (.indexOf % "https://") 0)) links))

(defn get-valid-links
  [url links]
  (let [relative-links (get-relative-links url links)
        absolute-links (get-absolute-links links)]
    (concat relative-links absolute-links)))

(defn get-links
  [html]
  (map :href (map :attrs (html/select html #{[:a]}))))

(defn parse-document
  [url response]
  (let [content-type ((response :headers) :content-type)]
    (if (re-find #"text/html" content-type)
      (get-valid-links url (get-links (html/html-snippet (response :body))))
      '())))

(defn fetch-page
  [url]
  (println "try fetch")
  (try+
   (client/get url)
   (catch Object _ {:status 404})))

(defn parse-page
  [url]
  (let [html (fetch-page url)
        status (html :status)]
    (if (not= status 404)
      (println url (count (parse-document url html)))
      (println url "bad"))))

(defn visit-node
  [urls depth]
  (let [new-depth (dec depth)]
    ;(if (< depth 1)
    ;  (println new-depth)
    ;  (println new-depth))))
    ;(map #(parse-html-page url) urls)))
    (map #(parse-page %) urls)))

(defn crawling
  [file-name depth]
  (let [urls (read-file file-name)]
    (visit-node urls depth)))

(defn -main
  [& args]
  (let [file-name "resources/urls.txt"
        depth 1]
    ;(parse-document "https://github.com/" (fetch-page "https://github.com/"))))
    ;(parse-page "https://github.com/")))
    (crawling file-name 1)))

    ;(map :href
    ;   (map :attrs
    ;         (html/select (html/html-resource (java.net.URL. "http://example.com/")) #{[:a]})))))

    ;(crawling file-name depth)))
  ;(let
  ;  [links (map :href
  ;     (map :attrs
  ;           (html/select (html/html-resource (java.net.URL. "https://github.com/")) #{[:a]})))
  ;   ]
  ;  (println (string/join "\n" links))))

