(ns toepen.server.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [hiccup.page :as page]))

(defonce server (atom nil))
(def socket (sente/make-channel-socket! (get-sch-adapter) {}))

(defn stop-server
  []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn index
  [req]
  (let [csrf-token (:anti-forgery-token req)
        html (page/html5
               {:lang "en"}
               [:head
                  [:meta {:charset "utf-8"}]
                  [:title "Toepen 4 evah!"]
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
             :cookie-attrs {:http-only true
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
     [["/ws" {:get (:ajax-get-or-ws-handshake-fn socket)
              :post (:ajax-post-fn socket)}]
      ["/" index]]
     {:conflicts nil})
    (ring/routes
      (ring/create-default-handler))
    {:middleware [[wrap-defaults mw-config]]}))

(comment
  (do
    (stop-server)
    (reset! server (http/run-server #'handler {:port 8080})))

  nil)

(defn -main
  [& _]
  (println "Starting toepen..."))
