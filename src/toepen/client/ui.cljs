(ns toepen.client.ui
  (:require [toepen.client.state :refer [state]]
            [toepen.client.ws :as ws]
            [toepen.common.cards :as c]
            [toepen.common.game :as g]
            [clojure.string :as str]
            [reagent.core :as r]))

(defn card
  [{:keys [card index visible? base-size ml mt back-color card-action]
    :or {base-size "6rem"
         ml 14
         mt 14
         visible? true}}]
  (let [[suit c] card
        filename (if visible?
                   (str "cards.svg#" (name suit) "_" (name c))
                   "cards.svg#back")]
    [:svg {:viewBox "0 0 169.075 244.640"
           :class (str/join " " [(when visible? "card") (when (not visible?) back-color) "absolute top-0 left-0 hover:z-50 fill-current"])
           :style {:width base-size
                   :margin-left (* index ml)
                   :margin-top (* index mt)}
           :on-click (fn [e] (card-action e card))}
     [:use {:href filename}]]))

(defn stack
  [{:keys [stack mode uid size back-color extra-classes card-action stack-action order]
    :or {mode :stack ; stack, hand or deck
         size :base ; :xs, :sm, :base, :lg
         back-color "text-blue-800"
         extra-classes ""
         card-action (fn [_ _] nil)
         stack-action (fn [_] nil)}}]
  (let [cards (vec (c/get-cards stack order))
        visible-for (:visible-for stack)
        visible? (if (= visible-for :all)
                   true
                   (contains? visible-for uid))
        base-size (case size
                    :xs "2rem"
                    :sm "4rem"
                    :base "6rem"
                    :lg "8rem")
        ml (case size
             :xs (case mode :stack 4 :hand 20 :deck 0.5)
             :sm (case mode :stack 10 :hand 26 :deck 0.5)
             :base (case mode :stack 14 :hand 30 :deck 0.5)
             :lg (case mode :stack 20 :hand 36 :deck 0.5))
        mt (case mode
             :stack ml
             :hand 0
             :deck 0.5)
        extra-width (* (dec (count cards)) ml)
        extra-height (* (dec (count cards)) mt)]
    [:div {:class (str "relative " extra-classes)
           :style {:width (str "calc(" base-size " + " extra-width "px)")
                   :height (str "calc(1.446931835 * " base-size " + " extra-height "px)")}
           :on-click stack-action}
     (for [i (range (count cards))]
       ^{:key i} [card {:card (get cards i)
                        :index i
                        :visible? visible?
                        :base-size base-size
                        :ml ml
                        :mt mt
                        :back-color back-color
                        :card-action card-action}])]))

(defn counter
  [n]
  (let [color (cond
                (> n 10) "bg-red-400"
                (> n 5) "bg-orange-400"
                :else "bg-blue-400")]
    [:div {:class (str "flex items-center justify-center text-white rounded-full text-xs w-5 h-5 ml-2 " color)}
     (str n)]))

(defn name-tag
  []
  (let [editing? (r/atom false)
        new-name (r/atom "")]
    (fn [{:keys [name editable? uid dealer? points]
          :or {name ""
               editable? false
               dealer? false
               points 0}}]
      (let [close #(do (swap! editing? not) (reset! new-name ""))
            send (fn []
                   (let [v (-> @new-name str str/trim)]
                     (if-not (empty? v)
                       (ws/send! [:game/update-name {:player-id uid :name v}]
                                 4000
                                 (fn [reply]
                                   (if (ws/success? reply)
                                     (swap! editing? not)
                                     (close))))
                       (close))))]
        (if @editing?
          [:input {:class "bg-gray-200 rounded-lg p-2 shadow text-lg text-center font-medium outline-none focus:shadow-outline focus:bg-blue-100 w-48"
                   :type :text
                   :value @new-name
                   :placeholder "Player name"
                   :on-change #(reset! new-name (-> % .-target .-value))
                   :on-blur #(do (send))
                   :on-key-down #(case (.-which %)
                                   13 (send)
                                   nil)
                   :autoFocus true}]
          [:div {:class (str "flex flex-row flex-no-wrap items-baseline justify-between bg-white rounded-lg p-2 shadow text-lg font-medium truncate" (when editable? " select-none cursor-pointer"))
                 :style {:max-width "14rem"}
                 :on-click #(when editable? (swap! editing? not))}
            (when dealer? "⭐️ ")
            name
            [counter points]])))))

(defn player
  [{:keys [game uid player-id]}]
  (let [{:keys [name hand table dealer? points dirty?]} (get-in game [:players player-id])
        my-hand-visible? (contains? (get-in game [:players uid :hand :visible-for]) player-id)]
    [:div {:class "flex-1 flex flex-col flex-no-wrap items-center justify-start"}
     [:div {:class (str "flex flex-col flex-no-wrap items-center justify-between p-4 rounded-b-lg " (if my-hand-visible? "bg-red-200" "bg-gray-200"))
            :style {:min-width "8rem"}}
      [name-tag {:name name
                 :dealer? dealer?
                 :points points}]
      [stack {:stack hand
              :uid uid
              :mode :hand
              :size (if dirty? :sm :xs)
              :extra-classes "mt-2"
              :back-color (if dirty? "text-red-800" "text-blue-800")
              :stack-action (fn [_] (when dirty? (ws/send! [:game/show-hand-to {:player-id-from player-id
                                                                                :player-id-to uid}])))}]]
     [stack {:stack table
             :uid uid
             :mode :hand
             :extra-classes "mt-3"
             :order :by-order}]]))

