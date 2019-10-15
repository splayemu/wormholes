(ns app.server
  (:require
   [app.util :as util]
   [app.parser :refer [api-parser]]
   [org.httpkit.server :as http]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.server.api-middleware :as server]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.cookies :refer [wrap-cookies]]))

(def ^:private not-found-handler
  (fn [req]
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not Found"}))

(defn cookie []
  {:value (str (java.util.UUID/randomUUID))
   ;;:max-age   (str (* 60 60))
   ;;:http-only true
   })

;; for cookieless people,
;; create a new uesr and cookie
;; and append it to the response
;;(defn )

(defn user-middleware [handler]
  (fn [req]
    (def treq req)
    (util/log "cookies req" (:cookies req))
    (let [resp (handler req)
          resp (update resp :cookies merge {"user_id" (cookie)})
          ]
      
      (def tresp resp)
      (util/log "cookies resp" resp)
      resp)))

(defn log-middleware [handler]
  (fn [req]
    (def tlogreq req)
    ;;(util/log "cookies req" req)
    (let [resp (handler req)]
      (def tlogresp resp)
      (util/log "resp" resp)
      resp)))

(defonce stop-fn (atom nil))

;; might need to setup a separate endpoint in order to return a cookie
;; or perhaps set it on the client?

(defn start []
  (util/log "starting server")
  (let [websockets (fws/start! (fws/make-websockets
                                 api-parser
                                 {:http-server-adapter (get-sch-adapter)
                                  :sente-options {:csrf-token-fn nil}}
                                 ))
        middleware (-> not-found-handler
                     (fws/wrap-api websockets)
                     log-middleware
                     user-middleware
                     wrap-cookies
                     wrap-keyword-params
                     wrap-params
                     log-middleware
                     (wrap-resource "public")
                     wrap-content-type
                     wrap-not-modified
                     )
        server (http/run-server middleware {:port 3000})]
    (reset! stop-fn
      (fn []
        (fws/stop! websockets)
        (server)))))

(defn stop []
  (when @stop-fn
    (@stop-fn)
    (reset! stop-fn nil)))

(comment
  (start)

  (stop)


  )
