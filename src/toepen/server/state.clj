(ns toepen.server.state
  (:require [toepen.server.ws :as ws]
            [taoensso.sente :as sente]
            [toepen.common.game :as game]
            [clojure.walk :as walk]))

(def state (atom {}))

(defn game-client?
  "Checks if the id of the client belongs to
  the game id specified"
  [game-id id]
  (= (subs id 37) game-id))

(defn filter-state
  "Filters cards information out of the state
  so it is not possible to cheat."
  [game-state uid]
  (walk/postwalk
    (fn [form] (if (and (map? form)
                        (contains? form :visible-for)
                        (not= (:visible-for form) :all)
                        (not ((:visible-for form) uid)))
                 (dissoc form :cards)
                 form))
    game-state))

(defn send-state
  [_ _ old-state new-state]
  (doseq [[game-id game-state] new-state]
    (when (not= (get old-state game-id) game-state)
      (tap> (str "[" game-id "] new state"))
      (doseq [uid (->> @ws/connected
                       :any
                       (filter (partial game-client? game-id)))]
        (ws/send! uid [:state/new (filter-state game-state uid)])))))

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

(defn get-game-id
  "Extracts the game id from the msg"
  [msg]
  (get-in msg [:ring-req :params :game-id]))

; handle the arrival of a new client
(defmethod handle-msg :chsk/uidport-open
  [{:keys [uid] :as msg}]
  (let [game-id (get-game-id msg)]
    (tap> (str "[" game-id "] user connected"))
    (if (contains? @state game-id)
      (swap! state update game-id game/add-player uid)
      (swap! state assoc game-id (-> (game/new-game)
                                     (game/add-player uid))))))

(defmethod handle-msg :state/request
  [{:keys [uid] :as msg}]
  (let [game-id (get-game-id msg)]
    (ws/send! uid [:state/new (filter-state (get @state game-id) uid)])))

(defmethod handle-msg :chsk/uidport-close
  [{:keys [uid] :as msg}]
  (let [game-id (get-game-id msg)]
    (tap> (str "[" game-id "] user disconnected"))
    (if (= 1 (count (:players (get @state game-id))))
      (do
        (tap> (str "[" game-id "] all users disconnected, clearing game"))
        (swap! state dissoc game-id))
      (swap! state update game-id game/remove-player uid))))

(defmethod handle-msg :game/reset
  [msg]
  (let [game-id (get-game-id msg)]
    (swap! state update game-id game/reset-game)))

(defmethod handle-msg :game/shuffle-and-deal
  [msg]
  (let [game-id (get-game-id msg)]
    (swap! state update game-id game/shuffle-and-deal)))

(defmethod handle-msg :game/play-card
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id card]} ?data]
    (swap! state update game-id game/play-card player-id card)))

(defmethod handle-msg :game/draw-card
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/draw-card player-id)))

(defmethod handle-msg :game/discard-hand
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/discard-hand player-id)))

(defmethod handle-msg :game/update-name
  [{:keys [?data ?reply-fn] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id name]} ?data]
    (swap! state update game-id game/update-name player-id name)
    (?reply-fn true)))

(defmethod handle-msg :game/inc-points
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/inc-points player-id)))

(defmethod handle-msg :game/dec-points
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/dec-points player-id)))

(defmethod handle-msg :game/claim-dirty
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/claim-dirty player-id)))

(defmethod handle-msg :game/cancel-dirty
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/cancel-dirty player-id)))

(defmethod handle-msg :game/show-hand-to
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id-from player-id-to]} ?data]
    (swap! state update game-id game/show-hand-to player-id-from player-id-to)))

(defmethod handle-msg :game/deactivate
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/deactivate player-id)))

(defmethod handle-msg :game/activate
  [{:keys [?data] :as msg}]
  (let [game-id (get-game-id msg)
        {:keys [player-id]} ?data]
    (swap! state update game-id game/activate player-id)))

(defn start-event-handling!
  []
  (sente/start-server-chsk-router!
    ws/chan handle-msg))

(comment
  (tap> @state)

  nil)
