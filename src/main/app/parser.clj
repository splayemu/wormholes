(ns app.parser
  (:require
   [app.resolvers]
   [app.mutations]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   [taoensso.timbre :as log]))

(def resolvers [app.resolvers/resolvers app.mutations/mutations])

(def pathom-parser
  (p/parser {::p/env {::p/reader [p/map-reader
                                  pc/reader2
                                  pc/ident-reader
                                  pc/index-reader]
                      ::pc/mutation-join-globals [:tempids]}
             ::p/mutate pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                          p/error-handler-plugin]}))

(defn api-parser [query]
  (log/info "Process" query)
  (pathom-parser {} query))



(comment
  (pathom-parser {:user/id :base-state}
    [{:starting-state [:room/id :room/items :room/neighbors :wormhole/status :wormhole/connected]}])


  (api-parser [{[:list/id :friends] [:list/id]}])

  ;; with global parser
  (api-parser [{:friends [:list/id {:list/people [:person/name]}]}])

  (api-parser [:friends ])

  (api-parser [:user/id])
  (api-parser [{[:room/id :room.id/starting] [:room/id]}])

  (api-parser [{:starting-state [:room/id :room/items :room/neighbors :wormhole/status :wormhole/connected]}])
  (api-parser [{[:room/id :room.id/starting] [:room/id :room/items :room/neighbors :wormhole/status :wormhole/connected]}])
  
  (api-parser [{[:room/id :room.id/starting] [:room/id]}])

  (api-parser [:user/id])

  #_(api-parser [[[:room/id :room.id/starting] [:room/neighbors]] [:user/id :base-state]])


  )


