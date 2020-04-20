(ns toepen.server.ws
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [org.httpkit.server] ;required for sente adapter
            [clojure.string :as str]))

(def socket (sente/make-channel-socket-server!
              (get-sch-adapter)
              {:user-id-fn (fn [req]
                             (let [session (get-in req [:session/key])
                                   game-id (str/lower-case (get-in req [:params :game-id] ""))]
                               (str session "-" game-id)))
               :packer :edn}))

(def get-handler (:ajax-get-or-ws-handshake-fn socket))
(def post-handler (:ajax-post-fn socket))
(def connected (:connected-uids socket))
(def send! (:send-fn socket))
(def chan (:ch-recv socket))
