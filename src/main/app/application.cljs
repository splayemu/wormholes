(ns app.application
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro-css.css :as css]))

(defonce app (app/fulcro-app
              {:remotes {:remote (fws/fulcro-websocket-remote {})}
               :props-middleware (comp/wrap-update-extra-props
                                  (fn [cls extra-props]
                                    (merge extra-props (css/get-classnames cls))))}))
