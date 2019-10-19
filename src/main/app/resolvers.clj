(ns app.resolvers
  (:require
   [app.util :as util :refer [inspect]]
   [app.state :as state]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]))

(pc/defresolver room-resolver [env {user-room-id :user-room/id}]
  {::pc/input #{:user-room/id}
   ::pc/output [:room/id
                :user-room/id
                :room/items
                {:room/neighbors [:room/id]}
                :wormhole/status
                :wormhole/connected]}
  (do 
    (util/log "room-resolver" user-room-id)
    (get-in @state/room-table user-room-id)))

(comment
  (get-in @state/room-table ["1bdeac6c-3922-4deb-8b94-793c4a340766" :room.id/starting])

  )



#_(pc/defresolver room-resolver [env input]
  {::pc/input #{:room/id}
   ::pc/output [:room/id
                :room/status]}
  (util/log "calling room-resolver")
  (let [user-id (:user/id (:user env))]
    (get-in @state/room-table [user-id (:room/id input)])))

(pc/defresolver user-resolver [env input]
  {::pc/output [:user]}
  {:user (:user env)})

(pc/defresolver user-room-resolver [env {:keys [room/id]}]
  {::pc/input #{:room/id}
   ::pc/output [:user-room/id]}
  (util/log "user-room-resolver" id)
  (let [user-id (-> env :user :user/id)]
    {:user-room/id [user-id id]}))

(pc/defresolver initial-state-resolver [env {:room/keys [id neighbors]}]
  {::pc/input #{:room/id :room/neighbors}
   ::pc/output [{:starting-state [{:center-room [:room/id]}
                                  {:neighbors [:room/id]}]}]}
  (do
    (util/log "test")
    {:starting-state {:center-room {:room/id id}
                      ;;:neighbors [{:room/id :room.id/starting}]
                      }}))

(def resolvers
  [room-resolver
   initial-state-resolver
   user-room-resolver
   user-resolver])

