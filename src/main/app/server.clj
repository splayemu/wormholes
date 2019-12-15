(ns app.server
  (:require
   [app.util :as util :refer [inspect]]
   [app.parser :refer [api-parser]]
   [app.state :as state]
   [org.httpkit.server :as http]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [com.stuartsierra.component :as component]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.server.api-middleware :as server]
   [com.fulcrologic.fulcro.networking.websocket-protocols :refer [WSListener WSNet add-listener remove-listener client-added client-dropped]]
   ;; not sure if I need this
   ;;[ring.middleware.multipart-params]
   [ring.util.response :as response]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [taoensso.timbre :as log]))

(def cookie-name "user_id")
(def cookie-path [:cookies cookie-name :value])

(defn create-cookie [{:keys [user/id]}]
  {:value id 
   ;;:max-age   (str (* 60 60 365))
   ;;:http-only true
   })

(defn create-user! []
  (let [user-id (str (java.util.UUID/randomUUID))]
    ;; probably should create a db namespace
    (state/add-user! user-id)))

;; TODO
;; we should change this to return the immutable base state
;; until they make a change, then we create the user state

;; if no cookie exists on the req chain
;; create a userid
;; insert into the database the user
;; and return the cookie with the user id
(defn user-middleware [handler]
  (fn [req]
    (util/log "user-middleware: req")
    (let [cookie       (get-in req cookie-path)
          ;; user/id is the same as the cookie
          user         (get @state/user-table cookie)
          ;; create the user if the user doesn't exist
          create-user? (nil? user)
          user         (if create-user?
                         (create-user!)
                         user)]
      (try
        ;; pass down the user on the request context
        (let [resp (handler (assoc req :user user))]
          (if create-user?
            (let [cookie {cookie-name (create-cookie user)}]
              (update resp :cookies merge cookie))
            resp))
        (catch Throwable e
          (util/log "user-middleware: Exception:" e)
          (util/log "user-middleware: Removing user state")
          (state/remove-user! (:user/id user))
          ;; for some reason this isn't returning to the client
          {:status  500
           :headers {"Content-Type" "text/plain"}
           :body    "Server Error"})))))

(comment
  (create-cookie {:user/id "meow"})
  (create- cookie (create-user!))

  (let [user tuser
        resp tresp]
    (update resp :cookies merge {cookie-name (inspect (cookie (inspect user)))}))

  )

(defn user-id-fn [req]
  (-> req :user :user/id))

(defroutes app
  (route/resources "/")
  (route/not-found "Not Found"))

(defn always-index [handler]
  (fn [req]
    (handler
      (assoc req :uri "/index.html"))))

(defrecord WebsocketListener []
  WSListener
  (client-added [this _ws user-id]
    (log/info "User connected" user-id))
  (client-dropped [this _ws user-id]
    (log/info "User disconnected" user-id)))

(def stop-server-backup (atom nil))
(defn start-server! [component & [stop-server-atom]]
  (util/log "starting server")
  (let [websockets (fws/start! (fws/make-websockets
                                 api-parser
                                 {:http-server-adapter (get-sch-adapter)
                                  :sente-options {:csrf-token-fn nil
                                                  :user-id-fn user-id-fn}
                                  :parser-accepts-env? true}))
        middleware (-> app
                     always-index
                     (fws/wrap-api websockets)
                     user-middleware
                     wrap-cookies
                     wrap-keyword-params
                     wrap-params
                     (wrap-resource "public")
                     wrap-content-type
                     wrap-not-modified)
        server (http/run-server middleware {:port 3000})
        listener (WebsocketListener.)]
    (add-listener websockets listener)
    (let [stop (fn stop-fn []
                    (fws/stop! websockets)
                    (server))]
      (when stop-server-atom
        (reset! stop-server-atom stop))
      (-> component
        (assoc :stop-fn stop)
        (assoc :websockets websockets)))))

(defrecord Server [stop-fn websockets]
  component/Lifecycle
  (start [component]
    (start-server! component stop-server-backup))
  (stop [component]
    (let [stop-fn (:stop-fn component)]
      (if stop-fn
        (stop-fn)
        (log/info "stop-fn is nil. Server is lost."))
      component)))
