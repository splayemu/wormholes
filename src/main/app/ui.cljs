(ns app.ui
  (:require-macros [garden.def :refer [defkeyframes]])
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro-css.css-injection :as inj]
   [app.mutations :as api]
   [app.util :refer-macros [inspect]]
   [app.graphics :as graphics]
   [garden.core :as g]
   ))

(defkeyframes meow
  [:from {:opacity 0}]
  [:to {:opacity 1}])

(defkeyframes rotate
  [:from {:transform       "rotate(0deg)"
          :transform-origin "48.5% 49%"}]
  [:to {:transform         "rotate(360deg)"
        :transform-origin  "48.5% 49%"}])

(def spiral-length 2161.78125)
(defkeyframes grow
  [:from {"stroke-dashoffset" spiral-length
          "transform" "scale(0.2)"
          :transform-origin  "48.5% 49%"}]
  [:to {"stroke-dashoffset" 0
        "transform" "scale(1)"
        :transform-origin  "48.5% 49%"}])

(defonce spiral-path
  (-> "M 78.364487,212.2036 c 0.284267,1.59189 -2.005595,1.16857 -2.645834,0.47247 -1.735007,-1.88639 -0.232517,-4.74557 1.700893,-5.76414 3.458415,-1.82198 7.51609,0.51027 8.882441,3.87426 2.005174,4.93679 -1.25027,10.33316 -6.047618,12.00074 -6.39411,2.22263 -13.167925,-1.9876 -15.119049,-8.22098 -2.45529,-7.84407 2.723253,-16.01141 10.394343,-18.23735 9.290718,-2.69592 18.859867,3.45787 21.355657,12.5677 2.941202,10.73563 -4.191835,21.71143 -14.741069,24.47396 -12.179508,3.18946 -24.565066,-4.92536 -27.592264,-16.91443 -3.439695,-13.62274 5.658582,-27.42015 19.087794,-30.71056 15.065546,-3.69135 30.276309,6.39158 33.828869,21.26115 3.94403,16.50804 -7.12443,33.13325 -23.434516,36.94718 -17.950315,4.19747 -35.990805,-7.85715 -40.06548,-25.60788 -4.451524,-19.39242 8.589772,-38.84885 27.781245,-43.18379 20.834396,-4.70604 41.707261,9.32232 46.302091,29.95461 4.96094,22.27627 -10.05481,44.566 -32.127974,49.42039 -23.718067,5.21615 -47.424991,-10.78725 -52.538695,-34.30133 -5.471603,-25.1598 11.519645,-50.2842 36.474695,-55.657 26.60147,-5.72727 53.143594,12.25201 58.775304,38.64806 5.98312,28.0431 -12.98435,56.00313 -40.821422,61.8936 -29.484691,6.23912 -58.862809,-13.71665 -65.01191,-42.99478 -6.495247,-30.92625 14.448944,-61.7226 45.168146,-68.13021 32.367786,-6.75149 64.582486,15.18121 71.248516,47.3415 7.00783,33.8093 -15.91347,67.44247 -49.51487,74.36683 -35.250778,7.26425 -70.302516,-16.64572 -77.485126,-51.68824 -7.5207508,-36.69225 17.377945,-73.16263 53.861598,-80.60343 38.133698,-7.77732 76.022818,18.11017 83.721738,56.03496 8.03394,39.57514 -18.84238,78.88305 -58.208327,86.84004 C 50.577096,294.57755 9.8503282,266.71236 1.6353212,225.90524 -6.912028,183.44726 21.942089,141.30158 64.19037,132.8286 c 43.89939,-8.80412 87.46402,21.03895 96.19495,64.72841 9.06093,45.34078 -21.77114,90.32443 -66.901775,99.31325"
    (graphics/translate-path-str {:y-translation -130.67721})))

(defsc Wormhole [this
                 {:keys [wormhole/status] :as props}
                 {:keys [on-wormhole-click
                         on-wormhole-mouse-enter
                         on-wormhole-mouse-leave
                         center?]
                  :as computed}
                 {:as css}
                 ]
  {:query [:wormhole/status]
   :css [
         #_[:$wormhole-activated-animation
            {
             ;;:transition "stroke-dashoffset 2s ease-in-out"
             }
            ]
         [:$spiral-wrapper
          {:height "80%"
           :padding "5px"}]
         [:$spiral-wrapper
          [:path {"fill" "none",
                  "fill-rule" "evenodd",
                  "stroke-width" "2px",
                  "stroke-linejoin" "miter",
                  "stroke-opacity" "1",
                  "stroke" "#000000",
                  "stroke-linecap" "butt"
                  }]]
         [:$wormhole
          {:transition "2s cubic-bezier(0, 0, 0, 1)"
           :transform-origin  "48.5% 49%"
           "stroke-dasharray" (str spiral-length " " spiral-length)}
          [:&$unactivated
           {"transform" "scale(0.2)"
            "stroke-dashoffset" (* spiral-length 0.6)}]
          [:&$activated
           {"stroke-dashoffset" 0}]
          [:&$hover
           {"transform" "scale(0.7)"
            "stroke-dashoffset" (* spiral-length 0.4)}]]
         [:$wormhole-rotating-animation
          ^:prefix {:animation "rotate 2s infinite linear"}]
         rotate
         ]}

  (let [path-classes (cond
                       (= status :wormhole.status/active) ["wormhole" "activated"]
                       (= status :wormhole.status/hover) ["wormhole" "hover"]
                       :else ["wormhole" "unactivated"])

        openable-wormhole? (and (= status :wormhole.status/hover) (not center?))

        spiral-svg
        (dom/svg {:width "100%"
                  :viewBox "0 0 162.61878 166.3229"
                  :onClick on-wormhole-click
                  :onMouseLeave on-wormhole-mouse-leave
                  :onMouseEnter on-wormhole-mouse-enter
                  :id "spiral"}
          (dom/g {:classes path-classes}
            (dom/path :.wormhole-rotating-animation {:d spiral-path})))]
    (dom/div 
      (if openable-wormhole?
        (dom/div :.spiral-wrapper {:href "/down" :target "_blank"}
          spiral-svg)
        (dom/div :.spiral-wrapper
          spiral-svg)))))


