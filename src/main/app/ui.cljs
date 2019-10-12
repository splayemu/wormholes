(ns app.ui
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro-css.css-injection :as inj]
   [app.mutations :as api]
   ))

;; start from nothing
;; clicker
;; click the single button
;; and augs could be added to a different element
;; or new things could appear
;; 

;; okay so
;; rooms consist of a wormhole that can be open to one other place
;; hovering or clicking on a wormhole shows the neighboring rooms
;; when your wormhole is open, clicking on a different room connects the two rooms
;;   and opens a new browser tab
;; when that browser tab closes, the wormhole connection breaks

;; a single room may only have one wormhole open to another room
;; to close the wormhole, you need to close the other browser tab

;; items can live in a room
;; items can be clicked and dragged into a wormhole (and they go through the wormhole)
;;
;; first level
;; you only have two things:
;; one wormhole mouth
;; one keycard slot
;; 

;; wormhole states
#{:wormhole.state/active
  :wormhole.state/opened
  :wormhole.state/deactive}

(defn room-ident [id]
  [:room/by-id id])

;; domain modelling
{:room/id :room.id/starting
 :room/items [{:item/id 1}
              {:item/id 1}]
 :wormhole/state :wormhole.state/active
 :wormhole/connected :room.id/two}

(defn item-ident [id]
  [:item/by-id id])

{:item/id 1
 :item/type :item.type/keycard
 :item/color :red
 :item/room :room.id/starting}

(defsc Room [this
             {:keys [room/id room/items wormhole/state wormhole/connected]
              :as props}
             {:as computed}
             {:keys [shape]
              :as css}]
  {:query [:room/id
           :room/items
           :wormhole/state
           :wormhole/connected]
   :initial-state (fn [_] {:room/id :room.id/starting
                           :room/items [{:item/id 1}
                                        {:item/id 1}]
                           :wormhole/state :wormhole.state/active
                           :wormhole/connected :room.id/two})
   :ident (fn [] (item-ident id))
   :css [[:.shape {:color "green"

                 :font-size "2.2em"
                 :--aug-border "2px"
                 :--aug-inset "2px"
                 :--aug-border-bg "linear-gradient(red, transparent), linear-gradient(to right, blue, transparent), black"
                 ;; background between border and center
                 :background "green"
                 :--aug-border-opacity 0.8
                 :--aug-inset-bg "gold"
                 :--aug-inset-opacity 1
                 :--aug-r "25px"
                 :--aug-tl "35px"
                 :--aug-b-width "30%"
                 :--aug-b-height "20%"
                 :--aug-bl-height "3px"
                 :--aug-bl-width "30%"
                 ;; sets the origin from left to right
                 :--aug-t-origin-x "10%"
                 ;; sets the size of the aug
                 :--aug-t "30px"
                 :--aug-t-width "30px"


                 }]]}
  (dom/div {:classes [shape]
            :augmented-ui "r-clip bl-clip-x tl-round t-clip br-clip b-rect exe"}
   (str "I am a room" props)))

(def ui-room (comp/factory Room {:keyfn :room/id}))

(defsc Root [this {:keys [room]}]
  {:query [{:room (comp/get-query Room)}]
   :initial-state (fn [params] {:room (comp/get-initial-state Room)})}
  (dom/div
   (inj/style-element {:component Root})
   (ui-room room)))

(defsc Person [this
               {:person/keys [id name age] :as props}
               {:keys [onDelete]}
               {:keys [red]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])
   :css [[:.red {:color "green"

                 :font-size "2.2em"
                 :--aug-border "2px"
                 :--aug-inset "2px"
                 :--aug-border-bg "linear-gradient(red, transparent), linear-gradient(to right, blue, transparent), black"
                 ;; background between border and center
                 :background "green"
                 :--aug-border-opacity 0.8
                 :--aug-inset-bg "gold"
                 :--aug-inset-opacity 1
                 :--aug-r "25px"
                 :--aug-tl "35px"
                 :--aug-b-width "30%"
                 :--aug-b-height "20%"
                 :--aug-bl-height "3px"
                 :--aug-bl-width "30%"
                 ;; sets the origin from left to right
                 :--aug-t-origin-x "10%"
                 ;; sets the size of the aug
                 :--aug-t "30px"
                 :--aug-t-width "30px"


                 }]]}
  
  (dom/li
   (dom/div
    (dom/h5 {:classes [red]
             :augmented-ui "r-clip bl-clip-x tl-round t-clip br-clip b-rect exe"}
            (str name " (age: " age ")")))
   (dom/button {:onClick #(onDelete id)} "X")))

#_(def ui-person (comp/factory Person {:keyfn :person/name}))

#_(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query [:list/id :list/label {:list/people (comp/get-query Person)}]
   :ident (fn [] [:list/id (:list/id props)])}
  (let [delete-person (fn [person-id] (comp/transact! this [(api/delete-person {:list/id id :person/id person-id})]))]
    (dom/div
     (dom/ul
      (map (fn [p] (ui-person (comp/computed p {:onDelete delete-person}))) people)))))

#_(def ui-person-list (comp/factory PersonList))

#_(defsc Root [this {:keys [friends enemies]}]
  {:query [{:friends (comp/get-query PersonList)}
           {:enemies (comp/get-query PersonList)}]
   :initial-state {}}
  (dom/div
   (inj/style-element {:component Root})
   (dom/h3 "Friends")
   (when friends
     (ui-person-list friends))
   (dom/h3 "Enemies")
   (when enemies
     (ui-person-list enemies))))

(comment
  ;; try this in REPL once working
  (fdn/db->tree [{:friends [:list/label]}] (comp/get-initial-state Root {}) {})

  (def state (com.fulcrologic.fulcro.application/current-state app.application/app))

  (def query (com.fulcrologic.fulcro.components/get-query app.ui/Root))

  (com.fulcrologic.fulcro.algorithms.denormalize/db->tree query state state)

  (meta (com.fulcrologic.fulcro.components/get-query app.ui/PersonList))

  )
