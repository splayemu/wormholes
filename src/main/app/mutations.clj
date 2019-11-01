(ns app.mutations
  (:require
   [app.resolvers :as resolvers]
   [app.state :as state]
   [com.wsscode.pathom.connect :as pc]
   [taoensso.timbre :as log]))

#_(pc/defmutation delete-person [env {list-id :list/id
                                    person-id :person/id}]
  {::pc/sym `delete-person}
  (log/info "Deleting person" person-id "from list" list-id)
  (swap! list-table update list-id update :list/people (fn [old-list]
                                                         (filterv #(not= person-id %) old-list))))

(pc/defmutation make-connection [env {:keys [from-room-id
                                             to-room-id]}]
  {::pc/sym `make-connection}
  (log/info "Making connection" from-room-id " to " to-room-id)
  #_(swap! list-table update list-id update :list/people (fn [old-list]
                                                         (filterv #(not= person-id %) old-list))))

(def mutations [make-connection])

