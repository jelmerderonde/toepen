(ns toepen.client.ui
  (:require [toepen.client.state :refer [state]]
            [toepen.client.ws :as ws]
            [toepen.common.cards :as c]
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
           :class (str/join " " [(when visible? "card") back-color "absolute top-0 left-0 hover:z-50 fill-current"])
           :style {:width base-size
                   :margin-left (* index ml)
                   :margin-top (* index mt)}
           :on-click (fn [e] (card-action e card))}
     [:use {:href filename}]]))

(defn stack
  [{:keys [stack mode visible? size back-color extra-classes card-action stack-action order]
    :or {mode :stack ; stack, hand or deck
         size :base ; :xs, :sm, :base, :lg
         visible? true
         back-color "text-blue-800"
         extra-classes ""
         card-action (fn [_ _] nil)
         stack-action (fn [_] nil)}}]
  (let [cards (vec (c/get-cards stack order))
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

(defn name-tag
  []
  (let [editing? (r/atom false)
        new-name (r/atom "")]
    (fn [{:keys [name editable? uid]
          :or {name ""
               editable? false}}]
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
          [:span {:class (str "text-center block bg-white rounded-lg p-2 shadow text-lg font-medium truncate" (when editable? " select-none cursor-pointer"))
                  :style {:max-width "14rem"}
                  :on-click #(when editable? (swap! editing? not))}
                 name])))))

(defn player
  [{:keys [name hand table]}]
  [:div {:class "flex-1 flex flex-col flex-no-wrap items-center justify-start"}
   [:div {:class "flex flex-col flex-no-wrap items-center justify-between p-4 bg-gray-200 rounded-b-lg"
          :style {:min-width "8rem"}}
    [name-tag {:name name}]
    [stack {:stack hand
            :mode :hand
            :visible? false
            :size :xs
            :extra-classes "mt-2"}]]
   [stack {:stack table
           :mode :hand
           :extra-classes "mt-3"
           :order :by-order}]])

(defn menu
  [{:keys [name uid]}]
  [:div {:class "flex-initial flex flex-col flex-no-wrap items-center justify-start pt-10"
         :style {:width "24rem"}}
   [name-tag {:name name :editable? true :uid uid}]
   [:button {:class "bg-green-400 text-green-100 hover:bg-green-600 text-white font-bold py-2 px-4 rounded shadow mt-4 w-48"
             :on-click (fn [_] (ws/send! [:game/deal]))}
     "Deal (4)"]
   [:button {:class "bg-blue-400 text-blue-100 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded shadow  mt-4 w-48"
             :on-click (fn [_] (ws/send! [:game/shuffle]))}
     "Shuffle all cards"]
   [:button {:class "bg-red-400 text-red-100 hover:bg-red-600 text-white font-bold py-2 px-4 rounded shadow  mt-4 w-48"
             :on-click (fn [_] (when (js/confirm "Reset the game? All points will be reset!")
                                 (ws/send! [:game/reset])))}
     "Reset game"]])

(defn the-deck
  [{:keys [deck discarded uid]}]
  [:div {:class "flex-initial flex flex-row flex-no-wrap items-center justify-center"
         :style {:width "24rem"}}
   [stack {:stack deck
           :mode :deck
           :visible? false
           :extra-classes ""
           :size :base
           :stack-action (fn [_] (ws/send! [:game/draw-card {:player-id uid}]))}]
   [stack {:stack discarded
           :mode :deck
           :visible? false
           :size :base
           :extra-classes ""
           :back-color "text-gray-700"}]])

(defn hand
  [{:keys [player uid]}]
  (let [{:keys [hand table]} player]
    [:div {:class "flex-grow flex flex-col flex-no-wrap items-center justify-end"}
     [stack {:stack table
             :mode :hand
             :visible? true
             :size :base
             :extra-classes "mb-3"}]
     [:div {:class "flex flex-row flex-no-wrap items-center justify-center p-4 bg-gray-200 rounded-t-lg"
            :style {:min-width "50%"}}
      [stack {:stack hand
              :mode :hand
              :visible? true
              :size :lg
              :order :by-rank
              :card-action (fn [_ card] (ws/send! [:game/play-card {:player-id uid :card card}]))}]]]))

(defn top
  [{:keys [players uid]}]
  [:div {:class "flex-1 flex flex-row flex-wrap items-start justify-around"}
   (for [[id p] (dissoc players uid)]
     ^{:key id} [player p])])

(defn bottom
  [{:keys [uid players deck discarded]}]
  (let [player (get players uid)]
    [:div {:class "flex-1 flex flex-row flex-no-wrap items-stretch justify-around"}
     [menu {:name (get player :name)
            :uid uid}]
     [hand {:player player
            :uid uid}]
     [the-deck {:deck deck
                :discarded discarded
                :uid uid}]]))

(defn root
  []
  (let [uid (:uid @state)
        {:keys [players deck discarded]} (:game @state)]
    [:div {:class "h-screen w-screen bg-gray-100 flex flex-col flex-no-wrap items-stretch justify-start"}
     [top {:players players
           :uid uid}]
     [bottom {:players players
              :uid uid
              :deck deck
              :discarded discarded}]]))
