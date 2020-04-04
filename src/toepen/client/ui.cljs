(ns toepen.client.ui
  (:require [toepen.client.state :refer [state]]))

;(defn root
;  []
;  [:div
;   [:button
;    {:class "bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
;     :on-click (fn [_] (ws/send! [:game/reset]))}
;    "reset game"]
;   [:button
;     {:class "bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
;      :on-click (fn [_] (ws/send! [:game/deal]))}
;     "deal cards"]
;   [:pre [:code (with-out-str (pp/pprint @state))]]])

(defn player-comp
  [{:keys [name]}]
  [:div {:class "flex-1"} name])

(defn root
  []
  [:div {:class "h-screen w-screen bg-gray-300 flex flex-col flex-no-wrap items-stretch justify-start"}
   [:div {:class "flex-1 bg-red-300 flex flex-row flex-no-wrap items-stretch justify-around"}
    (for [[id player] (:players @state)]
      ^{:key id} [player-comp player])]
   [:div {:class "flex-grow bg-yellow-300"}]
   [:div {:class "flex-grow bg-blue-300"}]])


