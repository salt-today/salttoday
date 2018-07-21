(ns salttoday.scraper
  (:require [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [clojure.string :as str]))

(defn ^:private starts-with-strings
  [string substrings]
  (some true?
        (for [sub substrings]
          (.startsWith string sub))))


(defn ^:private get-url-from-div
  [div]
  (str "https://www.sootoday.com"
       (-> div
           (html/select [:a])
           first
           :attrs
           :href)))

(defn ^:private get-id-from-div
  [div]
  (-> div
      (html/select [:a])
      first
      :attrs
      :data-id))

(defn ^:private get-title-from-div
  [div]
  (-> div
      (html/select [:div.section-title])
      first
      :content
      first
      (.trim)))


(defn ^:private get-articles-from-homepage
  []
  (let [article-divs (-> (html/html-snippet
                           (:body @(http/get "https://www.sootoday.com" {:insecure? true})))
                         (html/select [:div.section-items]))
        most-recent (take 10 article-divs)
        titles-and-urls (for [div most-recent]
                          {:title (get-title-from-div div)
                           :url (get-url-from-div div)
                           :id (get-id-from-div div)})]
    (filter #(starts-with-strings (:url %)
                                  ["https://www.sootoday.com/local-news/" "https://www.sootoday.com/spotlight/"
                                   "https://www.sootoday.com/great-stories/" "https://www.sootoday.com/videos/"])
            titles-and-urls)))



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
  [{:keys [id title url]}]
  (let [comment-url (format "https://www.sootoday.com/comments/load?Type=Comment&ContentId=%s&TagId=2346&TagType=Content" id)
        comments-html (-> (html/html-snippet
                            (:body @(http/get comment-url {:insecure? true})))
                          (html/select [:div.comment]))]
    {:url url
     :title title
     :comments (for [comment-html comments-html]
                 {:username (get-username comment-html)
                  :timestamp (get-comment-time comment-html)
                  :comment (get-comment-text comment-html)
                  :upvotes (get-upvotes comment-html)
                  :downvotes (get-downvotes comment-html)})}))

(defn scrape-sootoday
  []
  (flatten
    (for [article (get-articles-from-homepage)]
      (get-comments-from-article article))))

