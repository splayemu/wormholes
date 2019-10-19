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
  (get-in @state/room-table ["ce677006-71f7-4ee6-a63a-c6e4b8529e62" :room.id/starting])

  )

(pc/defresolver user-resolver [env input]
  {::pc/output [:user]}
  {:user (:user env)})

(pc/defresolver user-room-resolver [env {:keys [room/id]}]
  {::pc/input #{:room/id}
   ::pc/output [:user-room/id]}
  (let [user-id (-> env :user :user/id)]
    {:user-room/id [user-id id]}))

(pc/defresolver initial-state-resolver [env input]
  {::pc/output [:room-configuration]}
  (let [user-id (-> env :user :user/id) 
        room-id (-> env :ast :params :center-room-id)
        room    (get-in @state/room-table [user-id room-id])

        {:keys [room/id room/neighbors]} room
        {:keys [up down left right]}     neighbors]
    {:room-configuration {:center-room {:user-room/id [user-id room-id]}
                          :up-room     (when up {:user-room/id [user-id up]})
                          :down-room   (when down {:user-room/id [user-id down]})
                          :left-room   (when left {:user-room/id [user-id left]})
                          :right-room  (when right {:user-room/id [user-id right]})}}))



(def resolvers
  [room-resolver
   initial-state-resolver
   user-room-resolver
   user-resolver])

