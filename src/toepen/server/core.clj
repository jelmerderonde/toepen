(ns toepen.server.core
  (:require [org.httpkit.server :as http]
            [reitit.ring :as ring]))

(defonce server (atom nil))

(defn stop-server
  []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(def handler
  (ring/ring-handler
    (ring/router
     ["/ping" (constantly {:status 200 :body "pong"})])
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

(comment
  (reset! server (http/run-server #'handler {:port 8080}))
  (stop-server)
  nil)

(defn -main
  [& _]
  (println "Starting toepen..."))
