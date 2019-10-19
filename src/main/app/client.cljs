(ns app.client
  (:require
   [app.application :refer [app]]
   [app.ui :as ui]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))


;; could define here a set of rooms that are exposed as url routes
;; if the user enters in a bogus route, it could reroute to starting or w/e
;; also we can now set some starting state and load the correct state
;; try to load a different room state by setting the url

(defn pathname []
  js/window.location.pathname)

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (let [;; strip off starting "/"
        room-id (subs (pathname) 1)
        room-id (keyword (str "room.id/" room-id))]

    (def troom room-id)

    ;;(js/history.replaceState (clj->js {:page 3}) "title 3" "/meow")

    (app/mount! app ui/Root "app")
    ;; okay so how do we load a specific room and pass the user id
    (df/load! app [:room/id room-id] ui/Room {:target (targeting/replace-at [:center-room])})
    ;;
    (js/console.log "Loaded")))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app ui/Root "app")
  (js/console.log "Hot reload"))
