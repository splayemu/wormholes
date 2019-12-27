(ns app.timer-loop
  (:require
   [clojure.core.async :as async :refer [go go-loop <! timeout]]
   [clojure.data :as data]
   [com.stuartsierra.component :as component]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [taoensso.timbre :as log]
   [app.parser :as parser]
   [app.server :as server]
   [app.components :as components]
   [app.state :as state]
   [app.mutations :as api]
   [clojure.walk :as walk])
  )

(defonce timed-callbacks (atom {:ticks []}))

(defn decrement-timers [timer-state]
  (let [decrement-tick #(update % :amount (fnil dec 0))]
    (update timer-state :ticks #(map decrement-tick %))))

;; for each timer, decrement
;; if any of the timers are zero, remove
(let [timer-state {:ticks [{:amount 5
                            :callbacks [:b]}
                           {:amount 4
                            :callbacks [:b]}]}]
  (decrement-timers timer-state))

(defn pop-callbacks [timer-state]
  (let [active-callbacks (->> (:ticks timer-state)
                           (filter #(<= (:amount %) 0))
                           (mapcat :callbacks)
                           concat)
        filtered-ticks (remove #(<= (:amount %) 0) (:ticks timer-state))]
    (-> timer-state
      (update :active-callbacks concat active-callbacks)
      (assoc :ticks filtered-ticks))))

(comment
  (let [timer-state {:ticks [{:amount 0
                             :callbacks [:b]}
                            {:amount 0
                             :callbacks [:b :c]}]}]
    (pop-callbacks timer-state))

  (let [timer-state {:ticks [{:amount 0
                              :callbacks [:b]}
                             {:amount 0
                              :callbacks [:b :c]}]
                     :active-callbacks '(:meow)}]
    (pop-callbacks timer-state))

  )

(defonce active-timer? (atom true))

(defn callback-timer
  [tick-length-ms]
  (log/info "started callback-timer loop")
  (go-loop []
    (if @active-timer?
      (do
        (<! (timeout tick-length-ms))
        (let [timer-state (swap! timed-callbacks
                            #(-> %
                               decrement-timers
                               pop-callbacks))
              active-callbacks (:active-callbacks timer-state)]
          (swap! timed-callbacks assoc :active-callbacks [])
          (log/info "callback-timer" {:active-callbacks-count (count active-callbacks)})
          (doseq [callback active-callbacks]
            (try
              (callback)
              (catch Throwable t
                (log/error "callback-timer" {:error t})))))
        (recur))
      (log/info "stopped callback-timer loop"))))

(comment

  (reset! active-timer? false)

  (do
    (reset! active-timer? true)
    (callback-timer 1000))

  )

(defn add-timed-callback [timer-state amount callback]
  (let [index (->>
                (:ticks timer-state)
                (map-indexed (fn [i e]
                                  (when (= (:amount e) amount)
                                    i)))
                (keep identity)
                first)]
    (if index
      (update-in timer-state [:ticks index :callbacks] conj callback)
      (update timer-state :ticks conj {:amount amount
                                       :callbacks [callback]}))))

;; add callback if there is a tick that exists
(comment
  (let [timer-state {:ticks [
                             {:amount 2
                              :callbacks [:b]}
                             {:amount 5
                              :callbacks [:b]}]}
       callback :c
       amount 3]
    (-> (add-timed-callback timer-state amount callback)
      (add-timed-callback amount callback)))

  (let [timer-state {:ticks [{:amount 5
                              :callbacks [:b]}]}
        callback :c
        amount 5]
    (add-timed-callback timer-state amount callback))

  )

;; two loops
;; one for adding things to the loop
;; one for the timers

;; for now will use an atom w/ vector, but should update to be
;; some java queue most likely
(defn callback-input
  "Builds the go loop that runs on a timer"
  []
  (let [input-ch (async/chan)]
    (log/info "started callback-input loop")
    (go-loop [message (<! input-ch)]
      (if (nil? message)
        (log/info "stopped callback-input loop")
        (let [{:keys [ticks callback]} message]
          (log/info "callback-input message" message (type callback))
          (try
            (swap! timed-callbacks add-timed-callback ticks callback)
            (catch Throwable t
              (log/error "callback-input" {:error t})))
          (recur (<! input-ch)))))
    input-ch))

(defonce callback-input-ch (atom nil))

(defn update-after-ticks! [ticks callback]
  (if @callback-input-ch
    (async/put! @callback-input-ch
      {:ticks    ticks
       :callback callback})
    :error-timer-down))

(comment
  (reset! callback-input-ch (callback-input))

  ;; hmm for some reason this throws an error
  (do
    (update-after-ticks! 1 #(do (log/info "hehe")))
    (update-after-ticks! 1 #(do (log/info "hhp"))))

  )

(defrecord Timer [input-ch tick-length-ms]
  component/Lifecycle
  (start [component]
    (log/info "starting Timer")
    (reset! active-timer? true)
    (callback-timer tick-length-ms)
    (let [callback-input-ch* (callback-input)]
      (reset! callback-input-ch callback-input-ch*)
      (assoc component :input-ch callback-input-ch*)))
  (stop [component]
    (log/info "stopping Timer")
    (reset! active-timer? false)
    (let [input-ch (:input-ch component)]
      (reset! callback-input-ch nil)
      (when input-ch (async/close! input-ch))
      (assoc component :input-ch nil))))
