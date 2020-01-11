(ns app.broadcaster
  (:require
   [clojure.core.async :as async :refer [go go-loop <!]]
   [clojure.data :as data]
   [com.stuartsierra.component :as component]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [taoensso.timbre :as log]
   [app.parser :as parser]
   [app.server :as server]
   [app.components :as components]
   [app.state :as state]
   [app.mutations :as api]
   [clojure.walk :as walk]))

(defn query-component [parser component user-id query-ident]
  (let [query       (comp/get-query component)
        result      (-> (parser {:user {:user/id user-id}}
                          [{query-ident query}])
                      first
                      val)]
    result))

(comment
  (query-component
    parser/api-parser
    components/Room
    :base-state
    [:room/id :room.id/starting])

  )

(defn push-merge-query
  "Pushes a merge query to the clients.

  Never push the base-state up."
  [websockets user-id query data]
  (when (not= user-id :base-state)
    (let [merge-query-push {:message/topic :merge
                            :merge/query   query
                            :merge/data    data}]
      (.push websockets user-id :merge merge-query-push))))

(defn nil-ident? [ident]
  (-> ident second nil?))

(defn query+push [pathom-parser websockets user-id ident]
  (let [query    (comp/get-query components/Room)
        result   (query-component pathom-parser components/Room user-id ident)
        ident-fn (fn [res] [:room/by-id (:room/id res)])
        ident    (ident-fn result)
        data     {ident result}]
    (cond
      (nil? user-id)
      (log/info "Unable to broadcast to nil user with ident:" ident)

      (nil-ident? ident)
      (log/info user-id "Unable to broadcast to nil ident:" ident)

      :else
      (do
        (log/info user-id "Broadcasting to ident:" ident)
        (doseq [room-id (keys (get @state/room-table user-id))]
          (push-merge-query websockets [user-id room-id] query data))))))

;; validate the message
{:ident [:room/id :room.id/starting]
 :user :user-id}

(defn entity-broadcaster
  "Builds the go loop that accepts messages, queries for entities, and broadcasts them."
  [pathom-parser websockets]
  (let [input-ch (async/chan)]
    (go-loop [message (<! input-ch)]
      (let [{:keys [user-id ident]} message]
        (query+push pathom-parser websockets user-id ident)
        (recur (<! input-ch))))
    input-ch))

(defonce broadcast-input-ch (atom nil))

(defn broadcast! [user-id ident]
  (if user-id
    (if @broadcast-input-ch
     (async/put! @broadcast-input-ch
       {:user-id user-id
        :ident   ident})
     :error-broadcaster-down)
    (throw (ex-info "Unable to broadcast! to a nil user" {:ident ident}))))

(defrecord Broadcaster [server input-ch]
  component/Lifecycle
  (start [component]
    (log/info "starting Broadcaster")
    (let [broadcast-ch (entity-broadcaster parser/pathom-parser (:websockets server))]
      (reset! broadcast-input-ch broadcast-ch)
      (assoc component :input-ch broadcast-ch)))
  (stop [component]
    (log/info "stopping Broadcaster")
    (let [input-ch (:input-ch component)]
      (reset! broadcast-input-ch nil)
      (when input-ch (async/close! input-ch))
      (assoc component :input-ch nil))))

(comment

  (let [user-id "451d9259-a521-4904-8823-bc55e55b512d"
        ident [:room/id :room.id/starting]]
    (broadcast! user-id ident ))


  )


;; create watchers
(require '[clojure.walk :as walk])

(defn nested-coll? [c]
  (and (coll? c) (coll? (first c))))

(defn create-path [k v]
  (if (nested-coll? v)
    (->> v
      (map #(into [k] %))
      (into []))
    [[k]]))

(create-path :a [:c])

(let [k :a
      v [[:b]
         [:c]]]
  (create-path k v))

(let [k :a
      v [[:b]
         [:c]]]
  (create-path k v))


(defn key-paths
  "Returns a sequence of all the paths of keys in the nested dictionaries."
  [c]
  (walk/postwalk
    (fn [n]
      (if (map? n)
        (->> (keys n)
          (map (fn [k]
                 (let [v (get n k)]
                   (create-path k v))))
          (mapcat identity))
        n))
    c))

(defn state-change-paths [old-state new-state]
  (let [[old new _] (data/diff old-state new-state)]
    (concat (key-paths new) (key-paths old))))

(defn add-change-path-watcher [atom name doer]
  (log/info "watching" name "for change paths")
  (add-watch atom name
    (fn [key_ atom_ old-state new-state]
      (-> (state-change-paths old-state new-state)
        doer))))

(defrecord Changes [broadcaster]
  component/Lifecycle
  (start [component]
    (add-change-path-watcher state/room-table :room-watcher
      (fn [change-paths]
        (let [state-change-idents (->> change-paths
                                    (map #(take 2 %))
                                    distinct
                                    (into []))]
          (log/info "change" state-change-idents)
          (doseq [[user-id room-id] state-change-idents]
            (if user-id
              (broadcast! user-id [:room/id room-id])
              (log/info "ignoring change broadcast to" {:user-id user-id
                                                        :room-id room-id}))))))
    (log/info "Starting Changes")
    component)
  (stop [component]
    (remove-watch state/room-table :room-watcher)
    (log/info "Stopping Changes")
    component))
