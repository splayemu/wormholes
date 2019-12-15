(ns app.resolvers
  (:require
   [app.util :as util :refer [inspect]]
   [app.state :as state]
   [taoensso.timbre :as log]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]))

(defn user-room-ident->room-ident [user-room-ident]
  [:room/by-id (-> user-room-ident :user-room/id second)])

(comment
  (user-room-ident->room-ident {:user-room/id [:meow :room-id]})

  )

(defn push-merge-query
  "Pushes a merge query to the clients.

  Never push the base-state up."
  [push-fn user-id query data]
  (when (not= user-id :base-state)
    (let [merge-query-push {:message/topic :merge
                            :merge/query   query
                            :merge/data    data}]
      (push-fn user-id [:api/server-push {:topic :merge
                                         :msg   merge-query-push}]))))

(defn nil-ident? [ident]
  (-> ident second nil?))

(defn broadcast-result
  "Decorate a resolver to broadcast the query result using ident-fn to grab the
  ident from the resolver's return data. "
  [ident-fn]
  (fn [{::pc/keys [resolve output] :as resolver}]
    (log/info "broadcast-result associng ")
    (assoc resolver
      ::pc/resolve
      (fn [{:keys [push] :as env} params]
        (let [server-result (resolve env params)
              user-id       (-> env :user :user/id)
              ident         (ident-fn server-result)
              data          {ident server-result}

              _ (def tsr server-result)]
          (if (nil-ident? ident)
            (log/info user-id "Unable to broadcast to nil ident:" ident)
            (do 
              (log/info user-id "Broadcasting to ident:" ident)
              (push-merge-query push user-id output data)))
          server-result)))))

(pc/defresolver room-resolver [env {user-room-id :user-room/id}]
  {::pc/input #{:user-room/id}
   ::pc/output [:room/id
                :user-room/id
                :room/items
                {:room/neighbors [:room/id]}
                :wormhole/status
                :wormhole/connected]
   ;;::pc/transform (broadcast-result (fn [res] [:room/by-id (:room/id res)]))
   }
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

