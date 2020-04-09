(ns dev
  (:require
   [dev-extras :refer :all]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.set :as set]
   [clojure.spec.alpha :as s]
   clojure.tools.namespace.repl
   [expound.alpha :as expound]
   [integrant.core :as ig]
   [reitit.core :as r]
   spell-spec.expound
   {{root-ns}}.api.spec))

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(alter-var-root #'s/*explain-out* (constantly expound/printer))

;; easy system access

(defn db []
  (val (ig/find-derived-1 system :juxt.crux.ig/system)))

(defn router []
  (val (ig/find-derived-1 system :{{root-ns}}.api/router)))

(defn ig-lookup [k]
  (val (ig/find-derived-1 system k)))

;; convenient repl commands

;; eg (table *1) or (table (list-routes)) etc.
(def table pprint/print-table)

(defn list-routes
  "Lists all app routes + method. Helpful to debug routing issues."
  []
  (mapcat (fn [[route route-data]]
            (let [methods (set/intersection (set (keys route-data))
                                            #{:options :get :post :put :delete :head})]
              (if (seq methods)
                (map (fn [method]
                       {:method method
                        :route route
                        :name (:name route-data)})
                     methods)
                [{:method :any
                  :route route}])))
          (r/routes (router))))

(defn- inspect-route [match method]
  (let [route-info (get-in match [:result (keyword method)])]
    (merge (select-keys (:data route-info) [:name :middleware :parameters])
           (select-keys route-info [:path :method]))))

(defn inspect-route-by-path [path method]
  (-> (router)
      (r/match-by-path path)
      (inspect-route method)))

(defn inspect-route-by-name [route-name method]
  (-> (router)
      (r/match-by-name route-name)
      (inspect-route method)))
