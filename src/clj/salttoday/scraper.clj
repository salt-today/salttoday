(ns salttoday.scraper
  (:require [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [clojure.string :as str]
            [salttoday.metrics.core :as honeycomb]))

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
  (let [contents (-> div first :content)
        content-text-list (for [content contents]
                            (-> (if (map? content)
                                  (-> content :content first)
                                  content)
                                .trim))]
    (clojure.string/join " " content-text-list)))

(defn ^:private get-articles-from-homepage
  []
  (let [article-divs (-> (html/html-snippet
                          (:body @(http/get "https://www.sootoday.com" {:insecure? true})))
                         (html/select [[:a (html/attr? :data-id)]]))
        titles-and-urls (for [div article-divs]
                          {:url (get-url-from-div div)
                           :id (get-id-from-div div)})]
    (filter #(starts-with-strings (:url %)
                                  ["https://www.sootoday.com/local-news/" "https://www.sootoday.com/spotlight/"
                                   "https://www.sootoday.com/great-stories/" "https://www.sootoday.com/videos/"
                                   "https://www.sootoday.com/local-sports/" "https://www.sootoday.com/local-entertainment/"])
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
  (-> (html/select comment-html [:a.comment-un])
      get-content-helper))

(defn ^:private tag-type->encoding [tag-type content]
  (cond
    (= tag-type :p) (str content "\n")
    (= tag-type :b) (str "**" content "**")
    (= tag-type :strong) (str "**" content "**")
    (= tag-type :i) (str "_" content "_")
    (= tag-type :em) (str "_" content "_")
    (= tag-type :br) "\n"
    :else content))

(defn ^:private collect-content [tag]
  (if (nil? (:content tag))
    (tag-type->encoding (:tag tag) "")
    (let [tag-contents (:content tag)
          tag-type (:tag tag)
          result (for [content tag-contents]
                   (if (map? content)
                     (collect-content content)
                     content))]
      (tag-type->encoding tag-type
                          (clojure.string/join result)))))

(defn ^:private get-comment-text
  "Gets the comment text given the html of a comment"
  [comment-html]
  (-> (html/select comment-html [:div.comment-text])
      first
      (collect-content)
      (clojure.string/trim-newline)))

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
  [{:keys [id url]}]
  (let [comment-url (format "https://www.sootoday.com/comments/load?Type=Comment&ContentId=%s&TagId=2346&TagType=Content" id)
        comments-html (-> (html/html-snippet
                           (:body @(http/get comment-url {:insecure? true})))
                          (html/select [:div.comment]))
        title-div (-> (html/html-snippet
                       (:body @(http/get url {:insecure? true})))
                      (html/select [:h1]))
        title (get-title-from-div title-div)
        comments {:url url
                  :title title
                  :comments (for [comment-html comments-html]
                              {:username (get-username comment-html)
                               :timestamp (get-comment-time comment-html)
                               :comment (get-comment-text comment-html)
                               :upvotes (get-upvotes comment-html)
                               :downvotes (get-downvotes comment-html)})}]
    (honeycomb/send-metrics {"context" "scraper"
                             "article-id" id
                             "article-title" title
                             "comment-url" comment-url
                             "num-comments" (count (:comments comments))})
    comments))

(defn scrape-sootoday
  []
  (flatten
   (let [articles (get-articles-from-homepage)]
     (honeycomb/send-metrics {"context" "scraper"
                              "num-articles" (count articles)})
     (for [article articles]
       (get-comments-from-article article)))))

