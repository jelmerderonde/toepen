(ns toepen.client.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as s :refer [<!]]
            [toepen.client.ws :as ws]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs.pprint :as pp]
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
  (let [[_ {:keys [first-open?]}] ?data]
    (when first-open?
      (refresh-state))))

(defmethod handle-msg :state/new
  [{:keys [?data]}]
  (reset! state (second ?data)))

(defn root
  []
  [:div
   [:button
    {:on-click (fn [_] (ws/send! [:game/reset]))}
    "reset game"]
   [:button
     {:on-click (fn [_] (ws/send! [:game/deal]))}
     "deal cards"]
   [:pre [:code (with-out-str (pp/pprint @state))]]])

(sente/start-client-chsk-router! ws/chan handle-msg)
(rdom/render [root] (js/document.getElementById "app"))
(refresh-state)
