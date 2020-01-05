(ns app.state
  (:require
   [taoensso.timbre :as log]
   [app.util :as util :refer [inspect]]))

;; wormhole.status
#{:wormhole.status/active
  :wormhole.status/deactive
  :wormhole.status/connecting
  :wormhole.status/connected
  }

;; starting state
(def starting-room
  {:room/id :room.id/starting
   :room/coords [0 0]
   :room/items []
   :wormhole/status :wormhole.status/deactive
   :wormhole/connection nil})

(def down-room
  {:room/id :room.id/down
   :room/coords [0 1]
   :room/items [{:item/id 1}]
   :wormhole/status :wormhole.status/deactive
   :wormhole/connection nil})

;; user
{:user/id :uuid
 :user/cookie :cookie
 :user/last-message-at :at}

(defn now [] (new java.util.Date))

(defn add-room [room-table user-id room]
  (assoc-in room-table [user-id (:room/id room)] room))

(def initial-room-table
  (-> {}
    (add-room :base-state starting-room)
    (add-room :base-state down-room)))

;; finding neighbors
(def neighbor-coords-order [:left :up :right :down])
(defn neighbor-coords [coords]
  (let [offset-coords [;; left
                       [-1 0]
                       ;; up
                       [0 -1]
                       ;; right
                       [1 0]
                       ;; down
                       [0 1]]]
    (map (fn [[c1x c1y] [c2x c2y]]
           [(+ c1x c2x) (+ c1y c2y)])
      (repeat coords)
      offset-coords)))

(comment
  (neighbor-coords [1 0])

  )

(defn place-room
  "Places a room in the grid with its x, y location."
  [grid room]
  (assoc grid (:room/coords room) (:room/id room)))

(defn create-grid [rooms]
  (reduce place-room {} rooms))

(defn room+neighbors
  "Adds neighbor room ids to a single room."
  [room grid]
  (let [neighbor-coords* (neighbor-coords (:room/coords room))
        neighbor-rooms   (->> (map #(get grid %) neighbor-coords*)
                           (map (fn [k coords] (when coords {k coords})) neighbor-coords-order)
                           (remove nil?)
                           (apply merge))]
    (assoc room :room/neighbors neighbor-rooms)))

(defn build-neighbors
  "Adds neighbor room ids to all rooms in the room-table."
  [room-table]
  (let [rooms (vals room-table)
        grid (create-grid rooms)]
    (reduce (fn [room-table room]
              (update room-table (:room/id room) room+neighbors grid))
      room-table
      rooms)))

(def initial-room-table-with-neighbors
  (update initial-room-table :base-state build-neighbors))

;; stateful changes
(defonce user-table
  (atom {}))
(defonce room-table
  (atom initial-room-table-with-neighbors))

;; deprecated
(defn add-room! [user-id room]
  (swap! room-table add-room user-id room))

(defn set-rooms!
  "Duplicate initial room state into user room table."
  [user-id]
  (swap! room-table assoc user-id (:base-state initial-room-table-with-neighbors)))

(defn add-user!
  "Add the user and create initial state if the user doesn't exist already."
  [id]
  (when (not (get @user-table id))
    (log/info "adding user" id)
    (let [user {:user/id id
                :user/last-message-at (now)}]
      (swap! user-table assoc id user)
      (set-rooms! id)
      user)))

(defn remove-user!
  [id]
  (swap! user-table dissoc id)
  (swap! room-table dissoc id)
  :ok)

(defn reset-state! []
  (reset! user-table {})
  (reset! room-table initial-room-table-with-neighbors)
  :ok)

(comment
  (keys @room-table)

  (count (keys @user-table))
  (count (keys @room-table))

  (reset-state!)

  (get-in @room-table ["451d9259-a521-4904-8823-bc55e55b512d"])

  (add-user! :test-user)
  (add-user! :test-mow)

  (remove-user! 1)

  ;; starting state
  (add-room :base-state starting-room)
  (add-room :base-state down-room)

  )
