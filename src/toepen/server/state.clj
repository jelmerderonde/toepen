(ns toepen.server.state
  (:require [toepen.server.ws :as ws]
            [taoensso.sente :as sente]
            [toepen.server.game :as game]))

(def state (atom {}))

; TODO filter state so it is no longer possible to cheat
(defn send-state
  [_ _ _ new-state]
  (tap> "[server] new state")
  (doseq [uid (:any @ws/connected)]
    (ws/send! uid [:state/new new-state])))

(defn start-watch!
  []
  (add-watch state :send-state send-state))

(defn stop-watch!
  []
  (remove-watch state :send-state))

(defmulti handle-msg :id)

(defmethod handle-msg :default
  [{:keys [id] :as msg}]
  (when (not= id :chsk/ws-ping)
    (tap> (dissoc msg :ring-req))))

; handle the arrival of a new client
(defmethod handle-msg :chsk/uidport-open
  [{:keys [uid]}]
  (tap> "[server] user connected")
  (if (empty? @state)
    (reset! state (-> (game/new-game)
                      (game/add-player uid)))
    (swap! state game/add-player uid)))

(defmethod handle-msg :state/request
  [{:keys [uid]}]
  (ws/send! uid [:state/new @state]))

(defmethod handle-msg :chsk/uidport-close
  [{:keys [uid]}]
  (tap> "[server] user disconnected")
  (if (= (count (:players @state)) 1)
    (do
      (tap> "[server] all users disconnected, clearing game")
      (reset! state {}))
    (swap! state game/remove-player uid)))

(defmethod handle-msg :game/reset
  [_]
  (swap! state game/reset-game))

(defmethod handle-msg :game/deal
  [_]
  (swap! state game/deal-cards 4))

(defn start-event-handling!
  []
  (sente/start-server-chsk-router!
    ws/chan handle-msg))

(comment
  (do @state)

  (do
    (stop-watch!)
    (start-watch!))

  (swap! state game/deal-cards 4)
  nil)
