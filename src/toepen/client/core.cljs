(ns toepen.client.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as s :refer [<!]]
            [toepen.client.ws :as ws]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(def state (r/atom {}))

(defmulti handle-ws-event first)

(defmethod handle-ws-event :state/new
  [[_ new-state]]
  (reset! state new-state))

(go-loop []
  (let [{:keys [event]} (<! ws/chan)]
    (when (= (first event) :chsk/recv)
      (handle-ws-event (second event)))
    (recur)))

(defn test-comp
  []
  [:div (get @state :text "no text yet")])

(rdom/render [test-comp] (js/document.getElementById "app"))
