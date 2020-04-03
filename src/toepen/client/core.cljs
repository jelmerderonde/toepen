(ns toepen.client.core
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente  :as sente :refer (cb-success?)]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(def socket (sente/make-channel-socket! "/ws" ?csrf-token {:type :auto}))

(go-loop []
  (let [{:keys [event id ?data send-fn]} (<! (:ch-recv socket))]
    (js/console.log event)
    (recur)))

(comment
  (let [{:keys [chsk ch-recv send-fn state]}]


    (def chsk       chsk)
    (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
    (def chsk-send! send-fn) ; ChannelSocket's send API fn
    (def chsk-state state)))   ; Watchable, read-only atom