(defn button
  [{:keys [text extra-classes on-click]
    :or {extra-classes ""
         on-click (fn [_] nil)}}]
  [:button {:class (str "text-white font-bold py-2 px-4 rounded shadow mt-4 " extra-classes)
            :on-click on-click}
    text])

(defn menu
  [{:keys [player uid]}]
  (let [{:keys [dealer? points dirty? name]} player]
    [:div {:class "flex-initial flex flex-col flex-no-wrap items-center justify-start pt-10"
           :style {:width "24rem"}}
     [name-tag {:name name
                :editable? true
                :uid uid
                :dealer? dealer?
                :points points}]
     [:div {:class "flex flex-no-wrap justify-between w-48"}
      [button {:extra-classes "bg-gray-400 text-gray-100 hover:bg-gray-600 w-20"
               :on-click (fn [_] (ws/send! [:game/dec-points {:player-id uid}]))
               :text "-"}]
      [button {:extra-classes "bg-gray-400 text-gray-100 hover:bg-gray-600 w-20"
               :on-click (fn [_] (ws/send! [:game/inc-points {:player-id uid}]))
               :text "+"}]]
     [button {:extra-classes (str "w-48 " (if dirty? "bg-gray-400 text-gray-100 hover:bg-gray-600"
                                                     "bg-red-400 text-red-100 hover:bg-red-600"))
              :on-click (fn [_] (if dirty?
                                  (ws/send! [:game/cancel-dirty {:player-id uid}])
                                  (ws/send! [:game/claim-dirty {:player-id uid}])))
              :text (if dirty? "Oeps, toch niet" "Vuile was")}]
     (when dirty?
       [button {:extra-classes "bg-red-400 text-red-100 hover:bg-red-600 w-48"
                :on-click (fn [_] (ws/send! [:game/discard-hand {:player-id uid}]))
                :text "Leg kaarten af"}])
     (when dealer?
       [:div {:class "flex flex-col flex-no-wrap items-center justify-start"}
        [button {:extra-classes "bg-green-400 text-green-100 hover:bg-green-600 w-48"
                 :on-click (fn [_] (ws/send! [:game/deal]))
                 :text "Deel 4 kaarten"}]
        [button {:extra-classes "bg-blue-400 text-blue-100 hover:bg-blue-600 w-48"
                 :on-click (fn [_] (ws/send! [:game/shuffle]))
                 :text "Schud kaarten"}]
        [button {:extra-classes "bg-purple-400 text-purple-100 hover:bg-purple-600 w-48"
                 :on-click (fn [_] (when (js/confirm "Reset the game? All points will be reset!")
                                     (ws/send! [:game/reset])))
                 :text "Nieuw spel"}]])]))

(defn the-deck
  [{:keys [deck discarded uid]}]
  [:div {:class "flex-initial flex flex-row flex-no-wrap items-center justify-center"
         :style {:width "24rem"}}
   [stack {:stack deck
           :uid uid
           :mode :deck
           :extra-classes ""
           :size :base
           :stack-action (fn [_] (ws/send! [:game/draw-card {:player-id uid}]))}]
   [stack {:stack discarded
           :uid uid
           :mode :deck
           :size :base
           :extra-classes ""
           :back-color "text-gray-700"}]])

(defn hand
  [{:keys [player uid]}]
  (let [{:keys [hand table dirty?]} player]
    [:div {:class "flex-grow flex flex-col flex-no-wrap items-center justify-end"}
     [stack {:stack table
             :mode :hand
             :uid uid
             :size :base
             :extra-classes "mb-3"}]
     [:div {:class (str "flex flex-row flex-no-wrap items-center justify-center p-4 rounded-t-lg " (if dirty? "bg-red-200" "bg-gray-200"))
            :style {:min-width "50%"}}
      [stack {:stack hand
              :uid uid
              :mode :hand
              :size :lg
              :order :by-rank
              :card-action (fn [_ card] (ws/send! [:game/play-card {:player-id uid :card card}]))}]]]))

(defn ordered-opponents
  [game uid]
  (let [order (g/get-order game)
        players (get game :players)
        c (count players)
        idx (.indexOf (to-array order) uid)]
    (->> (cycle order)
         (take (+ c idx))
         (drop (inc idx)))))

(defn top
  [{:keys [game uid]}]
  [:div {:class "flex-1 flex flex-row flex-wrap items-start justify-around"}
   (for [id (ordered-opponents game uid)]
     ^{:key id} [player {:game game
                         :uid uid
                         :player-id id}])])

(defn bottom
  [{:keys [uid game]}]
  (let [{:keys [players deck discarded]} game
        player (get players uid)]
    [:div {:class "flex-1 flex flex-row flex-no-wrap items-stretch justify-around"}
     [menu {:player player
            :uid uid}]
     [hand {:player player
            :uid uid}]
     [the-deck {:deck deck
                :discarded discarded
                :uid uid}]]))

(defn root
  []
  (let [uid (:uid @state)
        game (:game @state)]
    [:div {:class "h-screen w-screen bg-gray-100 flex flex-col flex-no-wrap items-stretch justify-start"}
     [top {:game game
           :uid uid}]
     [bottom {:game game
              :uid uid}]]))
