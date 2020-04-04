(ns toepen.server.state
  (:require [toepen.server.ws :as ws]))

(def state (atom {}))

(defn send-state
  [_ _ _ new-state]
  (doseq [uid (:any @ws/connected)]
    (ws/send! uid [:state/new new-state])))

(defn start-watch!
  []
  (add-watch state :send-state send-state))

(defn stop-watch!
  []
  (remove-watch state :send-state))

(comment
  (swap! state assoc :text "Hello world!!")
  nil)
