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
  [url result]
  (let [html (fetch-page url)
        status (html :status)]
    (if (not= status 404)
      (let [new-urls (parse-content url html)]
        (swap! result conj new-urls)
        (println url (count new-urls))
        new-urls)
      ; bad url
      (swap! result conj '()))))

(defn visit-link
  [urls depth result]
  (let [new-depth (dec depth)]
    (map #(parse-page % result) urls)))

(defn crawling-loop
  [urls depth result]
  (let [new-depth (dec depth)]
    (if (> depth 0)
      (doseq [new-urls (visit-link urls depth result)]
        (crawling-loop new-urls new-depth result)))))

(defn crawling
  [file-name depth result]
  (let [urls (read-file file-name)]
    (crawling-loop urls depth result)))

(defn -main
  [& args]
  (let [file-name "resources/urls.txt"
        depth 2
        result (atom[])]
    (crawling file-name depth result)
    (println (count @result))))
    ;(println @result)))

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

