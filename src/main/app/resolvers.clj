(ns app.resolvers
  (:require
   [app.util :as util :refer [inspect]]
   [app.state :as state]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]))

(pc/defresolver room-resolver [env {room-id :room/id user-id :user/id}]
  {::pc/input #{:room/id :user/id}
   ::pc/output [:room/id
                :room/items
                {:room/neighbors [:room/id]}
                :wormhole/status
                :wormhole/connected]}
  (def troom room-id)
  (def tuser user-id)
  (get-in @state/room-table [user-id room-id]))

;; for some reason the user-id isn't resolving
(pc/defresolver room-resolver2 [env {room-id :room/id}]
  {::pc/input #{:room/id}
   ::pc/output [:room/id
                :room/items
                {:room/neighbors [:room/id]}
                :wormhole/status
                :wormhole/connected]}
  (def troom room-id)
  (get-in @state/room-table [:base-state room-id]))

(comment
  (get-in @state/room-table [:base-state :room.id/starting])

  )

;; the goal here was to extract the user id from the ring env
(pc/defresolver user-resolver [env input]
  {::pc/output [:user/id]}
  (do
    (util/log "meow")
    {:user/id :base-state}))


(pc/defresolver initial-state-resolver [env input]
  {::pc/input #{:user/id}
   ::pc/output [{:starting-state [:room/id
                                  :user/id
                                  ;;{:neighbors [:room/id :room/neighbors]}
                                  ]}]}
  (do
    (util/log "test")
    {:starting-state {:room/id :room.id/starting
                      :user/id :base-state
                      ;;:neighbors [{:room/id :room.id/starting}]
                      }}))

(def resolvers
  [room-resolver
   room-resolver2
   initial-state-resolver
   user-resolver])

