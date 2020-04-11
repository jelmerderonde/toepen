(ns toepen.server.state
  (:require [toepen.server.ws :as ws]
            [taoensso.sente :as sente]
            [toepen.common.game :as game]))

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

(defmethod handle-msg :game/shuffle
  [_]
  (swap! state game/clean-table))

(defmethod handle-msg :game/play-card
  [{:keys [?data]}]
  (let [{:keys [player-id card]} ?data]
    (swap! state game/play-card player-id card)))

(defmethod handle-msg :game/draw-card
  [{:keys [?data]}]
  (let [{:keys [player-id]} ?data]
    (swap! state game/draw-card player-id)))

(defmethod handle-msg :game/discard-hand
  [{:keys [?data]}]
  (let [{:keys [player-id]} ?data]
    (swap! state game/discard-hand player-id)))

(defmethod handle-msg :game/update-name
  [{:keys [?data ?reply-fn]}]
  (let [{:keys [player-id name]} ?data]
    (swap! state game/update-name player-id name)
    (?reply-fn true)))

(defmethod handle-msg :game/inc-points
  [{:keys [?data]}]
  (let [{:keys [player-id]} ?data]
    (swap! state game/inc-points player-id)))

(defmethod handle-msg :game/dec-points
  [{:keys [?data]}]
  (let [{:keys [player-id]} ?data]
    (swap! state game/dec-points player-id)))

(defmethod handle-msg :game/claim-dirty
  [{:keys [?data]}]
  (let [{:keys [player-id]} ?data]
    (swap! state game/claim-dirty player-id)))

(defmethod handle-msg :game/cancel-dirty
  [{:keys [?data]}]
  (let [{:keys [player-id]} ?data]
    (swap! state game/cancel-dirty player-id)))

(defmethod handle-msg :game/show-hand-to
  [{:keys [?data]}]
  (let [{:keys [player-id-from player-id-to]} ?data]
    (swap! state game/show-hand-to player-id-from player-id-to)))

(defn start-event-handling!
  []
  (sente/start-server-chsk-router!
    ws/chan handle-msg))

(comment
  (tap> @state)

  nil)
