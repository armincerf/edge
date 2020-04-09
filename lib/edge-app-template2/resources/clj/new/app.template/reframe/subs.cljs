(ns {{root-ns}}.frontend.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :active-panel
 :<- [:active-page]
 (fn [active-page _]
   (:page/panel active-page)))

(rf/reg-sub
 :active-page-title
 :<- [:active-page]
 (fn [active-page _]
   (or (:page/panel-title active-page)
       (:page/title active-page))))

(rf/reg-sub
 :active-page
 (fn [db _]
   (:active-page db)))

(rf/reg-sub
 :route-data
 (fn [db _]
   (:route-data db)))

(rf/reg-sub
 :route-params
 :<- [:route-data]
 (fn [route-data]
   (get route-data :route-params)))
