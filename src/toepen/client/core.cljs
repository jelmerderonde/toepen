(ns toepen.client.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [toepen.client.ui :as ui]
            [toepen.client.state :as state]))

(defn index
  []
  (let [state (r/atom {:proposed-game "ProudPinoPrancingVictoriously"
                       :proposed-game-typed "ProudPinoPrancingVictoriously"
                       :game "ProudPinoPrancingVictoriously"})
        start-game (fn [] (set! (.. js/window -location -href) (str "/" (get @state :game))))]
    (fn []
      [:div {:class "w-screen h-screen flex flex-row items-center justify-center"}
       [:div {:class "w-1/2 flex flex-col items-center justify-start"}
        [:img {:src "logo.jpg"}]
        [:input {:class "mt-8 text-xl focus:outline-none focus:shadow-outline border-2 border-blue-400 text-blue-400 py-2 px-4 block appearance-none leading-normal"
                 :style {:width "22rem"}
                 :placeholder (get @state :proposed-game-typed "")}]
        [:button {:class "mt-4 text-xl text-blue-100 bg-blue-400 hover:bg-blue-600 font-bold py-2 px-8"
                  :on-click start-game}
         "Toep!"]]])))

(let [el (js/document.getElementById "app")
      data (.getAttribute el "data")]
  (case data
    "index" (rdom/render [index] el)
    "game" (do (rdom/render [ui/game] el)
               (state/refresh-state))))
