(ns toepen.server.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]
            [ring.util.response :as resp]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

; TODO Add anti-forgery-middleware
; TODO Tweak the middlware until it works


(defonce server (atom nil))
(def channel (sente/make-channel-socket! (get-sch-adapter) {}))

(def connected-uids (:connected-uids channel))

(defn stop-server
  []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn index
  [_]
  (resp/resource-response "index.html" {:root "public"}))

(def handler
  (ring/ring-handler
    (ring/router
     [["/chsk" {:get {:middleware [[wrap-keyword-params] [wrap-params]]
                      :handler (:ajax-get-or-ws-handshake-fn channel)}
                :post {:middleware [[wrap-keyword-params] [wrap-params]]
                       :handler (:ajax-post-fn channel)}}]
      ["/" index]
      ["/:game" index]]
     {:conflicts nil})
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

(handler {:request-method :get :uri "/chsk?udt=1585853173870&client-id=06aba6cd-58f0-4632-b91e-e2525c09508c&handshake?=true"})

(handler {:request-method :get :uri "/ping"})


(comment
  (do
    (stop-server)
    (reset! server (http/run-server #'handler {:port 8080})))

  nil)

(defn -main
  [& _]
  (println "Starting toepen..."))
