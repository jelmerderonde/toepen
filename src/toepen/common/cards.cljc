(ns toepen.common.cards)

(def toep-cards
  (let [suits [:heart :club :diamond :spade]
        cards [:jack :queen :king :1 :7 :8 :9 :10]]
    (for [s suits
          c cards]
      [s c])))

(defn next-pos
  "Determines the next insertion position
  for a stack."
  [{:keys [cards]}]
  (if (seq cards)
    (->> cards vals (apply max) inc)
    0))

(defn add-card
  "Adds card to a stack"
  [stack card]
  (if (contains? (:cards stack) card)
    stack
    (-> stack (assoc-in [:cards card] (next-pos stack))
              (update-in [:n] inc))))

(defn add-cards
  "Adds multiple cards to a stack"
  [stack cards]
  (reduce add-card stack cards))

(defn stack
  "Creates a new stack, optionally
  with initial cards"
  ([]
   {:n 0
    :cards {}})
  ([initial-cards]
   (add-cards (stack) initial-cards)))

(def ranks
  "Ranking for the suits and cards for
  sorting."
  {:suit {:spade 4
          :heart 3
          :club 2
          :diamond 1}
   :card {:10 8
          :9 7
          :8 6
          :7 5
          :1 4
          :king 3
          :queen 2
          :jack 1}})

(defn by-rank
  "Comparator suitable for sorting toep cards"
  [[suit1 card1] [suit2 card2]]
  (let [s1r (get-in ranks [:suit suit1])
        s2r (get-in ranks [:suit suit2])
        c1r (get-in ranks [:card card1])
        c2r (get-in ranks [:card card2])]
    (cond
      (> s1r s2r) -1
      (< s1r s2r) 1
      (> c1r c2r) -1
      (< c1r c2r) 1
      :else 0)))

(defn get-cards
  "Returns the cards from a stack. You can optionally pass
  :by-rank or :by-order to return the cards in the specified
  order."
  ([stack]
   (-> stack :cards keys))
  ([stack order]
   (case order
     :by-order (keys (sort-by val < (:cards stack)))
     :by-rank (sort by-rank (-> stack :cards keys))
     (get-cards stack))))

(defn remove-card
  "Removes a card from a stack"
  [stack card]
  (if (contains? (:cards stack) card) ; check if card is in stack
    (-> stack (update :cards dissoc card)
              (update :n dec))
    stack))

(defn remove-cards
  "Removes multiple cards from a stack"
  [stack cards]
  (reduce remove-card stack cards))

(defn remove-all-cards
  "Removes all the cards from a stack"
  [stack]
  (-> stack (assoc :cards {})
            (assoc :n 0)))

(defn rand-card
  "Picks a random card from a stack, returns
  nil if there are no cards in the stack."
  [stack]
  (let [cards (get-cards stack)]
    (if (seq cards)
      (rand-nth cards)
      nil)))

(defn rand-cards
  "Picks `n` random cards from a stack"
  [stack n]
  (let [cards (get-cards stack)
        c (count cards)
        coll (shuffle cards)]
    (if (> c n)
      (take n coll)
      (take c coll))))

; -- `from` and `to` are always vectors indicating a path

(defn move-card
  "Moves the specified card from the stack at
  `from` to the stack at `to`"
  [game from to card]
  (-> game
      (update-in from remove-card card)
      (update-in to add-card card)))

(defn move-cards
  "Moves the specified cards from the stack at
  `from` to the stack at `to`"
  [game from to cards]
  (-> game
      (update-in from remove-cards cards)
      (update-in to add-cards cards)))

(defn move-all-cards
  "Moves all the cards from the stack at
  `from` to the stack at `to`"
  [game from to]
  (let [cards (-> game
                  (get-in (conj from :cards))
                  keys)]
    (move-cards game from to cards)))

(defn draw-card
  "Draws a random card from the stack at `from`
  and moves it to the stack at `to`."
  [game from to]
  (let [card (rand-card (get-in game from))]
    (if card
      (move-card game from to card)
      game)))

(defn draw-cards
  "Draws n random cards from the stack at `from`
  and moves them to the stack at `to`."
  [game from to n]
  (let [cards (rand-cards (get-in game from) n)]
    (move-cards game from to cards)))
