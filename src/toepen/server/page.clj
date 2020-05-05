(ns toepen.server.page
  (:require [hiccup.page :as page]))

(defn index
  [req]
  (let [csrf-token (:anti-forgery-token req)
        html (page/html5
               {:lang "en"}
               [:head
                  [:meta {:charset "utf-8"}]
                  [:title "Toepen 4 evah!"]
                  [:link {:href "favicon.ico"
                          :rel "icon"
                          :type "image/x-icon"}]
                  [:link {:href "https://unpkg.com/tailwindcss@^1.0/dist/tailwind.min.css"
                          :rel "stylesheet"}]
                  (page/include-css "style.css")]
               [:body
                [:div {:id "app"
                       :data "index"}]
                (page/include-js "js/main.js")])]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

(defn game
  [req]
  (let [csrf-token (:anti-forgery-token req)
        game-id (get-in req [:path-params :game-id])
        html (page/html5
               {:lang "en"}
               [:head
                  [:meta {:charset "utf-8"}]
                  [:title "Toepen 4 evah!"]
                  [:link {:href "favicon.ico"
                          :rel "icon"
                          :type "image/x-icon"}]
                  [:link {:href "https://unpkg.com/tailwindcss@^1.0/dist/tailwind.min.css"
                          :rel "stylesheet"}]
                  (page/include-css "style.css")]
               [:body
                [:div {:id "sente-csrf-token"
                       :data-csrf-token csrf-token}]
                [:div {:id "game-id"
                       :data-game-id game-id}]
                [:div {:id "app"
                       :data "game"}]
                (page/include-js "js/main.js")])]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

