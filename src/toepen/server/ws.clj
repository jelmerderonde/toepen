(ns toepen.server.ws
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            [org.httpkit.server] ;required for sente adapter
            [taoensso.timbre :as log]
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

(defn- log-connection-delta
  [_ _ old-uids new-uids]
  (let [old-any (count (:any old-uids))
        new-any (count (:any new-uids))]
    (when (not= old-any new-any)
      (log/info {:event :connection-delta
                 :any new-any
                 :ws (count (:ws new-uids))
                 :ajax (count (:ajax new-uids))
                 :delta (- new-any old-any)}))))

(add-watch connected ::log-connection-delta log-connection-delta)
