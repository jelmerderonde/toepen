(ns toepen.server.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [reitit.ring.middleware.dev]
            [toepen.server.ws :as ws]
            [toepen.server.state :as state]
            [toepen.server.page :as page]
            [clojure.string :as str]))

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

(def mw-config
  {:params {:urlencoded true
            :multipart true
            :nested true
            :keywordize true}
   :cookies true
   :session {:flash false
             :cookie-attrs {:http-only false
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

(defn lower-case-game-id
  [handler]
  (fn [request]
    (handler (if (get-in request [:params :game-id])
               (update-in request [:params :game-id] str/lower-case)
               request))))

(defn handler
  [config]
  (ring/ring-handler
    (ring/router
     [["/ws" {:get ws/get-handler
              :post ws/post-handler}]
      ["/:game-id" page/game]
      ["/" page/index]]
     {:conflicts nil})
      ;:reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs})
    (ring/routes
      (ring/create-default-handler))
    {:middleware [[wrap-defaults config] [lower-case-game-id]]}))

(defn start-server
  [{:keys [middleware port]}]
  (stop-server)
  (stop-event-handler)
  (state/stop-watch!)
  (reset! server (http/run-server (handler middleware) {:port port}))
  (state/start-watch!)
  (reset! event-handler (state/start-event-handling!)))


(comment
  #_ (user/rebl)

  (start-server {:middleware mw-config
                 :port 8080})

  nil)

(def prod-config
  (let [port (Integer. (or (System/getenv "PORT") 8080))
        prod-mw {:session {:cookie-attrs {:http-only true}}
                 :security {:ssl-redirect true}
                 :proxy true}]
    {:port port
     :middleware (merge-with merge mw-config prod-mw)}))

(defn -main
  [& _]
  (println "Starting toepen...")
  (start-server prod-config))
