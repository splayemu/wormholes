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
  (get-in @state/room-table [user-id room-id]))

(def people-table
  (atom
   {1 {:person/id 1 :person/name "Sally" :person/age 32}
    2 {:person/id 2 :person/name "Joe" :person/age 22}
    3 {:person/id 3 :person/name "Fred" :person/age 11}
    4 {:person/id 4 :person/name "Bobby" :person/age 55}}))

(def list-table
  (atom
   {:friends {:list/id     :friends
              :list/label  "Friends"
              :list/people [1 2]}
    :enemies {:list/id     :enemies
              :list/label  "Enemies"
              :list/people [4 3]}}))

(pc/defresolver person-resolver [env {:person/keys [id]}]
  {::pc/input #{:person/id}
   ::pc/output [:person/name :person/age]}
  (get @people-table id))

(pc/defresolver list-resolver [env {:list/keys [id]}]
  {::pc/input #{:list/id}
   ::pc/output [:list/label {:list/people [:person/id]}]}
  (when-let [list (get @list-table id)]
    (assoc list
           :list/people (mapv (fn [id] {:person/id id}) (:list/people list)))))

#_(pc/defresolver neighbor-resolver [env {:keys [room/id]}]
  {::pc/input #{:room/id}
   ::pc/output [{:neighbors [:room/id]}]}
  )

(pc/defresolver initial-state-resolver [env input]
  {::pc/output [{:starting-state [:room/id
                                  :user/id
                                  ;;{:neighbors [:room/id :room/neighbors]}
                                  ]}]}
  {:starting-state {:room/id :room.id/starting
                    :user/id :base-state
                    ;;:neighbors [{:room/id :room.id/starting}]
                    }})

(pc/defresolver friends-resolver [env input]
  {::pc/output [{:friends [:list/id]}]}
  {:friends {:list/id :friends}})

(pc/defresolver enemies-resolver [env input]
  {::pc/output [{:enemies [:list/id]}]}
  {:enemies {:list/id :enemies}})

(def resolvers
  [room-resolver
   initial-state-resolver])

