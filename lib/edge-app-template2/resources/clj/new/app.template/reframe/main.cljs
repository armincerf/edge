(ns ^:figwheel-hooks {{root-ns}}.frontend.main
  ;; Be careful when running cljr in this namespace, multiple requires are
  ;; needed for the frontend to function but cljr thinks it can get rid of
  ;; them!!
  (:require [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [{{root-ns}}.frontend.views :as views]
            [{{root-ns}}.frontend.subs]
            [{{root-ns}}.frontend.routes :as routes]
            [{{root-ns}}.frontend.events :as events]))

(defn main-panel
  []
  (let [active-panel (rf/subscribe [:active-panel])]
    [:<>
     [views/show-panel @active-panel]]))

(defn mount-root
  []
  (when-let [section (.getElementById js/document "app")]
    (rf/clear-subscription-cache!)
    (reagent/render [main-panel] section)))

(defn init
  []
  (routes/app-routes)
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [::events/init])
  (mount-root))

(defn ^:after-load reload
  []
  (mount-root))

(defonce run (init))
