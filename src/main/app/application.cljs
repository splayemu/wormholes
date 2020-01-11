(ns app.application
  (:require
   [taoensso.encore :as enc]
   [com.fulcrologic.fulcro.application :as fulcro.app]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro.algorithms.merge :as fulcro-merge]
   [app.messages]
   [app.components]))

(defonce app (atom nil))

(def lame-symbol-mapping
  {'app.components/Room app.components/Room})

(defn merge-component-handler [{:keys [merge/component merge/data]}]
  (js/console.log "merge-component-handler:" component data)
  (fulcro-merge/merge-component!
    @app
    (lame-symbol-mapping component)
    data))

(defn merge-handler [{:keys [merge/query merge/data] :as params}]
  (js/console.log "merge-handler:" query data)
  (comp/transact! @app `[(app.mutations/merge-push ~params)]))

(defn push-handler [{:keys [topic msg]}]
  (case topic
    :merge (merge-handler msg)
    :else (js/console.error "push-handler: topic" topic "does not exist")))

(defn create-app! [room-id]
  (reset! app (fulcro.app/fulcro-app
                {:remotes {:remote (fws/fulcro-websocket-remote
                                     {:push-handler push-handler
                                      :sente-options {:client-id (str room-id "|" (enc/uuid-str))}})}
                 :props-middleware (comp/wrap-update-extra-props
                                     (fn [cls extra-props]
                                       (merge extra-props (css/get-classnames cls))))})))
