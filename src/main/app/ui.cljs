(ns app.ui
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro-css.css-injection :as inj]
   [app.mutations :as api]
   [app.util :refer-macros [inspect]]
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




{:item/id 1
 :item/type :item.type/keycard
 :item/color :red
 :item/room :room.id/starting}

(def item-css [[:.keycard
                {:width "10px"
                 :height "10px"
                 :color "red"
                 :--aug-border "1px"
                 :--aug-inset "1px"
                 :--aug-border-opacity 1
                 :--aug-inset-bg "red"
                 :--aug-inset-opacity 1
                 :--aug-border-bg "red"}]])

(defsc Item [this
             {:keys [item/id item/type item/color] :as props}
             computed
             {:keys [keycard] :as css}]
  {:query [:item/id :item/type :item/color]
   :initial-state (fn [item] item)
   :ident (fn [] (api/item-ident id))
   ;; not sure why this css isn't loading
   :css item-css}
  (let [item-type->classes {:item.type/keycard [keycard]}]
    (dom/div {:classes [keycard]
              :augmented-ui "br-round tl-round exe"}
             id)))

(def ui-item (comp/factory Item {:keyfn :item/id}))

;; wormhole.statuss
#{:wormhole.status/active
  :wormhole.status/opened
  :wormhole.status/deactive}

(def starting-room
  {:room/id :room.id/starting
   :room/items []
   :wormhole/status :wormhole.status/deactive
   :wormhole/connected nil})

(def down-room
  {:room/id :room.id/down
   :room/items [{:item/id 1}]
   :wormhole/status :wormhole.status/deactive
   :wormhole/connected nil})

(defsc Room [this
             {:keys [room/id room/items wormhole/status wormhole/connected]
              :as props}
             {:as computed}
             {:keys [basic-style wormhole-opened]
              :as css}]
  {:query [:room/id
           :user-room/id
           :room/items
           :wormhole/status
           :wormhole/connected]
   :ident (fn [] (api/room-ident id))
   :css [[:.basic-style
          {:color "black"
           :width "50%"
           :height "50%"
           :top "25%"
           :left "25%"
           :padding "12px"
           :font-size "2.2em"
           :--aug-border "6px"
           :--aug-border-bg "white"
           :--aug-inset "6px"
           :--aug-border-opacity 0.8
           :--aug-inset-bg "white"
           :--aug-inset-opacity 0.8
           :--aug-tl "15px"
           :--aug-br "15px"}]
         [:.wormhole-opened
          {:--aug-border-bg "linear-gradient(red, transparent), linear-gradient(to right, blue, transparent), black"}]]}
  (let [wormhole-class (if (= status :wormhole.status/active)
                         wormhole-opened
                         nil)
        on-wormhole-click #(comp/transact! this `[(api/click-wormhole {:room/id ~id})])]
    (dom/div {:classes [basic-style wormhole-class]
              :augmented-ui "br-round tl-round exe"}
      (dom/div (str "I am room " id))
      (dom/div {:onClick on-wormhole-click}
        (str "I am a wormhole " status))
      (when items
        (dom/div :.items
          (dom/ul
            (map (fn [item] (ui-item item)) items)))))))

(def ui-room (comp/factory Room {:keyfn :room/id}))

(defsc RoomConfiguration [this
             {:keys [center-room up-room down-room left-room right-room] :as props}
             computed
             ]
  {:query [{:center-room (comp/get-query Room)}
           {:up-room (comp/get-query Room)}
           {:down-room (comp/get-query Room)}
           {:left-room (comp/get-query Room)}
           {:right-room (comp/get-query Room)}]
   }
  (dom/div 
    (ui-room up-room)
    (ui-room left-room)
    (ui-room center-room)
    (ui-room right-room)
    (ui-room down-room)
    ))

(def ui-room-configuration (comp/factory RoomConfiguration))

(defsc Root [this
             {:keys [room-configuration] :as props}
             computed
             ;;{:keys [space]}
             ]
  {:query [{:room-configuration (comp/get-query RoomConfiguration)}]
   ;;:css [[:.space {:background "black"}]]
   }
  (dom/div 
    (inj/style-element {:component Root})
    (ui-room-configuration room-configuration)))



