(ns app.mutations
  (:require
   [app.util :as util :refer-macros [inspect]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defn item-ident [id]
  [:item/by-id id])

(defn room-ident [id]
  [:room/by-id id])

(def starting-room-id :room.id/starting)

(def starting-room-ident
  (room-ident starting-room-id))

;; update other room
;; update starting room

(defn activate-wormhole [room]
  (merge room {:wormhole/status :wormhole.status/active}))

(defn toggle-room-wormhole
  "asd"
  [state clicked-id]
  (let [{clicked-status :wormhole/status} (get-in state (room-ident clicked-id))
        {starting-status :wormhole/status} (get-in state (room-ident starting-room-id))]
    (cond
      (and
        (= clicked-id starting-room-id)
        (= starting-status :wormhole.status/deactive))
      (update-in state (room-ident starting-room-id) activate-wormhole)

      (and
        (= starting-status :wormhole.status/active)
        (= clicked-status :wormhole.status/deactive))
      (-> state
        (update-in (room-ident clicked-id) activate-wormhole))

      :else
      (assoc-in state (conj (room-ident clicked-id) :wormhole/status) :wormhole.status/deactive)))
  )

(comment
  (toggle-room-wormhole
    {:wormhole/status :wormhole.status/deactive}
    {:wormhole/status :wormhole.status/deactive})

  )

;; if clicked on main room
;; if clicked on other room

(defmutation click-wormhole
  "Mutation: "
  [{:keys [room/id] :as params}]
  (action [{:keys [state]}]
    (js/console.log "clicked" params)
    (swap! state toggle-room-wormhole id)))

