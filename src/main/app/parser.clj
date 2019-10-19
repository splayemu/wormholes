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
                      ::pc/mutation-join-globals [:tempids]}
             ::p/mutate pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin
                          sample-plugin]}))

(defn api-parser
  ([query] (pathom-parser {} query))
  ([env query]
   (pathom-parser {:user (:user (:request env))} query)))

(comment


  (api-parser {:request {:user {:user/id :base-state}}}
    [:user/id
     {[:room/id :room.id/starting] [:room/id :room/items :wormhole/status :wormhole/connected]}])

  (api-parser {:request {:user {:user/id :base-state}}}
    [{[:room/id :room.id/starting] [:user-room/id :wormhole/status]}
     :user])

  (api-parser {:request {:user {:user/id :base-state}}}
    [{:starting-state [:center-room :neighbors]}])

  (api-parser {:request {:user {:user/id :base-stat222e}}}
    [{[:room/id :room.id/starting] [:room/id :user/id]}])

  (api-parser {:request {:user {:user/id :base-easdfadsf}}} [:user/id])
  )


