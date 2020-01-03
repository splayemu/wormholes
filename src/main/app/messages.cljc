(ns app.messages)

;; we only define server -> client pushing

(def valid-topics #{:merge})

;; msg structure
{:message/topic :merge
 :merge/component 'ui/Room}

#?(:clj
   (defn pusher [{:keys [topic msg]}]
     (js/console.log "push" topic msg)))
