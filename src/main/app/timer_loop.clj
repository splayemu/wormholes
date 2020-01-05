(ns app.timer-loop
  (:require
   [clojure.core.async :as async :refer [go go-loop <! timeout alts!]]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as log]))

(defonce timed-callbacks (atom {:ticks []}))

(defn decrement-timers [timer-state]
  (let [decrement-tick #(update % :amount (fnil dec 0))]
    (update timer-state :ticks #(map decrement-tick %))))

;; for each timer, decrement
;; if any of the timers are zero, remove
(comment
  (let [timer-state {:ticks [{:amount 5
                             :callbacks [:b]}
                            {:amount 4
                             :callbacks [:b]}]}]
    (decrement-timers timer-state))

  )

;; must return a vector for ticks as update-in doesn't work on lists
(defn pop-callbacks [timer-state]
  (let [active-callbacks (->> (:ticks timer-state)
                           (filter #(<= (:amount %) 0))
                           (mapcat :callbacks)
                           concat)
        filtered-ticks (->> (:ticks timer-state)
                         (remove #(<= (:amount %) 0))
                         (into []))]
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

(defn process-tick!
  "Advances the ticks of callback-timer-atom and invokes the stored callbacks."
  [callback-timer-atom]
  (let [timer-state (swap! callback-timer-atom #(-> %
                                                  decrement-timers
                                                  pop-callbacks))
        active-callbacks (:active-callbacks timer-state)]
    (swap! callback-timer-atom assoc :active-callbacks [])
    (doseq [callback active-callbacks]
      (try
        (callback)
        (catch Throwable t
          (log/error "callback-timer" {:error t}))))))

(defonce callback-timer-kill-ch (atom nil))
(defn callback-timer
  "Builds the go loop that processes one tick each tick-length-ms.

  The timer loop is running constantly so if callbacks are added halfway through
  a tick, those callbacks won't wait for the full duration of that tick.

  Shut it down by calling `kill-callback-timer!`."
  [tick-length-ms]
  (log/info "started callback-timer loop")
  (let [kill-ch (async/chan)]
    (go-loop []
      (let [timeout-ch (timeout tick-length-ms)
            [val ch] (alts! [timeout-ch kill-ch])]
        (if (= ch timeout-ch)
          (do
            (process-tick! timed-callbacks)
            (recur))
          (log/info "stopped callback-timer loop"))))
    (reset! callback-timer-kill-ch kill-ch)))

(defn kill-callback-timer! []
  (when @callback-timer-kill-ch
    (async/put! @callback-timer-kill-ch :die)
    (reset! callback-timer-kill-ch nil)))

(comment
  (def kill-ch
    (callback-timer 1000))

  )

(defn add-timed-callback
  "Updates a timer-state map to include a callback indexed by a number of ticks."
  [timer-state amount callback]
  (let [tick-index (->> (:ticks timer-state)
                     (map-indexed (fn [i e]
                                    (when (= (:amount e) amount)
                                      i)))
                     (keep identity)
                     first)]
    (if tick-index
      (update-in timer-state [:ticks tick-index :callbacks] conj callback)
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

  (let [timer-state {:ticks []}
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

(defn callback-input
  "Builds the go loop that adds input to the callback-timer."
  []
  (let [input-ch (async/chan)]
    (log/info "started callback-input loop")
    (go-loop [message (<! input-ch)]
      (if (nil? message)
        (log/info "stopped callback-input loop")
        (let [{:keys [ticks callback]} message]
          (log/info "callback-input message" message)
          (try
            (swap! timed-callbacks add-timed-callback ticks callback)
            (catch Throwable t
              (log/error "callback-input" {:error t})))
          (recur (<! input-ch)))))
    input-ch))

(defonce callback-input-ch (atom nil))

(defn callback-after-ticks-ticks!
  "After waiting the number of ticks, asynchronously run callback."
  [ticks callback]
  (if @callback-input-ch
    (async/put! @callback-input-ch
      {:ticks    ticks
       :callback callback})
    :error-timer-down))

(comment
  (reset! callback-input-ch (callback-input))

  ;; hmm for some reason this throws an error
  (do
    (callback-after-ticks-ticks! 1 #(do (log/info "hehe")))
    (callback-after-ticks-ticks! 1 #(do (log/info "hhp"))))

  )

(defrecord TimerLoop [tick-length-ms input-ch callback-timer-kill-ch]
  component/Lifecycle
  (start [component]
    (log/info "starting Timer")
    (let [callback-input-ch* (callback-input)
          callback-timer-kill-ch (callback-timer tick-length-ms)]
      (reset! callback-input-ch callback-input-ch*)
      (-> component
        (assoc :callback-timer-kill-ch callback-timer-kill-ch)
        (assoc :input-ch callback-input-ch*))))
  (stop [component]
    (log/info "stopping Timer")
    (let [input-ch (:input-ch component)]
      (reset! callback-input-ch nil)
      (kill-callback-timer!)
      (when input-ch (async/close! input-ch))
      (assoc component :input-ch nil :callback-timer-kill-ch nil))))
