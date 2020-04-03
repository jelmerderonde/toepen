(ns toepen.client.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as s :refer [<!]]
            [toepen.client.ws :as ws]))

(go-loop []
  (let [{:keys [event]} (<! ws/chan)]
    (when (= (first event) :chsk/recv)
      (js/console.log (str (second event))))
    (recur)))
