(ns traffic-lights.views
  (:require
    [re-frame.core :as re-frame]
    ))

(def CIRCLE-STYLE {:width         "200px"
                   :height        "200px"
                   :border-radius "50%"
                   :border        "1px solid black"})

(defn main-panel []
  [:div

   [:button {:on-click (fn [e]
                         (re-frame/dispatch [:toggle]))}
    "Toggle mode: " (case @(re-frame/subscribe [:current-mode])
                      :working
                      "Working"
                      :idle
                      "Idle")]
   [:pre (with-out-str (cljs.pprint/pprint @(re-frame/subscribe [:current-state])))]
   [:hr]
   [:div {:style (cond-> CIRCLE-STYLE
                         @(re-frame/subscribe [:light-on? :red])
                         (assoc :background "red"))}]
   [:div {:style (cond-> CIRCLE-STYLE
                         @(re-frame/subscribe [:light-on? :yellow])
                         (assoc :background "yellow"))}]
   [:div {:style (cond-> CIRCLE-STYLE
                         @(re-frame/subscribe [:light-on? :green])
                         (assoc :background "green"))}]])
