(ns app.application
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro.algorithms.merge :as fulcro-merge]
   [app.ui :as ui]
   [app.messages]))

(declare app)

(def lame-symbol-mapping
  {'app.ui/Room app.ui/Room})

(defn merge-handler [{:keys [merge/component merge/data]}]
  (js/console.log "merge-handler:" component data)
  (fulcro-merge/merge-component!
    app
    (lame-symbol-mapping component)
    data))

(defn push-handler [{:keys [topic msg]}]
  (case topic
    :merge (merge-handler msg)
    :else (js/console.error "push-handler: topic" topic "does not exist")))

(defonce app (app/fulcro-app
               {:remotes {:remote (fws/fulcro-websocket-remote
                                    {:push-handler push-handler})}
               :props-middleware (comp/wrap-update-extra-props
                                  (fn [cls extra-props]
                                    (merge extra-props (css/get-classnames cls))))}))
