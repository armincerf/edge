(ns {{root-ns}}.frontend.routes
  (:require [pushy.core :as pushy]
            [reitit.core :as reitit]
            [re-frame.core :as rf]))

(defn- dispatch-route!
  [url]
  (rf/dispatch [:url-change url]))

(def history (pushy/pushy dispatch-route! identity))

(defn app-routes
  []
  (pushy/start! history))

(def pages
  {:home #:page{:title "Home"
                :panel-title "Thanks"
                :panel :home-panel
                :dispatch [:load-home-page]}})

(def router
  (reitit/router
   [["/game/home" :home]]))


