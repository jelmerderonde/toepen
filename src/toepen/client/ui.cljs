(ns toepen.client.ui
  (:require [toepen.client.state :refer [state]]
            [toepen.client.ws :as ws]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

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
  [{:keys [stack mode visible? size back-color extra-classes card-action stack-action]
    :or {mode :stack ; stack, hand or deck
         size :base ; :xs, :sm, :base, :lg
         visible? true
         back-color "text-blue-800"
         extra-classes ""
         card-action (fn [_ _] nil)
         stack-action (fn [_] nil)}}]
  (let [cards (into [] (:cards stack))
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
          [:input {:class "bg-gray-200 rounded-lg p-2 shadow m-4 text-lg font-medium outline-none focus:shadow-outline focus:bg-blue-100"
                   :type :text
                   :value @new-name
                   :placeholder "Player name"
                   :on-change #(reset! new-name (-> % .-target .-value))
                   :on-blur #(do (send))
                   :on-key-down #(case (.-which %)
                                   13 (send)
                                   nil)
                   :autoFocus true}]
          [:span {:class "bg-white rounded-lg p-2 shadow m-4 text-lg font-medium select-none cursor-pointer"
                  :on-click #(when editable? (swap! editing? not))}
                 name])))))

(defn player
  [{:keys [name hand table]}]
  [:div {:class "flex-1 flex flex-col flex-no-wrap items-center justify-around"}
   [name-tag {:name name}]
   [stack {:stack hand
           :mode :hand
           :visible? false
           :size :xs
           :extra-classes "mb-4"}]
   [stack {:stack table
           :mode :stack
           :extra-classes "mb-4"}]])

(defn middle-of-table
  [{:keys [deck discarded]}]
  [:div {:class "flex-1 flex flex-row flex-no-wrap items-center justify-center"}
   [:div {:class "flex flex-col flex-no-wrap items-stretch"}
    [:button {:class "bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded my-4"
              :on-click (fn [_] (ws/send! [:game/deal]))}
      "Deal (4)"]
    [:button {:class "bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded my-4"
              :on-click (fn [_] (ws/send! [:game/reset]))}
      "Reset game"]]
   [stack {:stack deck
           :mode :deck
           :visible? false
           :extra-classes "m-6"
           :size :base}]
   [stack {:stack discarded
           :mode :deck
           :visible? false
           :size :base
           :extra-classes "m-6"
           :back-color "text-gray-700"}]])

(defn dashboard
  [{:keys [player uid]}]
  (let [{:keys [name hand table]} player]
    [:div {:class "flex-grow flex flex-col flex-no-wrap items-stretch justify-start"}
     [:div {:class "flex flex-row flex-no-wrap items-start justify-start"}
      [name-tag {:name name :editable? true :uid uid}]]
     [:div {:class "flex-1 flex flex-col flex-no-wrap items-center justify-around"}
      [stack {:stack table
              :mode :stack
              :visible? true
              :size :base
              :extra-classes "mb-4"}]
      [stack {:stack hand
              :mode :hand
              :visible? true
              :size :lg
              :card-action (fn [_ card] (ws/send! [:game/play-card {:player-id uid :card card}]))
              :extra-classes "mb-4"}]]]))


(defn root
  []
  (let [uid (:uid @state)
        {:keys [players deck discarded]} (:game @state)]
    [:div {:class "h-screen w-screen bg-gray-100 flex flex-col flex-no-wrap items-stretch justify-start"}
     [:div {:class "flex-1 flex flex-row flex-no-wrap items-stretch justify-around"}
      (for [[id p] (dissoc players uid)]
        ^{:key id} [player p])]
     [middle-of-table {:deck deck
                       :discarded discarded}]
     [dashboard {:player (get-in @state [:game :players uid])
                 :uid uid}]]))
