(ns app.messages)

;; we only define server -> client pushing

#?(:clj
   (defn pusher [{:keys [topic msg]}]
     (js/console.log "push" topic msg)))

#?(:cljs
   (defn push-handler [{:keys [topic msg]}]
     (js/console.log "push" topic msg)))
