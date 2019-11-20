(ns app.server
  (:require
   [app.util :as util :refer [inspect]]
   [app.parser :refer [api-parser]]
   [app.state :as state]
   [org.httpkit.server :as http]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [com.fulcrologic.fulcro.server.api-middleware :as server]
   ;; not sure if I need this
   ;;[ring.middleware.multipart-params]
   [ring.util.response :as response]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.cookies :refer [wrap-cookies]]))

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

(defn log-middleware [handler]
  (fn [req]
    #_(do
      (def tlogreq req)
      (util/log "log-middleware: req:" req))
    (let [resp (handler req)]
      #_(do
        (def tlogresp resp)
        (util/log "log-middleware: resp:" resp))
      resp)))

(defn user-id-fn [req]
  (-> req :user :user/id))

(defonce stop-fn (atom nil))

(defroutes app
  (route/resources "/")
  (route/not-found "Not Found"))

(defn always-index [handler]
  (fn [req]
    (handler
      (assoc req :uri "/index.html"))))

(defn start []
  (util/log "starting server")
  (let [websockets (fws/start! (fws/make-websockets
                                 api-parser
                                 {:http-server-adapter (get-sch-adapter)
                                  :sente-options {:csrf-token-fn nil
                                                  :user-id-fn user-id-fn}
                                  :parser-accepts-env? true}))
        connected-uids (:connected-uids websockets)
        middleware (-> app
                     always-index
                     (fws/wrap-api websockets)
                     user-middleware
                     wrap-cookies
                     wrap-keyword-params
                     wrap-params
                     log-middleware
                     (wrap-resource "public")
                     wrap-content-type
                     wrap-not-modified)
        server (http/run-server middleware {:port 3000})]
    (reset! stop-fn
      (fn []
        (fws/stop! websockets)
        (server)))
    connected-uids))

(defn stop []
  (when @stop-fn
    (@stop-fn)
    (reset! stop-fn nil)))

(comment
  ;; start
  (let [connected-uids (start)]
    (def curr-uids connected-uids))
  
  @curr-uids

  (stop)


  )
