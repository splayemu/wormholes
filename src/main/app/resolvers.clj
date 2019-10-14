(ns app.resolvers
  (:require
   [app.util :refer [inspect]]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]))

;; starting state
;; wormhole.statuss
#{:wormhole.status/active
  :wormhole.status/opened
  :wormhole.status/deactive}

(def starting-room
  {:room/id :room.id/starting
   :room/items []
   :room/neighbors {:down :room.id/down}
   :wormhole/status :wormhole.status/deactive
   :wormhole/connected nil})

(def down-room
  {:room/id :room.id/down
   :room/items [{:item/id 1}]
   :room/neighbors {:up :room.id/starting}
   :wormhole/status :wormhole.status/deactive
   :wormhole/connected nil})

;; we will have a starting state
;; we will have in memory atoms based on the user id

;; user
{:user/id :uuid
 :user/cookie :cookie
 :user/last-message-at :at}
;; when a new user joins, we copy the starting state into user state
;; we will keep a timestamp of when the last connection came in from a user and clean up user state that's past that

(defonce user-table
  (atom {}))

(defn now [] (new java.util.Date))

(defn add-user [id]
  (swap! user-table assoc id {:user/id id
                              :user/last-message-at (now)}))

;; room table by user
{:user-id {}}

(defonce room-table
  (let [room-table* (atom {})]
    (add-room :base-state starting-room)
    (add-room :base-state down-room)
    room-table*))

(defn add-room [user-id room]
  (swap! room-table assoc-in [user-id (:room/id room)] room))

(comment
  @room-table

  ;; starting state
  (add-room :base-state starting-room)
  (add-room :base-state down-room)

  )

;; the goal is to move the initial state to the sever
;; we will create a new user on each load
;; it will insert the starting state into the room table
;; it will insert a user into the user table
;; it will return data to the frontend and render the page

;; also how does a 2d room map look like?
;; r | r | r
;; r | r | r
;; r | r | r

;; we can store a grid that indexes into an individual room
;; and/or we can store pointers to the neighbors in each room

(pc/defresolver room-resolver [env {room-id :room/id user-id :user/id}]
  {::pc/input #{:room/id :user/id}
   ::pc/output [:room/id
                :room/items
                {:room/neighbors [:room/id]}
                :wormhole/status
                :wormhole/connected]}
  (get-in @room-table [user-id room-id]))

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

