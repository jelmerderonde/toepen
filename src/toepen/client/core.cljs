(ns toepen.client.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [toepen.client.ui :as ui]
            [toepen.client.state :as state]
            [toepen.common.haiku-names :refer [haiku]]
            [clojure.string :as str]
            [cljs.core.async :refer [<! timeout]]
            [taoensso.encore :refer [ajax-lite]])
  (:require-macros [cljs.core.async.macros :as m :refer [go-loop]]))

(defn start-pings
  []
  (go-loop []
    (let [pause (+ 180000 (rand-int 120000))]
      (<! (timeout pause))
      (js/console.log "ping")
      (ajax-lite "/ping" {:method :get
                          :resp-type :text}
                         (fn [] (js/console.log "pong")))
      (recur))))

(defn new-name
  [state]
  (let [nn (haiku)]
    (-> state
        (assoc :proposed-game nn)
        (assoc :proposed-game-typed "")
        (assoc :type-queue (into [] nn)))))

(defn type-letter
  [{:keys [type-queue] :as state}]
  (-> state
      (update :proposed-game-typed str (first type-queue))
      (update :type-queue rest)))

(defn clean-game-name
  [s]
  (str/replace s #"[^A-Za-z0-9]" ""))

(defn index
  []
  (let [state (r/atom {:proposed-game ""
                       :proposed-game-typed ""
                       :type-queue []
                       :game ""})
        start-game (fn [] (let [{:keys [game proposed-game]} @state
                                dest (if (str/blank? game) proposed-game game)]
                            (set! (.. js/window -location -href) (str "/" dest))))]

    (go-loop []
      (if (empty? (get @state :type-queue))
        (do (<! (timeout 2500))
            (swap! state new-name))
        (do (<! (timeout 75))
            (swap! state type-letter)))
      (recur))

    (swap! state new-name)
    (fn []
      [:div {:class "w-screen h-screen flex flex-row items-center justify-center"}
       [:div {:class "w-1/2 flex flex-col items-center justify-start"}
        [:img {:src "logo.jpg"}]
        [:input {:id "game-input"
                 :class "mt-8 text-xl focus:outline-none focus:shadow-outline border-2 border-blue-400 text-blue-400 py-2 px-4 block appearance-none leading-normal"
                 :style {:width "24rem"}
                 :placeholder (get @state :proposed-game-typed "")
                 :value (get @state :game)
                 :on-change #(swap! state assoc :game (-> % .-target .-value clean-game-name))
                 :on-key-down #(case (.-which %)
                                 13 (start-game)
                                 27 (swap! state assoc :game "")
                                 nil)}]
        [:button {:class "mt-4 text-xl text-blue-100 bg-blue-400 hover:bg-blue-600 font-bold py-2 px-8"
                  :on-click start-game}
         "Toep!"]]])))

(start-pings)

(let [el (js/document.getElementById "app")
      data (.getAttribute el "data")]
  (case data
    "index" (do (rdom/render [index] el)
                (.focus (js/document.getElementById "game-input")))
    "game" (do (rdom/render [ui/game] el)
               (state/refresh-state))))
