(ns salttoday.scraper
  (:require [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [salttoday.metrics.core :as honeycomb]))

(defn ^:private starts-with-strings
  [string substrings]
  (some true?
        (for [sub substrings]
          (.startsWith string sub))))

(defn ^:private get-url-from-article-link
  [link]
  (str "https://www.sootoday.com"
       (-> link
           :attrs
           :href)))

(defn ^:private get-id-from-article-link
  [link]
  (-> link
      :attrs
      :data-id))

(defn safe-trim [str]
  (if (nil? str)
    nil
    (str/trim str)))

(defn render-node
  [node]
  (let [contents (-> node :content)
        content-text-list (for [content contents]
                            (safe-trim (if (map? content)
                                         ; TODO - im not terribly thrilled about this
                                         (when (not= (:tag content) :div)
                                           (-> content :content first))
                                         content)))]
    (safe-trim
     (clojure.string/join " " content-text-list))))

(defn select-links-and-titles [html-payload]
  (map vector (html/select html-payload [:a.section-item])
       (html/select html-payload [:div.section-title])))

(defn get-articles-from-homepage
  []
  (let [html-payload (html/html-snippet
                      (:body @(http/get "https://www.sootoday.com" {:insecure? true})))
        link-and-title-elements (select-links-and-titles html-payload)
        titles-and-urls (for [pair link-and-title-elements
                              :let [link (first pair)
                                    title (second pair)]]
                          {:url (get-url-from-article-link link)
                           :id (get-id-from-article-link link)
                           :title (render-node title)})]
    titles-and-urls
    (set
     (filter #(starts-with-strings (:url %)
                                   ["https://www.sootoday.com/local-news/"
                                    "https://www.sootoday.com/spotlight/"
                                    "https://www.sootoday.com/great-stories/"
                                    "https://www.sootoday.com/videos/"
                                    "https://www.sootoday.com/local-sports/"
                                    "https://www.sootoday.com/local-entertainment/"
                                    "https://www.sootoday.com/bulletin/"
                                    "https://www.sootoday.com/more-local/"])
             titles-and-urls))))

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

; NOTE - this relies on the fact that on SooToday's comments they do not consider
; replies as a comment, the number of comments is == to the top level comments
; It also relies on the fact that only 20 comments can be displayed per article
; and this includes replies!
(defn num-comment-api-calls [initial-payload]
  (let [comments-wrapper (html/select initial-payload [:div#comments])
        ; TODO - error handling here incase nothing is found
        num-top-level-comments (Integer/parseInt (:data-count (:attrs (first comments-wrapper))))]
    (int (Math/ceil (/ num-top-level-comments 20)))))

; Logging and Tracing around scraping
; TODO - last-id is currently unused because I've yet to see a reply chain >20 comments
; we can only surmise on the usage currently
(defn get-replies [article-id last-id parent-id-str]
  (let [req (format "https://www.sootoday.com/comments/get?ContentId=%s&TagId=2346&TagType=Content&Sort=Oldest&lastId=%22%22&ParentId=%s" article-id parent-id-str)
        resp (-> (html/html-snippet
                  (:body @(http/get req {:insecure? true}))))
        replies-html (html/select resp [:div.comment])]
    (for [reply-html replies-html]
      ; TODO - store reply relationship
      {:username (get-username reply-html)
       :timestamp (get-comment-time reply-html)
       :comment (get-comment-text reply-html)
       :upvotes (get-upvotes reply-html)
       :downvotes (get-downvotes reply-html)})))

; Result should be flattened
(defn get-comments [comments-html article-id]
  (for [comment-html comments-html
        :let [num-replies (Integer/parseInt (:data-replies (:attrs comment-html)))]]
    ; If the comment has no replies, just get the comment itself
    (if (= 0 num-replies)
      ; TODO - store reply relationship
      {:username (get-username comment-html)
       :timestamp (get-comment-time comment-html)
       :comment (get-comment-text comment-html)
       :upvotes (get-upvotes comment-html)
       :downvotes (get-downvotes comment-html)}
      ; If the comment has replies, check for a "Load More" button
      (let [load-more-html (first (html/select comment-html [:button.comments-more]))]
        ; If the button doesn't exist, just get the comments
        (if (nil? load-more-html)
          ; Replies cannot be replies to at this time, if they can, this needs to be refactored into a recursive format
          (for [reply-html (html/select comment-html [:div.comment])]
            ; TODO - store reply relationship
            {:username (get-username reply-html)
             :timestamp (get-comment-time reply-html)
             :comment (get-comment-text reply-html)
             :upvotes (get-upvotes reply-html)
             :downvotes (get-downvotes reply-html)})
          ; If the button exists, get all replies
          ; TODO - the reply endpoint is different, I'm not sure what happens if there are more than 20 replies
          ; log added to deal with this
          (if (> num-replies 20)
            (log/info "HUGE REPLY CHAIN FOUND! ARTICLE ID: " article-id)
            (get-replies article-id nil (:data-parent (:attrs load-more-html)))))))))

; SooToday has two types of return values from their API
; 1 - contains a div.comments element - returned from page-load
; 2 - just contains a body tag with a collection of comments
(defn select-top-level-comments [html]
  (if (empty? (html/select html [:div#comments]))
    ; This isn't completely necessary anymore because i found a selector that works for both
    ; However, separating this might still be a good idea incase sootoday changes and allows
    ; for deeply nested replies.  This only works but only top level comments have the
    ; data-replies attr
    (html/select html [[:div (html/attr? :data-replies)]])
    (html/select html [:div#comments :> :div.comment])))

(defn comment-search [comments calls-left html-payload article-id]
  ; If calls-left == 0, return comments
  (if (<= calls-left 0)
    comments
    ; Grab all _top_ level comments
    (let [comments-html (select-top-level-comments html-payload)
          new-comments (flatten (get-comments comments-html article-id))
          last-id (:data-id (:attrs (last comments-html)))
          next-url (format "https://www.sootoday.com/comments/get?ContentId=%s&TagId=2346&TagType=Content&ParentId=&lastId=%s" article-id last-id)
          next-resp (-> (html/html-snippet
                         (:body @(http/get next-url {:insecure? true
                                                     :timeout 5000}))))]
      ; Tail Recursion - Performant
      (comment-search (concat comments new-comments)
                      (dec calls-left)
                      next-resp
                      article-id))))

(defn get-comments-from-article [{:keys [id url title]}]
  (let [comment-url (format "https://www.sootoday.com/comments/load?Type=Comment&ContentId=%s&TagId=2346&TagType=Content&Sort=Oldest" id)
        initial-comment-payload (html/html-snippet
                                 (:body @(http/get comment-url {:insecure? true
                                                                :timeout 5000})))
        ; This is the number of times we have to hit the API to get all top-level comments
        num-comment-api-calls (num-comment-api-calls initial-comment-payload)
        comments (comment-search []
                                 num-comment-api-calls
                                 initial-comment-payload
                                 id)]
    (log/info "Scraped [" title "] from - " url)
    {:url url
     :title title
     :comments comments}))

(defn scrape-sootoday
  []
  (flatten
   (let [articles (get-articles-from-homepage)]
     (honeycomb/send-metrics {"context" "scraper"
                              "num-articles" (count articles)})
     (for [article articles]
       (get-comments-from-article article)))))

