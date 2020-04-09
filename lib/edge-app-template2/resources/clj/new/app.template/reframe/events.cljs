(ns {{root-ns}}.frontend.events
  (:require [pushy.core :as pushy]
            [re-frame.core :as rf]
            [reitit.core :as reitit]
            [{{root-ns}}.frontend.db :as db]
            [{{root-ns}}.frontend.routes :as routes])
  (:import goog.Uri
           goog.Uri.QueryData))

;; reg-fx should be kept as simple as possible.
;; ref: https://github.com/Day8/re-frame/blob/master/docs/Effects.md
(rf/reg-fx
 :location
 (fn [location]
   (set! (.-href js/window.location) location)))

(rf/reg-fx
 :pushy
 (fn [value]
   (pushy/set-token! routes/history value)))

(rf/reg-fx
 :title
 (fn [value]
   (set! (.. js/document -title) value)))

(defn- scroll-to-top
  []
  (set! (.. js/document -body -scrollTop) 0)
  (set! (.. js/document -documentElement -scrollTop) 0))

(rf/reg-fx
 :scroll
 (fn [value]
   (if (= value :top)
     (scroll-to-top)
     (js/console.error "Other scroll values not yet supported"))))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/app-db))

(rf/reg-event-fx
 :set-active-page
 (fn [{:keys [db]} [_ active-page route-data]]
   {:db (-> db
            (assoc :active-page active-page)
            (assoc :route-data route-data)
            (dissoc :page-state))
    :scroll :top}))

(rf/reg-event-fx
 :url-change
 (fn [{:keys [db]} [_ url]]
   (let [route-data (reitit/match-by-path routes/router url)
         page (get routes/pages (get-in route-data [:data :name]))]
     (prn page url route-data)
     {:dispatch-n (concat
                   [[:set-active-page page route-data]
                    (when (:page/dispatch page)
                      [(:page/dispatch page)])
                    (when-let [dispatch-n (:page/dispatch-n page)]
                      dispatch-n)])
      :title (:page/title page)
      :db (dissoc db :show-menu :show-burger-menu)
      :scroll :top})))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   ;(add-global-messages)
   {}))
