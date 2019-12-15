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
   [app.mutations :as api]))

(defrecord Watchers [broadcaster]
  component/Lifecycle
  (start [component]
    component)
  (stop [component]
    component))

;; create watchers
(defn add-watchers []
  (add-watch state/room-table :room-watcher
    (fn [key_ atom_ old-state new-state]
      (log/info "change" key_ old-state new-state))))

;; we are going to requery the data in order to make it UI friendly regardless
;; all we need are rooms changed by user
;; changes mean that either the new or old is missing front two level keys

(data/diff {:user-id {:room/one {:a 1}}} {:user-id {:room/one {:a 1}
                                                    :room/two {}}})
;; map changes to entity
(defn change->entity [] nil)
;; query entity
;; send entity

{:ident [:room/id :room.id/starting]
 :user :user-id}

(defn query-component [parser component query-ident]
  (let [query       (comp/get-query component)
        result      (-> (parser {} [{query-ident query}])
                      first
                      val)]
    result))

(comment
  (query-component
    parser/api-parser
    components/Room
    [:user-room/id [:base-state :room.id/starting]])

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
        result   (query-component pathom-parser components/Room ident)
        ident-fn (fn [res] [:room/by-id (:room/id res)])
        ident    (ident-fn result)
        data     {ident result}]
    (if (nil-ident? ident)
      (log/info user-id "Unable to broadcast to nil ident:" ident)
      (do
        (log/info user-id "Broadcasting to ident:" ident)
        (push-merge-query websockets user-id query data)))))

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
  (if @broadcast-input-ch
    (async/put! @broadcast-input-ch
      {:user-id user-id
       :ident   ident})
    :broadcaster-down))

(defrecord Broadcaster [server input-ch]
  component/Lifecycle
  (start [component]
    (log/info "starting Broadcaster")
    (let [broadcast-ch (entity-broadcaster parser/api-parser (:websockets server))]
      (reset! broadcast-input-ch broadcast-ch)
      (assoc component :input-ch broadcast-ch)))
  (stop [component]
    (log/info "stopping Broadcaster")
    (let [input-ch (:input-ch component)]
      (reset! broadcast-input-ch nil)
      (when input-ch (async/close! input-ch))
      (assoc component :input-ch nil))))

(comment

  (let [user-id "6500ba9f-b936-4772-b79c-748edcf739fc"
        ident [:user-room/id [user-id :room.id/starting]]]
    (broadcast! user-id ident ))


  )
