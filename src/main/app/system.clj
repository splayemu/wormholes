(ns app.system
  (:require
   [app.server :as server]
   [app.broadcaster :as broadcaster]
   [com.stuartsierra.component :as component]))

(defonce system (atom nil))

(defn atom? [a]
  (instance? clojure.lang.Atom a))

(defn start-system! [& [system]]
  (when (and (atom? system)
          (not (nil? @ system)))
    (swap! system component/stop))
  (let [system (or system (atom nil))]
    (reset! system (component/system-map
                     :server (server/map->Server {})
                     :broadcaster (component/using
                                    (broadcaster/map->Broadcaster {})
                                    [:server])))

    (swap! system component/start)
    system))

(comment
  (start-system! system)

  @system


  (swap! system component/stop)

         )

