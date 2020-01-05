(ns app.mutations
  (:require
   [app.util :as util :refer [inspect]]
   [app.state :as state]
   [com.wsscode.pathom.connect :as pc]
   [com.fulcrologic.fulcro.components :as comp]
   [taoensso.timbre :as log]))

;; duplicating in mutations.cljs
(defn item-ident [id]
  [:item/by-id id])

(defn room-ident [id]
  [:room/by-id id])

;; Properties of a connection
;; unconnected
;;   (hover)
;; connecting
;; between two rooms
;; persists until one of the two browser tabs closes

;; TODO:
;; [x] flesh out a connection model as well
;; [x] connection storage
;; [x] creating finalized connections
;; [x] updating wormhole state to include finalized connections
;; [x] pushing connections to other clients
;; [ ] breaking connections
;; [ ] pushing broken connections to the client

(defn connect-wormhole [status]
  ;; primarily toggles
  (case status
    :wormhole.status/connecting :wormhole.status/connected
    :wormhole.status/connected  :wormhole.status/connected
    :wormhole.status/connecting))

(defn initialize-room-connection [from-room to-room-id]
  (let [dont-overwrite-connection #(if (#{:wormhole.connection/awaiting nil} to-room-id)
                                     %
                                     to-room-id)]
    (-> from-room
      (update :wormhole/connection dont-overwrite-connection)
      (update :wormhole/status connect-wormhole))))

(defn connection-waiting? [room-table-map room-ident-from room-ident-to]
  (let [room-from (get-in room-table-map room-ident-from)
        room-to   (get-in room-table-map room-ident-to)]
    (and
      (= (:wormhole/connection room-from) (:room/id room-to))
      (= (:wormhole/connection room-to) (:room/id room-from))
      (or
        (= (:wormhole/status room-from) :wormhole.status/connected)
        (= (:wormhole/status room-to) :wormhole.status/connected)))))

(defn connection->connected [room-table-map room-ident-from room-ident-to]
  (log/info "confirming connection" room-ident-from room-ident-to)
  (-> room-table-map
    (update-in room-ident-from #(assoc % :wormhole/status :wormhole.status/connected))
    (update-in room-ident-to #(assoc % :wormhole/status :wormhole.status/connected))))

(defn get-connected-room [room-table-map user-id room-id]
  (let [connected-room-id (get-in room-table-map [user-id room-id :wormhole/connection])]
    (get-in room-table-map [user-id connected-room-id])))

(defn confirm-connection* [room-table-map user-id from-room-id to-room-id]
  (let [connected-room-id (if (not to-room-id)
                            (:room/id (get-connected-room room-table-map user-id from-room-id))
                            to-room-id)
        connection-waiting?* (connection-waiting?
                               room-table-map
                               [user-id from-room-id]
                               [user-id connected-room-id])]
    (if (and connected-room-id connection-waiting?*)
      (connection->connected room-table-map [user-id from-room-id] [user-id connected-room-id])
      room-table-map)))

;; can be passed a nil to as that means that it's either connecting or finalizing the connection
(defn initialize-connection*
  [room-table-map
   user-id
   {:keys [connection/from connection/to]}]
  (-> (if to
        (-> room-table-map
          (update-in [user-id from] initialize-room-connection to)
          (update-in [user-id to] initialize-room-connection from))
        (-> room-table-map
          (update-in [user-id from] initialize-room-connection :wormhole.connection/awaiting)))
    (confirm-connection* user-id from to)))

(comment
  (initialize-room-connection
    {:room/id :from}
    :to)

  (initialize-connection*
    {:user-id
     {:room-one {:room/id :room-one}
      :room-two {:room/id :room-two}}}
    :user-id
    {:connection/from :room-one
     :connection/to nil})

  (-> {:user-id
       {:room-one {:room/id :room-one}
        :room-two {:room/id :room-two}}}
    (initialize-connection* :user-id {:connection/from :room-one :connection/to nil})
    (initialize-connection* :user-id {:connection/from :room-two :connection/to :room-one}))

  (-> {:user-id
       {:room-one {:room/id :room-one}
        :room-two {:room/id :room-two}}}
    (initialize-connection* :user-id {:connection/from :room-one :connection/to :room-two})
    (initialize-connection* :user-id {:connection/from :room-two :connection/to nil}))




  (swap! state/room-table initialize-connection "5dd59ac0-ced9-497e-b67d-2c2d067b9179" {:connection/from :room.id/starting :connection/to :room.id/down})

  )

(pc/defmutation initialize-connection
  [env
   {:keys [connection/from
           connection/to]
    :as params}]
  {::pc/sym `initialize-connection}
  (let [user-id (-> env :user :user/id)]
    (log/info "Initializing connection" from "to" to)
    (swap! state/room-table initialize-connection* user-id params)))

(defn break-connection* [room-table-map user-id room-id]
  (let [connected-room (get-connected-room room-table-map user-id room-id)
        disconnect-room #(assoc %
                           :wormhole/status :wormhole.status/deactive
                           :wormhole/connection nil)]
    (-> room-table-map
     (update-in [user-id room-id] disconnect-room)
     (update-in [user-id (:room/id connected-room)] disconnect-room))))

(comment
  (let [connected-room-map (-> two-way-room-map
                             (confirm-connection* :user-id :room-one))]
    (break-connection* connected-room-map :user-id :room-one))

  ;; Test two way connection functionality
  (def two-way-room-map
    (initialize-connection*
      {:user-id
       {:room-one {:room/id :room-one}
        :room-two {:room/id :room-two}}}
      :user-id
      {:connection/from :room-one
       :connection/to :room-two}))

  (connection-waiting? two-way-room-map [:user-id :room-one] [:user-id :room-two])

  (get-connected-room two-way-room-map :user-id :room-one)

  (confirm-connection* two-way-room-map :user-id :room-one)



  (get-in @state/room-table ["5dd59ac0-ced9-497e-b67d-2c2d067b9179"])

  (swap! state/room-table confirm-connection* "5dd59ac0-ced9-497e-b67d-2c2d067b9179" :room.id/down)

  )

(comment
  ;; update the wormhole status
  (swap! state/room-table update-in ["a6e5d6b6-7e27-4306-82fd-825c1f8ed687" :room.id/down] assoc :wormhole/status :wormhole.status/active)

  (get-in @state/room-table ["a6e5d6b6-7e27-4306-82fd-825c1f8ed687" :room.id/down])

  (swap! state/room-table break-connection* "6ac96571-6009-42af-a3da-4f94b406daca" :room.id/down)

  )

(def mutations [initialize-connection])
