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

(defn create-node
  [url status depth children urls]
  {:url url :status status :depth depth :children children :urls urls})

(defn remove-nils
  [col]
  (filter #(not (nil? %)) col))

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
  (let [valid-links (remove-nils links)
        relative-links (get-relative-links url valid-links)
        absolute-links (get-absolute-links valid-links)]
    (concat relative-links absolute-links)))

(defn get-links
  [html]
  (map :href (map :attrs (html/select html #{[:a]}))))

(defn parse-content
  [url response]
  (let [content-type ((response :headers) :content-type)]
    (if (re-find #"text/html" content-type)
      (get-valid-links url (get-links (html/html-snippet (response :body))))
      '())))

(defn fetch-page
  [url]
  (try+
   (client/get url)
   (catch Object _ {:status 404})))

(defn parse-page
  [url depth node]
  (let [html (fetch-page url)
        status (html :status)
        new-node (if (not= status 404)
                   (create-node url status depth (atom '()) (parse-content url html))
                   (create-node url status depth (atom '()) '()))]
    (swap! (:children node) conj new-node)
    new-node))

(defn visit-link
  [urls depth node]
  (let [new-depth (dec depth)]
    (pmap #(parse-page % new-depth node) urls)))

(defn crawling-loop
  [node urls depth]
  (let [new-depth (dec depth)]
    (if (> depth 0)
      (doseq [new-node (visit-link urls depth node)]
        (crawling-loop new-node (:urls new-node) new-depth)))
    node))

(defn crawling
  [file-name depth]
  (let [urls (read-file file-name)
        root  (create-node "root" nil 0 (atom '()) urls)]
    (crawling-loop root urls depth)
    root))

(defn get-indent
  [n]
  (apply str (repeat n " ")))

(defn get-message
  [status links-count]
  (if (= status 404)
    (str " bad")
    (if (= (.indexOf (str status) "3") 0)
      (str " redirect " links-count)
      (str links-count))))

(defn print-node
  [node level]
  (let [indent (* 2 level)
        uri (:url node)
        status (:status node)
        links-count (count (:urls node))]
    (println (get-indent indent) uri (get-message status links-count))))

(defn walk-tree
  [node level]
  (print-node node level)
  (doseq [child @(:children node)] (walk-tree child (inc level))))

(defn print-tree
  [root]
  (do-walk root 0))


(defn -main
  [& args]
  (let [file-name "resources/urls.txt"
        depth 2
        tree (crawling file-name depth)]
    (print-tree tree)))

