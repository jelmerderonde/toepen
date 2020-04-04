(ns toepen.client.core
  (:require [reagent.dom :as rdom]
            [toepen.client.ui :as ui]
            [toepen.client.state :as state]))

(rdom/render [ui/root] (js/document.getElementById "app"))
(state/refresh-state)
