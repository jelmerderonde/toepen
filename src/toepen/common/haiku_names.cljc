(ns toepen.common.haiku-names
  (:require [clojure.string :as str]))

(def adjectives
  ["happy" "sad" "epic" "glorious" "proud" "generous" "aged" "ancient"
   "billowing" "bitter" "broken" "calm" "cool" "crimson" "curly" "damp"
   "dark" "delicate" "divine" "falling" "fancy" "fragrant" "frosty"
   "gentle" "hidden" "holy" "jolly" "lingering" "little" "lively" "lucky"
   "misty" "muddy" "mute" "nameless" "noisy" "odd" "patient" "plain"
   "polished" "quiet" "rapid" "restless" "rough" "royal" "shiny" "shy"
   "solitary" "sparkling" "chaotic" "sweet" "tiny" "wandering"
   "weathered" "white" "wild" "wispy" "withered"])

(def nouns
  ["pino" "paula" "tori" "pos" "amos" "jos" "voorzitter" "secretaris"
   "penningmeester" "vicevoorzitter" "manager" "scientist" "botanist"
   "biologist" "lecturer" "professor" "student"])

(def verbs
  ["prances" "toeps" "walks" "runs" "creates" "destroys" "eats" "draws"
   "codes" "drinks" "smiles" "laughs" "sings" "waves" "surfs" "accepts"
   "talks" "aims" "announces" "approves" "attempts" "becomes" "breaks"
   "builds" "bows" "calculates" "communicates" "claims" "complains"
   "dances" "desires" "deals" "disagrees" "explains" "experiments"
   "follows" "hesitates" "investigates" "jumps" "kneels" "looks"
   "manages" "negotiates" "overcomes" "postpones" "prevents" "prepares"
   "protects" "performes" "recommends" "relaxes" "remembers" "sanctions"
   "snores" "struggles" "survives" "threatens" "tries" "waits" "wins"])

(def adjectives2
  ["ably" "victoriously" "aggresively" "abashedly" "happily" "proudly"
   "gloriously" "extraordinarily" "necessarily" "unhandily" "daintily"
   "faultily" "prettily" "uneasily" "nastily" "sexily" "haughtily"
   "unhappily" "grouchily"])

(defn haiku
  []
  (->> [adjectives nouns verbs adjectives2]
       (map rand-nth)
       (map str/capitalize)
       (str/join)))
