(ns app.resolvers
  (:require
   [app.util :as util :refer [inspect]]
   [app.state :as state]
   [taoensso.timbre :as log]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]))

(pc/defresolver room-resolver [env {:keys [room/id]}]
  {::pc/input #{:room/id}
   ::pc/output [:room/id
                :room/items
                {:room/neighbors [:room/id]}
                :wormhole/status
                :wormhole/connected]}
  (let [user-id (-> env :user :user/id)]
    (util/log "room-resolver" [user-id id])
    (get-in @state/room-table [user-id id])))

(comment
  (get-in @state/room-table ["ce677006-71f7-4ee6-a63a-c6e4b8529e62" :room.id/starting])

  )

(pc/defresolver user-resolver [env input]
  {::pc/output [:user]}
  {:user (:user env)})

(pc/defresolver initial-state-resolver [env input]
  {::pc/output [:room-configuration]}
  (let [user-id (-> env :user :user/id)
        room-id (-> env :ast :params :center-room-id)
        room    (get-in @state/room-table [user-id room-id])

        {:keys [room/id room/neighbors]} room
        {:keys [up down left right]}     neighbors]
    {:room-configuration {:center-room {:room/id room-id}
                          :up-room     (when up {:room/id up})
                          :down-room   (when down {:room/id down})
                          :left-room   (when left {:room/id left})
                          :right-room  (when right {:room/id right})}}))



(def resolvers
  [room-resolver
   initial-state-resolver
   user-resolver])
