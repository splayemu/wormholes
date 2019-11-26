(ns app.parser
  (:require
   [app.resolvers]
   [app.mutations]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   [taoensso.timbre :as log]))

(def resolvers [app.resolvers/resolvers app.mutations/mutations])

(def sample-plugin
  {::p/wrap-parser
   (fn [parser]
     (fn [env tx]
       (parser env tx)))})

(def pathom-parser
  (p/parser {::p/env {::p/reader [p/map-reader
                                  pc/reader2
                                  pc/ident-reader
                                  pc/index-reader]
                      ::p/process-error (fn [_ err]
                                          (.printStackTrace err)
                                          (p/error-str err))
                      ::pc/mutation-join-globals [:tempids]}
             ::p/mutate pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin
                          sample-plugin]}))

(defn api-parser
  ([query] (pathom-parser {} query))
  ([env query]
   (def tenv env)
   (pathom-parser {:user (:user (:request env))
                   :push (:push env)}
     query)))

(comment

  (api-parser {} ['(:meow {:dep :heeh})])


  (api-parser {:request {:user {:user/id :base-state}}}
    [:user/id
     {[:room/id :room.id/starting] [:room/id :room/items :wormhole/status :wormhole/connected]}])

  (api-parser {:request {:user {:user/id :base-state}}}
    [{[:room/id :room.id/starting] [:user-room/id :wormhole/status]}
     :user])

  ;; turn this into a test
  ;; mutations
  (let []
    (api-parser {:request {:user {:user/id :test-user}}}
      [{'(app.mutations/initialize-connection {:connection/from :room.id/starting
                                               :connection/to :room.id/down})
        [:room/id
         :wormhole/status
         :wormhole/connection]}]))

  (let []
    (api-parser {:request {:user {:user/id :test-user}}}
      [{'(app.mutations/confirm-connection {:connection/room-id :room.id/down})
        [:room/id
         :wormhole/status
         :wormhole/connection]}]))

  ;; satisfy inputs of a query by passing in the input in an ident
  (let [ident [:user-room/id [:base-state :room.id/starting]]
        query [:wormhole/status]]
    (api-parser {}
      [{ident query}]))

  ;; satisfy inputs of query by passing in the input in and ident and using the env
  (let [ident [:room/id :room.id/starting]
        query [:user-room/id :wormhole/status]]
    (api-parser {:request {:user {:user/id :base-state}}}
      [{ident query}]))

  (api-parser
    {:request {:user {:user/id "ce677006-71f7-4ee6-a63a-c6e4b8529e62"}}}
    ['({:room-configuration
       [{:center-room
         [:room/id
          :user-room/id
          :room/items
          :wormhole/status
          :wormhole/connected]}
        {:up-room
         [:room/id
          :user-room/id
          :room/items
          :wormhole/status
          :wormhole/connected]}
        {:down-room
         [:room/id
          :user-room/id
          :room/items
          :wormhole/status
          :wormhole/connected]}
        {:left-room
         [:room/id
          :user-room/id
          :room/items
          :wormhole/status
          :wormhole/connected]}
        {:right-room
         [:room/id
          :user-room/id
          :room/items
          :wormhole/status
          :wormhole/connected]}]} 
      {:center-room-id :room.id/down})])


  )


