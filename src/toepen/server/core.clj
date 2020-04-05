(ns toepen.server.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [hiccup.page :as page]
            [reitit.ring.middleware.dev]
            [toepen.server.ws :as ws]
            [toepen.server.state :as state]))

(defonce server (atom nil))

(defn stop-server
  []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defonce event-handler (atom nil))

(defn stop-event-handler
  []
  (when-not (nil? @event-handler)
    (@event-handler)
    (reset! event-handler nil)))

(defn index
  [req]
  (let [csrf-token (:anti-forgery-token req)
        html (page/html5
               {:lang "en"}
               [:head
                  [:meta {:charset "utf-8"}]
                  [:title "Toepen 4 evah!"]
                  [:link {:href "https://unpkg.com/tailwindcss@^1.0/dist/tailwind.min.css"
                          :rel "stylesheet"}]
                  (page/include-css "style.css")]
               [:body
                [:div {:id "sente-csrf-token"
                       :data-csrf-token csrf-token}]
                [:div {:id "app"}]
                (page/include-js "js/main.js")])]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

(def mw-config
  {:params {:urlencoded true
            :multipart true
            :nested true
            :keywordize true}
   :cookies true
   :session {:flash false
             :cookie-attrs {:http-only false ; TODO add secure option in production
                            :same-site :strict}}
   :security {:anti-forgery true
              :xss-protection {:enable? true
                               :mode :block}
              :frame-options :sameorigin
              :content-type-options :nosniff}
   :static {:resources "public"}
   :responses {:not-modified-responses true
               :absolute-redirects true
               :content-types true
               :default-charset "utf-8"}})

(def handler
  (ring/ring-handler
    (ring/router
     [["/ws" {:get ws/get-handler
              :post ws/post-handler}]
      ["/" index]]
     {:conflicts nil})
      ;:reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs})
    (ring/routes
      (ring/create-default-handler))
    {:middleware [[wrap-defaults mw-config]]}))

(comment
  #_ (user/rebl)

  (do
    (stop-server)
    (stop-event-handler)
    (state/stop-watch!)
    (reset! server (http/run-server #'handler {:port 8080}))
    (state/start-watch!)
    (reset! event-handler (state/start-event-handling!)))

  nil)

(defn -main
  [& _]
  (println "Starting toepen..."))
