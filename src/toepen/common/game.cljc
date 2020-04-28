(ns toepen.common.game
  (:require [toepen.common.cards :as c]))

(defn new-game
  "Creates a new game state"
  []
  {:deck (assoc (c/stack c/toep-cards) :visible-for #{})
   :discarded (assoc (c/stack) :visible-for #{})
   :players {}})

(defn new-player
  "Creates a new empty player state"
  [player-id position]
  {:id player-id
   :name "Unnamed player"
   :hand (assoc (c/stack) :visible-for #{player-id})
   :table (c/stack)
   :position position
   :points 0
   :dealer? false
   :dirty? false
   :active? true})

(defn player-ids
  "Returns all player ids in the game"
  [game]
  (-> game :players keys))

(defn player-exists?
  "Checks if a player exists in the game."
  [game player-id]
  (contains? (:players game) player-id))

(defn add-player
  "Adds a new player to the game"
  [game player-id]
  (let [next-position (->> game
                           :players
                           vals
                           (map :position)
                           c/next-pos)]
    (-> game
        (assoc-in [:players player-id] (new-player player-id next-position))
        (assoc-in [:players player-id :dealer?] (= 0 next-position)))))

(defn is-dealer?
  "Checks if the player with this id is the dealer."
  [game player-id]
  (get-in game [:players player-id :dealer?] false))

(defn get-order
  "Returns the player-ids in the order of
  entering the game."
  [game]
  (->> game
       :players
       vals
       (map (juxt :id :position))
       (sort-by second)
       (map first)
       (vec)))

(defn remove-player
  "Removes a player from the game and
  moves their cards to the discarded pile."
  [game player-id]
  (if (player-exists? game player-id)
    (let [dealer? (is-dealer? game player-id)
          new-game (-> game
                       (c/move-all-cards [:players player-id :hand] [:discarded])
                       (c/move-all-cards [:players player-id :table] [:discarded])
                       (update :players dissoc player-id))]
      (if (and dealer? (seq (:players new-game)))
        (let [new-dealer (-> new-game :players ffirst)]
          (assoc-in new-game [:players new-dealer :dealer?] true))
        new-game))
    game))

(defn update-name
  "Updates display name of the player."
  [game player-id new-name]
  (assoc-in game [:players player-id :name] new-name))

(defn inc-points
  "Increments the points of the player."
  [game player-id]
  (update-in game [:players player-id :points] #(if (< % 15) (inc %) %)))

(defn dec-points
  "Increments the points of the player."
  [game player-id]
  (update-in game [:players player-id :points] #(if (> % 0) (dec %) %)))

(defn dirty-laundry?
  "Returns true if the player has claimed
  dirty laundry."
  [game player-id]
  (get-in game [:players player-id :dirty?] false))

(defn show-hand-to
  "Shows the hand of the from player
   to the to player."
  [game player-id-from player-id-to]
  (if (dirty-laundry? game player-id-from)
    (update-in game [:players player-id-from :hand :visible-for] conj player-id-to)
    game))

(defn reset-visibility
  "Resets visibility so cards are only
  visible for the player."
  [game player-id]
  (assoc-in game [:players player-id :hand :visible-for] #{player-id}))

(defn claim-dirty
  "Claims dirty laundry for the player."
  [game player-id]
  (assoc-in game [:players player-id :dirty?] true))

(defn cancel-dirty
  "Cancels dirty laundry for the player."
  [game player-id]
  (-> game
      (assoc-in [:players player-id :dirty?] false)
      (reset-visibility player-id)))

(defn deactivate
  "Deactivates the player."
  [game player-id]
  (assoc-in game [:players player-id :active?] false))

(defn activate
  "Activates the player."
  [game player-id]
  (assoc-in game [:players player-id :active?] true))

(defn active?
  "Checks if the player is active"
  [game player-id]
  (get-in game [:players player-id :active?]))

(defn activate-living-players
  "Loops over all players and sets players
  with points below 15 to active."
  [game]
  (let [player-ids (player-ids game)
        activate-player (fn [game player-id]
                          (let [points (get-in game [:players player-id :points])]
                            (if (< points 15)
                              (activate game player-id)
                              (deactivate game player-id))))]
    (reduce activate-player game player-ids)))

(defn deal-cards
  "Deal `n` cards from the deck to all
  the active players."
  [game n]
  (let [player-ids (player-ids game)
        draw-cards (fn draw-cards [game player-id]
                     (if (active? game player-id)
                       (c/draw-cards game [:deck] [:players player-id :hand] n)
                       game))]
    (reduce draw-cards game player-ids)))

(defn draw-card
  "Draws a new card from the deck for the
  player."
  [game player-id]
  (if (player-exists? game player-id)
    (c/draw-card game
      [:deck]
      [:players player-id :hand])
    game))

(defn play-card
  "Moves a card from player hand to
  player table. Also resets visibility"
  [game player-id card]
  (if (player-exists? game player-id)
    (-> game
        (c/move-card
          [:players player-id :hand]
          [:players player-id :table]
          card)
        (reset-visibility player-id))
    game))

(defn play-rand-card
  "Playes a random card for the player.
  Only useful for testing."
  [game player-id]
  (if (player-exists? game player-id)
    (let [card (c/rand-card (get-in game [:players player-id :hand]))]
      (if card
        (play-card game player-id card)
        game))
    game))

(defn discard-hand
  "Discards the hand of the player. Also
  resets visibilit status and dirty
  laundry status."
  [game player-id]
  (-> game
      (c/move-all-cards
        [:players player-id :hand]
        [:discarded])
      (reset-visibility player-id)
      (cancel-dirty player-id)))

(defn shuffle-cards
  "Shuffles all (un)played cards back into
  the deck."
  [game]
  (let [player-ids (player-ids game)
        discard-cards (fn discard-cards [game player-id]
                        (-> game
                            (c/move-all-cards
                              [:players player-id :hand]
                              [:deck])
                            (c/move-all-cards
                              [:players player-id :table]
                              [:deck])))
        game (c/move-all-cards game [:discarded] [:deck])]
    (reduce discard-cards game player-ids)))

(defn shuffle-and-deal
  "Sets up a next round by shuffling the cards,
  activting/deactivating players and dealing
  4 cards to active players."
  [game]
  (-> game
      (shuffle-cards)
      (activate-living-players)
      (deal-cards 4)))

(defn reset-game
  "Resets the game by resetting all the points
  and setting up a next round."
  [game]
  (let [player-ids (player-ids game)
        reset-players (fn reset-players [players player-id]
                        (-> players
                            (update-in [player-id :hand] c/remove-all-cards)
                            (update-in [player-id :table] c/remove-all-cards)
                            (assoc-in [player-id :points] 0)
                            (assoc-in [player-id :hand :visible-for] #{player-id})
                            (assoc-in [player-id :dirty?] false)
                            (assoc-in [player-id :active?] true)))
        players (reduce reset-players (:players game) player-ids)]
    (assoc (new-game) :players players)))

(comment
  (-> (new-game)
      (add-player :a)
      (add-player :b)
      (shuffle-and-deal)
      (doto tap>)
      (assoc-in [:players :a :points] 15)
      (shuffle-and-deal)
      (doto tap>))

  nil)


