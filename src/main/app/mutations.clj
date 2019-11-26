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
;; between two rooms
;; persists until one of the two browser tabs closes

;; Creating and Finalizing a connection
;; user actions to get a connection
;; user clicks on center room
;; user clicks on outside room
;;   client1 sends to remote
;;   remote initializes connection
;;   client2 comes up in new tab (does javascript run on a tab in the background? (yes))
;;   client2 initializes websocket (but how does server know that the new client is the correct one?)
;;     first thing each client can do is send down an "finalize-connection" mutation with the room id
;;     if there is a connection waiting for a specific room, finalize connection can finalize it

;; Finalized Connections
;; Clicking on a wormhole with a finalized connection does nothing
;; The only way to break the wormhole is to close one of the browser tabs
;; For that we need to listen to sente's events
;; How are we going to tie a sente "end event" to a specific connection?
;;   Can we add the pathom env above the sente layer?
;; How are we going to trigger a backend pathom mutation?
;; 
;;

;; TODO:
;; [x] flesh out a connection model as well
;; [x] connection storage
;; [x] creating finalized connections
;; [x] updating wormhole state to include finalized connections
;; [ ] pushing connections to other clients
;; [ ] breaking connections
;; [ ] pushing broken connections to the client

(defn initialize-room-connection [from-room to-room-id]
  (-> from-room
    (assoc :wormhole/connection to-room-id)
    (assoc :wormhole/status :wormhole.status/connecting)))

(defn initialize-two-way-connection
  [room-table-map
   user-id
   {:keys [connection/from connection/to]}]
  (-> room-table-map
    (update-in [user-id from] initialize-room-connection to)
    (update-in [user-id to] initialize-room-connection from)))

(comment
  (initialize-room-connection
    {:room/id :from}
    :to)

  (initialize-two-way-connection
    {:user-id
     {:room-one {:room/id :room-one}
      :room-two {:room/id :room-two}}}
    :user-id
    {:connection/from :room-one
     :connection/to :room-two})

  (swap! state/room-table initialize-two-way-connection "5dd59ac0-ced9-497e-b67d-2c2d067b9179" {:connection/from :room.id/starting :connection/to :room.id/down})

  )

(pc/defmutation initialize-connection
  [env
   {:keys [connection/from
           connection/to]
    :as params}]
  {::pc/sym `initialize-connection}
  (let [user-id (-> env :user :user/id)]
    (log/info "Initializing connection" from " to " to)
    (swap! state/room-table initialize-two-way-connection user-id params)
    {:room/id to}))

(defn connection-waiting? [room-table-map room-ident-from room-ident-to]
  (let [room-from (get-in room-table-map room-ident-from)
        room-to   (get-in room-table-map room-ident-to)]
    (and
      (= (:wormhole/connection room-from) (:room/id room-to))
      (= (:wormhole/connection room-to) (:room/id room-from))
      (= (:wormhole/status room-from)
        (:wormhole/status room-to)
        :wormhole.status/connecting))))

(defn connection->connected [room-table-map room-ident-from room-ident-to]
  (-> room-table-map
    (update-in room-ident-from #(assoc % :wormhole/status :wormhole.status/connected))
    (update-in room-ident-to #(assoc % :wormhole/status :wormhole.status/connected))))

(defn get-connected-room [room-table-map user-id room-id]
  (let [connected-room-id (get-in room-table-map [user-id room-id :wormhole/connection])]
    (get-in room-table-map [user-id connected-room-id])))

(defn confirm-connection* [room-table-map user-id room-id]
  (def troom-table-map room-table-map)
  (def tuser-id user-id)
  (def troom-id room-id)
  (let [connected-room-id (:room/id (inspect (get-connected-room room-table-map user-id room-id)))
        connection-waiting?* (connection-waiting?
                               room-table-map
                               [user-id connected-room-id]
                               [user-id room-id])]
    (if (and (inspect connected-room-id) (inspect connection-waiting?*))
      (connection->connected room-table-map [user-id room-id] [user-id connected-room-id])
      room-table-map)))

(comment

  (def two-way-room-map

    (initialize-two-way-connection
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

(defn broadcast-result [component]
  (fn [{::pc/keys [mutate] :as mutation}]
    (assoc mutation
      ::pc/mutate
      (fn [env params]
        (let [res (mutate env params)]
          (log/info "broadcast-result-mutation" (component) res)
          res)))))

(comment (:displayName app.components/Room))

(pc/defmutation confirm-connection
  [env
   {:keys [connection/room-id]
    :as params}]
  {::pc/sym `confirm-connection
   ;; broadcasts before querying for the room data
   ::pc/transform (broadcast-result app.components/Room)
   }
  (let [user-id (-> env :user :user/id)]
    (def tenv env)
    (log/info "Confirming connection" user-id room-id)
    (swap! state/room-table confirm-connection* user-id room-id)
    {:room/id room-id}))


(comment
  (user-room-ident->room-ident
    [:user-room/id ["a6e5d6b6-7e27-4306-82fd-825c1f8ed687" :room.id/down]])

  )

(defn break-connection* [room-table-map user-id room-id]
  room-table-map)

(comment
  ;; update the wormhole status
  (swap! state/room-table update-in ["a6e5d6b6-7e27-4306-82fd-825c1f8ed687" :room.id/down] assoc :wormhole/status :wormhole.status/active)

  (get-in @state/room-table ["a6e5d6b6-7e27-4306-82fd-825c1f8ed687" :room.id/down])

  (swap! state/room-table break-connection* "5dd59ac0-ced9-497e-b67d-2c2d067b9179" :room.id/down)

  (require '[app.parser])

  ;; how do we pass the query down?
  ;; perhaps using merge component isn't as good and instead we should just pass the query
  ;; down as data?
  ((:parser tenv)

    {:user {:user/id "d546db3a-7bc5-4fda-85e2-daefe73e779f"}}
   ;;[{[:room/id :room.id/down] [:room/id :wormhole/status]}]
   
    [{[:room/id :room.id/starting] [:room/id
                                    :user-room/id
                                    :room/items
                                    {:room/neighbors [:room/id]}
                                    :wormhole/status
                                    :wormhole/connected]}]
   )

  (app.parser/pathom-parser
   {:user {:user/id :base-state}}
   [{[:room/id :room.id/down] [:room/id :wormhole/status]}]
   ;;[{[:room/id :room.id/starting] [:room/id
   ;;                                :user-room/id
   ;;                                :room/items
   ;;                                {:room/neighbors [:room/id]}
   ;;                                :wormhole/status
   ;;                                :wormhole/connected]}]
   )


  )

(pc/defmutation break-connection
  [env
   {:keys [connection/room-id]
    :as params}]
  {::pc/sym `break-connection}
  (let [user-id (-> env :user :user/id)]
    (log/info "Breaking connection" user-id room-id)
    (swap! state/room-table break-connection* user-id room-id)))

(def mutations [initialize-connection confirm-connection break-connection])
