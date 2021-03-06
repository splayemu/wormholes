(ns app.client
  (:require
   [app.application :as application]
   [app.components :as components]
   [com.fulcrologic.fulcro.application :as fulcro.app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
   [com.fulcrologic.fulcro.algorithms.merge :as fulcro-merge]))

(defn pathname->room-id
  ([] (pathname->room-id js/window.location.pathname))
  ([pathname]
   (let [;; strip off starting "/"
         room-id (subs pathname 1)
         room-id (keyword (str "room.id/" room-id))]
     room-id)))

(defn init! [app room-id]
  (fulcro.app/mount! app components/Root "app")
  (df/load! app :user nil {:target (targeting/replace-at [:user])})
  (df/load! app :room-configuration components/RoomConfiguration {:params {:center-room-id room-id}})
  (js/console.log "Loaded")
  (comp/transact! app `[(app.mutations/initialize-connection
                          {:connection/from ~room-id})]))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (let [room-id (pathname->room-id)
        app (application/create-app! room-id)]
    ;;(js/history.replaceState (clj->js {:page 3}) "title 3" "/meow")
    (init! app room-id)))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (fulcro.app/mount! @application/app components/Root "app")
  (js/console.log "Hot reload"))
