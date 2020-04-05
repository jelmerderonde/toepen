(ns toepen.client.state
  (:require [toepen.client.ws :as ws]
            [reagent.core :as r]
            [taoensso.sente :as sente]))

(def state (r/atom {}))

(defn ^:dev/after-load refresh-state
  []
  (js/console.log "requesting state")
  (ws/send! [:state/request]))

(defmulti handle-msg
  (fn [{:keys [id ?data]}]
    (if (= id :chsk/recv)
      (first ?data)
      id)))

(defmethod handle-msg :default
  [msg]
  ;(js/console.log (with-out-str (pp/pprint msg))))
  nil)

(defmethod handle-msg :chsk/state
  [{:keys [?data]}]
  (let [[_ {:keys [first-open? uid]}] ?data]
    (when first-open?
      (swap! state assoc :uid uid)
      (refresh-state))))

(defmethod handle-msg :state/new
  [{:keys [?data]}]
  (swap! state assoc :game (second ?data)))

(sente/start-client-chsk-router! ws/chan handle-msg)
