(ns web-crawler.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as string])
  (:require [clj-http.client :as client])
  (:use [slingshot.slingshot :only [try+]])
  (:refer-clojure :exclude [resolve])
  (:use clojurewerkz.urly.core)
  (:import [java.net URI URL])
  (:require [clojure.tools.cli :refer [cli]]))

(defn read-file
  [file-name]
  (with-open [rdr (io/reader file-name)]
    (doall (line-seq rdr))))

(defn create-node
  [url status depth children urls location]
  {:url url :status status :depth depth :children children :urls urls :location location})

(defn remove-nils
  [col]
  (filter #(not (nil? %)) col))

(defn ?redirect
  [status]
  (if (some #(= status %) '(301 302 303 305 307))
    true
    false))

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
                   (if (?redirect status)
                     (create-node url status depth (atom '()) (parse-content url html) (:location (html :headers)))
                     (create-node url status depth (atom '()) (parse-content url html) nil))
                   (create-node url status depth (atom '()) '() nil))]
    (swap! (:children node) conj new-node)
    new-node))

(defn visit-links
  [urls depth node]
  (let [new-depth (dec depth)]
    (pmap #(parse-page % new-depth node) urls)))

(defn crawling-loop
  [node urls depth]
  (if (> depth 0)
    (let [new-depth (dec depth)]
      (doseq [new-node (visit-links urls depth node)]
        (crawling-loop new-node (:urls new-node) new-depth))))
    node)

(defn crawling
  [file-name depth]
  (let [urls (read-file file-name)
        root  (create-node "root" nil 0 (atom '()) urls nil)]
    (crawling-loop root urls depth)
    root))

(defn get-indent
  [n]
  (if (<= n 0)
    ""
    (apply str (repeat n " "))))

(defn get-message
  [status links-count location]
  (if (= status 404)
    (str " bad")
    (if (?redirect status)
      (str links-count " redirect " location)
      (str links-count))))

(defn print-node
  [node level]
  (let [indent (* 4 (dec level))
        uri (:url node)
        status (:status node)
        links-count (count (:urls node))
        location (:location node)]
    (println (str (get-indent indent) uri " " (get-message status links-count location)))))

(defn walk-tree
  [node level]
  (if (not (= level 0))
    (print-node node level))
  (doseq [child @(:children node)] (walk-tree child (inc level))))

(defn print-tree
  [root]
  (walk-tree root 0))


(defn -main
  [& args]
  (let [[opts args] (cli args ["-f" "--file"  :default "resources/urls.txt"]
                              ["-d" "--depth" :default "2"])
        tree (crawling (:file opts) (Integer/parseInt (:depth opts)))]
    (print-tree tree)))

