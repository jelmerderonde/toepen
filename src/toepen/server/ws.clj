(ns toepen.server.ws
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [org.httpkit.server])) ;required for sente adapter

(def socket (sente/make-channel-socket-server!
              (get-sch-adapter)
              {:user-id-fn (fn [req] (get-in req [:session/key]))
               :packer :edn}))

(def get-handler (:ajax-get-or-ws-handshake-fn socket))
(def post-handler (:ajax-post-fn socket))
(def connected (:connected-uids socket))
(def send! (:send-fn socket))
(def chan (:ch-recv socket))
