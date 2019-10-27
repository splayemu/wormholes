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

(defn activate-wormhole [room]
  (merge room {:wormhole/status :wormhole.status/active}))

(def deactive-status?
  #{:wormhole.status/deactive :wormhole.status/hover})

(defn can-open-wormhole? [state clicked-id]
  (let [{clicked-status :wormhole/status} (get-in state (room-ident clicked-id))
        {starting-status :wormhole/status} (get-in state (room-ident starting-room-id))]
    (cond
      (and
        (= clicked-id starting-room-id)
        (deactive-status? starting-status))
      true

      (and
        (= starting-status :wormhole.status/active)
        (deactive-status? clicked-status))
      true

      :else false)))

(defn toggle-room-wormhole
  [state clicked-id]
  (if (can-open-wormhole? state clicked-id)
    (update-in state (room-ident clicked-id) activate-wormhole)
    (assoc-in state (conj (room-ident clicked-id) :wormhole/status) :wormhole.status/deactive)))

(comment
  (toggle-room-wormhole
    {:wormhole/status :wormhole.status/deactive}
    {:wormhole/status :wormhole.status/deactive})

  )

(defmutation click-wormhole
  "Mutation: "
  [{:keys [room/id] :as params}]
  (action [{:keys [state]}]
    (js/console.log "clicked" params)
    (swap! state toggle-room-wormhole id)))

(defn enter-room-wormhole [state room-id]
  (let [hover-room-ident (room-ident room-id)
        {:keys [wormhole/status]} (get-in state hover-room-ident)]
    (if (can-open-wormhole? state room-id)
      (assoc-in state (conj hover-room-ident :wormhole/status) :wormhole.status/hover)
      state)))

(defn leave-room-wormhole [state room-id]
  (let [hover-room-ident (room-ident room-id)
        {:keys [wormhole/status]} (get-in state hover-room-ident)]
    (if (= status :wormhole.status/hover)
      (assoc-in state (conj hover-room-ident :wormhole/status) :wormhole.status/deactive)
      state)))

(defmutation mouse-enter-wormhole
  "Mutation: "
  [{:keys [room/id] :as params}]
  (action [{:keys [state]}]
    (js/console.log "mouseon" params)
    (swap! state enter-room-wormhole id)))

(defmutation mouse-leave-wormhole
  "Mutation: "
  [{:keys [room/id] :as params}]
  (action [{:keys [state]}]
    (js/console.log "mouseoff" params)
    (swap! state leave-room-wormhole id)))

