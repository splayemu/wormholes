(ns app.mutations
  (:require
   [app.util :as util :refer-macros [inspect]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]))

;; duplicating in mutations.clj
(defn item-ident [id]
  [:item/by-id id])

(defn room-ident [id]
  [:room/by-id id])

(def starting-room-ident
  [:room-configuration :center-room])

(defn get-center-room [state]
  (get-in state (get-in state starting-room-ident)))

(defn activate-wormhole [room]
  (merge room {:wormhole/status :wormhole.status/active}))

(def active-status?
  #{:wormhole.status/connected})

(def deactive-status?
  #{:wormhole.status/deactive :wormhole.status/hover})

(defn room->wormhole-status [state-map clicked-id]
  (:wormhole/status (get-in state-map (room-ident clicked-id))))

(defn can-close-wormhole? [state-map clicked-id]
  (let [clicked-status (room->wormhole-status state-map clicked-id)]
    (not (active-status? clicked-status))))

(comment
  (let [state-map (assoc-in {} (room-ident :center-room)
                    {:room/id :center-room
                     :wormhole/status :wormhole.status/connected})]
    (can-close-wormhole? state-map :center-room))

  )

(defn can-open-wormhole? [state clicked-id]
  (let [{clicked-status :wormhole/status}  (get-in state (room-ident clicked-id))
        starting-room-id                   (:room/id (get-center-room state))
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

;; if center wormhole is activated and new wormhole is opened,
;; then we open the connection

(defmutation click-wormhole
  [{:keys [room/id] :as params}]
  (action [{:keys [state app]}]
    (js/console.log "clicked" params)
    (when (inspect (can-close-wormhole? @state id))
      (let [state-map    (swap! state toggle-room-wormhole id)
            center-room  (get-center-room state-map)
            clicked-room (get-in state-map (room-ident id))]
        (when (and (not= (:room/id center-room) (:room/id clicked-room))
                (= (:wormhole/status center-room) :wormhole.status/active)
                (= (:wormhole/status clicked-room) :wormhole.status/active))
          (comp/transact! app `[(app.mutations/initialize-connection
                                  {:connection/from ~(:room/id center-room)
                                   :connection/to ~(:room/id clicked-room)})])
          )))))

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


;; Connection mutations
(defmutation initialize-connection
  [{:keys [connection/from
           connection/to] :as params}]
  (action [{:keys [state]}]
    (js/console.log "initialze-connection" params))
  (remote [env] true))

(defmutation confirm-connection
  [{:keys [connection/room-id] :as params}]
  (action [{:keys [state]}]
    (js/console.log "confirm-connection" params))
  (remote [env] true))
