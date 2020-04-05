(ns toepen.server.cards)

(def toep-cards
  (let [suits [:heart :club :diamond :spade]
        cards [:jack :queen :king :1 :7 :8 :9 :10]]
    (for [s suits
          c cards]
      [s c])))

(defn stack
  ([]
   {:cards #{}})
  ([initial-cards]
   (assoc (stack) :cards (into #{} initial-cards))))

(defn add-card
  "Adds card to a stack"
  [stack card]
  (update stack :cards conj card))

(defn add-cards
  "Adds multiple cards to a stack"
  [stack cards]
  (reduce  add-card stack cards))

(defn remove-card
  "Removes a card from a stack"
  [stack card]
  (if ((:cards stack) card) ; check if card is in stack
    (update stack :cards disj card)
    stack))

(defn remove-cards
  "Removes multiple cards from a stack"
  [stack cards]
  (reduce remove-card stack cards))

(defn remove-all-cards
  "Removes all the cards from a stack"
  [stack]
  (assoc stack :cards #{}))

(defn rand-card
  "Picks a random card from a stack, returns
  nil if there are no cards in the stack."
  [stack]
  (if (seq (:cards stack))
    (rand-nth (into [] (:cards stack)))
    nil))

(defn rand-cards
  "Picks `n` random cards from a stack"
  [stack n]
  (let [c (count (:cards stack))
        coll (shuffle (into [] (:cards stack)))]
    (if (> c n)
      (take n coll)
      (take c coll))))

; -- `from` and `to` are always vectors indicating a path

(defn move-card
  "Moves the specified card from the stack at
  `from` to the stack at `to`"
  [state from to card]
  (-> state
      (update-in from remove-card card)
      (update-in to add-card card)))

(defn move-cards
  "Moves the specified cards from the stack at
  `from` to the stack at `to`"
  [state from to cards]
  (-> state
      (update-in from remove-cards cards)
      (update-in to add-cards cards)))

(defn move-all-cards
  "Moves all the cards from the stack at
  `from` to the stack at `to`"
  [state from to]
  (let [cards (get-in state (conj from :cards))]
    (move-cards state from to cards)))

(defn draw-card
  "Draws a random card from the stack at `from`
  and moves it to the stack at `to`."
  [state from to]
  (let [card (rand-card (get-in state from))]
    (if card
      (move-card state from to card)
      state)))

(defn draw-cards
  "Draws n random cards from the stack at `from`
  and moves them to the stack at `to`."
  [state from to n]
  (let [cards (rand-cards (get-in state from) n)]
    (move-cards state from to cards)))
