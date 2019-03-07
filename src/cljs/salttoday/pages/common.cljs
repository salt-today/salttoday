(ns salttoday.pages.common)

(defn jumbotron
  "Big name at the top?"
  [title]
  [:div {:class "jumbotron text-center"}
   [:h1 title]])

(defn content [x]
  [:div {:class "container"}
   x])

(defn nav-link [uri title current-page page icon]
  [:div.navigation-link {:class (when (= page current-page) "active")} [:a {:href uri} title]  [:span {:class icon}]])

(defn make-layout
  [current-page content]
  [:div.outer
   [:div.content-wrapper
    [:div.navigation
     [nav-link "#/" "Home " current-page :home "icon-home3"]
     [nav-link "#/users" "Users " current-page :users "icon-group"]]
    content [:img.title-image {:src "/img/SaltTodayLogo.svg"}]]
   [:img.footer-pile {:src "/img/footer-pile.png"}]])
