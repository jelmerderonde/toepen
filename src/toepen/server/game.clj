(ns toepen.server.game
  (:require [toepen.server.cards :as c]))

(defn new-game
  "Creates a new game state"
  []
  {:deck (c/stack c/toep-cards)
   :discarded (c/stack)
   :players {}})

(defn new-player
  "Creates a new empty player state"
  []
  {:name "Unnamed player"
   :hand (c/stack)
   :table (c/stack)
   :points 0})

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
  (assoc-in game [:players player-id] (new-player)))

(defn remove-player
  "Removes a player from the game and
  moves their cards to the discarded pile"
  [game player-id]
  (if (player-exists? game player-id)
    (-> game
        (c/move-all-cards [:players player-id :hand] [:discarded])
        (c/move-all-cards [:players player-id :table] [:discarded])
        (update :players dissoc player-id))
    game))

(defn set-player-name
  "Updates display name of the player"
  [game player-id new-name]
  (assoc-in game [:players player-id :name] new-name))

(defn deal-cards
  "Deal `n` cards from the deck to all
  the players."
  [game n]
  (let [player-ids (player-ids game)
        draw-cards (fn draw-cards [game player-id]
                     (c/draw-cards game [:deck] [:players player-id :hand] n))]
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
  player table."
  [game player-id card]
  (if (player-exists? game player-id)
    (c/move-card game
      [:players player-id :hand]
      [:players player-id :table]
      card)
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

(defn finish-round
  "Finishes the round by moving all (un)played
  cards to the discarded pile."
  [game]
  (let [player-ids (player-ids game)
        discard-cards (fn discard-cards [game player-id]
                        (-> game
                            (c/move-all-cards
                              [:players player-id :hand]
                              [:discarded])
                            (c/move-all-cards
                              [:players player-id :table]
                              [:discarded])))]
    (reduce discard-cards game player-ids)))

(defn clean-table
  "Sets up a next round by finishing the
  current round and cleaning the table."
  [game]
  (-> game
      (finish-round)
      (c/move-all-cards [:discarded] [:deck])))

(defn reset-game
  "Resets the game by resetting all the points
  and setting up a next round."
  [game]
  (let [player-ids (player-ids game)
        reset-players (fn reset-players [game player-id]
                        (-> game
                            (update-in [:players player-id :hand] c/remove-all-cards)
                            (update-in [:players player-id :table] c/remove-all-cards)
                            (assoc-in [:players player-id :points] 0)))]
    (-> (reduce reset-players game player-ids)
        (assoc-in [:deck] (c/stack c/toep-cards))
        (assoc-in [:discarded] (c/stack)))))


(comment
  (-> (new-game)
      (add-player :a)
      (add-player :b)
      (add-player :c)
      (deal-cards 4)
      (play-rand-card :a)
      (play-rand-card :a)
      (play-rand-card :a)
      (play-rand-card :a)
      (play-rand-card :b))

  nil)


