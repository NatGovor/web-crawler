(ns web-crawler.core-test
  (:require [clojure.test :refer :all]
            [web-crawler.core :refer :all]))

(deftest fetch-page-test
  (are [result url] (= result (:status (fetch-page url)))
       200 "http://github.com/"
       404 "http://github.com/gsdjhsj"
       404 "http://sjdhjshdjs.html"))

(deftest parse-content-test
  (are [links-count valid-url] (= links-count (count (parse-content valid-url (fetch-page valid-url))))
       1 "http://example.com"
       31 "https://github.com"))
