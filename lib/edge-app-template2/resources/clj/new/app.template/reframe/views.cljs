(ns {{root-ns}}.frontend.views
  (:require [{{root-ns}}.frontend.home.views :as home.views]))

(defn show-panel
  [panel-name]
  (case panel-name
    :home-panel [home.views/main]
    [:div "no panel matching" panel-name]))