(comment
  (js/window.open "/down" "_blank")

  )

;; good for rotating animation
"stroke-dasharray" "200"
"stroke-dashoffset" "100"

(def ui-wormhole (comp/factory Wormhole))

(defn animate-spiral [spiral-ele]
  (let [length (.getTotalLength spiral-ele)
        transition "stroke-dashoffset 2s ease-in-out"]
    (set! (.-transition (.-style spiral-ele)) "none;")
    (set! (.-strokeDasharray (.-style spiral-ele)) (str spiral-length " " spiral-length))
    (set! (.-strokeDashoffset (.-style spiral-ele)) spiral-length)
    (.getBoundingClientRect spiral-ele)
    (set! (.-WebkitTransition (.-style spiral-ele)) transition)
    (set! (.-transition (.-style spiral-ele)) transition)
    (set! (.-strokeDashoffset (.-style spiral-ele)) "0")
    spiral-ele))

(comment
  (let [spiral-ele (.querySelector js/document "#spiral path")]
    (animate-spiral spiral-ele))

  )




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
             {:keys [room/id room/items wormhole/status wormhole/connected room/unaccessible?]
              :as props}
             {:as computed}
             {:keys [basic-style wormhole-opened room-unaccessible]
              :as css}]
  {:query [:room/id
           :user-room/id
           :room/items
           :room/unaccessible?
           :wormhole/status
           :wormhole/connected
           ;; dirty hack to get the wormhole css added
           {:wormhole (comp/get-query Wormhole)}
           ]
   :ident (fn [] (api/room-ident id))
   :css [[:.basic-style
          {:color "black"
           :width "100%"
           :height "100%"
           :min-height "250px"
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
          {:--aug-border-bg "linear-gradient(red, transparent), linear-gradient(to right, blue, transparent), black"}]

         
         [:.room-unaccessible
          {:opacity 0.5}]
         ;; global css class
         [:$column {:display "flex"
                    :flex-direction "column"}]
         [:$left {:width "20%"}]
         [:$right {:width "20%"}]]}
  (let [unaccessible? (or (= unaccessible? true) (nil? id))
        active? (= status :wormhole.status/active)
        wormhole-class (cond
                         unaccessible? room-unaccessible
                         active? wormhole-opened
                         :else nil)
        mouse-events {:on-wormhole-click #(comp/transact! this `[(api/click-wormhole {:room/id ~id})])
                      :on-wormhole-mouse-enter #(comp/transact! this `[(api/mouse-enter-wormhole {:room/id ~id})])
                      :on-wormhole-mouse-leave #(comp/transact! this `[(api/mouse-leave-wormhole {:room/id ~id})])}]
    (dom/div {:classes [basic-style wormhole-class]
              :augmented-ui "br-round tl-round exe"}
      (when-not unaccessible?
        (dom/div {:style {:display "flex"
                          :justify-content "space-between"}}
          (dom/div :.left.column)
          (dom/div :.mid.column 
            (dom/div {:style {:text-align "center"}}
              (name id))
            (ui-wormhole (comp/computed props (merge computed mouse-events))))
          (dom/div :.right.column 
            (when items
              (dom/div :.items
                (dom/ul
                  (map (fn [item] (ui-item item)) items))))))))))

(def ui-room (comp/factory Room {:keyfn :room/id}))

(defsc RoomConfiguration [this
             {:keys [center-room up-room down-room left-room right-room] :as props}
             computed
             {:keys [grid-wrapper]}]
  {:query [{:center-room (comp/get-query Room)}
           {:up-room (comp/get-query Room)}
           {:down-room (comp/get-query Room)}
           {:left-room (comp/get-query Room)}
           {:right-room (comp/get-query Room)}]

   :css [[:.grid-wrapper {:height "calc(100% - 42px)"
                          :width "calc(100% - 42px)"
                          :display "grid"
                          :grid-template-columns "repeat(3, 1fr)"
                          :grid-gap "42px"
                          :grid-template-rows "repeat(3, 1fr)"}]]}
  (dom/div {:classes [grid-wrapper]}
    (dom/div {:style {:grid-column 1
                      :grid-row 1}}
      (ui-room {:room/unaccessible? true}))
    (dom/div {:style {:grid-column 2
                      :grid-row 1}}
      (ui-room up-room))
    (dom/div {:style {:grid-column 3
                      :grid-row 1}}
      (ui-room {:room/unaccessible? true}))
    (dom/div {:style {:grid-column 1
                      :grid-row 2}}
      (ui-room left-room))
    (dom/div {:style {:grid-column 2
                      :grid-row 2}}
      (ui-room (comp/computed center-room {:center? true})))
    (dom/div {:style {:grid-column 3
                      :grid-row 2}}
      (ui-room right-room))
    (dom/div {:style {:grid-column 1
                      :grid-row 3}}
      (ui-room {:room/unaccessible? true}))
    (dom/div {:style {:grid-column 2
                      :grid-row 3}}
      (ui-room down-room))
    (dom/div {:style {:grid-column 3
                      :grid-row 3}}
      (ui-room {:room/unaccessible? true}))))

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
    (inj/style-element {:component Root
                        ;; doesn't seem to be needed right now
                        :garden-flags {:vendors [#_"webkit"]}})
    (ui-room-configuration (inspect room-configuration))))



