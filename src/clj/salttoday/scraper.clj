(ns salttoday.scraper
  (:require [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [clojure.string :as str]))


(defn ^:private starts-with-strings
  [string substrings]
  (some true?
        (for [sub substrings]
          (.startsWith string sub))))

(defn ^:private extract-title-from-url
  [url] (->>
          (-> (clojure.string/replace url #"\/(.*?)\/" "")
              (clojure.string/split #"-")
              (butlast))
          (clojure.string/join " ")
          (clojure.string/capitalize)))


(defn ^:private get-articles-from-homepage
  []
  (let [links (-> (html/html-snippet
                    (:body @(http/get "http://sootoday.com" {:insecure? true})))
                  (html/select [:div.widget-feature :a]))
        most-recent (take 10 links)
        hrefs (map #(get-in % [:attrs :href]) most-recent)]
    (->> (filter #(starts-with-strings % ["/local-news/" "/spotlight/" "/great-stories/"]) hrefs)
         (map #(last (.split % "-"))))))

(defn ^:private get-content-helper
  "Gets the content of username, text, upvotes, downvotes, etc"
  [comment-html]
  (-> comment-html
      first
      :content
      first))

(defn ^:private get-username
  "Gets the username given the html of a comment."
  [comment-html]
  (-> (html/select comment-html [:b])
      get-content-helper))

(defn ^:private get-comment-text
  "Gets the comment text given the html of a comment"
  [comment-html]
  (-> (html/select comment-html [:p])
      get-content-helper))

(defn ^:private get-upvotes
  "Gets the number of downvotes for a comment."
  [comment-html]
  (-> (html/select comment-html [(html/attr= :value "Upvote") :span])
      get-content-helper
      Integer/parseInt))

(defn ^:private get-downvotes
  "Gets the number of downvotes for a comment."
  [comment-html]
  (-> (html/select comment-html [(html/attr= :value "Downvote") :span])
      get-content-helper
      Integer/parseInt))

(defn ^:private get-comment-time
  [comment-html]
  (-> (html/select comment-html [:time])
      first
      :attrs
      :datetime))

(defn ^:private get-comments-from-article
  [article-id]
  (let [url (format "https://www.sootoday.com/comments/load?Type=Comment&ContentId=%s&TagId=2346&TagType=Content" article-id)
        comments-html (-> (html/html-snippet
                            (:body @(http/get url {:insecure? true})))
                          (html/select [:div.comment]))]
    {:url url
     :comments (for [comment-html comments-html]
                 {:username (get-username comment-html)
                  :timestamp (get-comment-time comment-html)
                  :comment (get-comment-text comment-html)
                  :upvotes (get-upvotes comment-html)
                  :downvotes (get-downvotes comment-html)})}))

(defn scrape-sootoday
  "Gets comments from the most recent articles"
  []
  (flatten
    (for [article (get-articles-from-homepage)]
      (get-comments-from-article article))))