(ns traffic-lights.events
  (:require
    [re-frame.core :as re-frame]
    [statecharts.re-frame :as scr]
    [statecharts.core :as sc]
    [statecharts.path :as path]))

;; Statechart can be defined globally and then put into re-frame, see ::initialize-db
;;
;; This statechart consists of 2 top level states, each has several internal state machines
(def statechart
  {:type   :xor                                             ;; XOR - only 1 substate is active at a time
   :init   :working                                         ;; initialize to :working state uppon re-frame initialization
   :states {:working {:type        :xor
                      :init        :red
                      :enter       [(scr/ctx-assoc-db-in [:current-mode] :working) ;; set current mode into db
                                    (scr/ctx-assoc-db-in [:lights] {:red    false ;; reset traffic lights
                                                                    :yellow false
                                                                    :green  false})]
                      :states      {:red        {:enter       [(scr/ctx-assoc-db-in [:lights] {:red true})
                                                               (scr/ctx-conj-fx :dispatch-later {:ms       2000
                                                                                                 :dispatch [:prepare]})]
                                                 :exit        [(scr/ctx-assoc-db-in [:lights] {:red false})]
                                                 :transitions [{:event  :prepare
                                                                :target (path/sibling :red-yellow)}]}
                                    :red-yellow {:enter       [(scr/ctx-assoc-db-in [:lights] {:red    true
                                                                                               :yellow true})
                                                               (scr/ctx-conj-fx :dispatch-later {:ms       700
                                                                                                 :dispatch [:proceed]})]
                                                 :exit        [(scr/ctx-assoc-db-in [:lights] {:red    false
                                                                                               :yellow false})]
                                                 :transitions [{:event  :proceed
                                                                :target (path/sibling :green)}]}
                                    :green      {:enter       [(scr/ctx-assoc-db-in [:lights] {:green true})
                                                               (scr/ctx-conj-fx :dispatch-later {:ms       2000
                                                                                                 :dispatch [:warning]})]
                                                 :exit        [(scr/ctx-assoc-db-in [:lights] {:green false})]
                                                 :transitions [{:event  :warning
                                                                :target (path/sibling :yellow)}]}
                                    :yellow     {:enter       [(scr/ctx-assoc-db-in [:lights] {:yellow true})
                                                               (scr/ctx-conj-fx :dispatch-later {:ms       700
                                                                                                 :dispatch [:stop]})]
                                                 :exit        [(scr/ctx-assoc-db-in [:lights] {:yellow false})]
                                                 :transitions [{:event  :stop
                                                                :target (path/sibling :red)}]}}
                      :transitions [{:event  :toggle
                                     :target (path/sibling :idle)}]}
            :idle    {:type        :xor
                      :init        :off
                      :enter       [(scr/ctx-assoc-db-in [:current-mode] :idle)
                                    (scr/ctx-assoc-db-in [:lights] {:red    false
                                                                    :yellow false
                                                                    :green  false})]
                      :states      {:off {:enter       [(scr/ctx-conj-fx :dispatch-later {:ms       500
                                                                                          :dispatch [:tick]})]
                                          :transitions [{:event  :tick
                                                         :target (path/sibling :on)}]}
                                    :on  {:enter       [(scr/ctx-assoc-db-in [:lights] {:yellow true})
                                                        (scr/ctx-conj-fx :dispatch-later {:ms       600
                                                                                          :dispatch [:tick]})]
                                          :exit        [(scr/ctx-assoc-db-in [:lights] {:yellow false})]
                                          :transitions [{:event  :tick
                                                         :target (path/sibling :off)}]}}
                      :transitions [{:event  :toggle
                                     :target (path/sibling :working)}]}}})

;; Initialize db and statecharts
(re-frame/reg-event-fx
  ::initialize-db
  (fn [_ _]
    (-> {:db {}}
        ;; DB needs to be initialized with statecharts model, statecharts needs to be "compiled", aka cache layer
        ;; for internal states is created
        (scr/initialize (sc/make statechart)))))

;; Subscriptions
(re-frame/reg-sub
  :light-on?
  (fn [db [_ light]]
    (get-in db [:lights light])))

(re-frame/reg-sub
  :current-mode
  (fn [db]
    (:current-mode db)))

(re-frame/reg-sub
  :current-state
  (fn [db]
    (get-in db [:statecharts.re-frame/configuration :configuration])))