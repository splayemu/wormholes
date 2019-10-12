(ns app.ui
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro-css.css-injection :as inj]
   [app.mutations :as api]
   ))

(defsc Person [this
               {:person/keys [id name age] :as props}
               {:keys [onDelete]}
               {:keys [red]}]
  {:query [:person/id :person/name :person/age]
   :ident (fn [] [:person/id (:person/id props)])
   :css [[:.red {:color "blue"
                 :font-size "1.2em"}]]}
  (dom/li
   (dom/div
    (dom/h5 {:classes [red]} (str name " (age: " age ")")))
   (dom/button {:onClick #(onDelete id)} "X")))

(def ui-person (comp/factory Person {:keyfn :person/name}))

(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query [:list/id :list/label {:list/people (comp/get-query Person)}]
   :ident (fn [] [:list/id (:list/id props)])}
  (let [delete-person (fn [person-id] (comp/transact! this [(api/delete-person {:list/id id :person/id person-id})]))]
    (dom/div
     (dom/ul
      (map (fn [p] (ui-person (comp/computed p {:onDelete delete-person}))) people)))))

(def ui-person-list (comp/factory PersonList))

(defsc Root [this {:keys [friends enemies]}]
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
