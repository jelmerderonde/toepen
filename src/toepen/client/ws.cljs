(ns toepen.client.ws
  (:require [taoensso.sente :as sente]))

(def csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(def game-id
  (when-let [el (.getElementById js/document "game-id")]
    (.getAttribute el "data-game-id")))

(def socket
  (sente/make-channel-socket-client!
    "/ws" csrf-token
    {:type :auto
     :packer :edn
     :params {:game-id game-id}}))

(def state (:state socket))
(def send! (:send-fn socket))
(def chan (:ch-recv socket))
(def success? sente/cb-success?)