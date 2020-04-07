(ns toepen.client.ws
  (:require [taoensso.sente :as sente]))

(def csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(def socket
  (sente/make-channel-socket-client!
    "/ws" csrf-token
    {:type :auto
     :packer :edn}))

(def state (:state socket))
(def send! (:send-fn socket))
(def chan (:ch-recv socket))
(def success? sente/cb-success?)