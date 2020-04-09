(ns {{root-ns}}.api.utils.route
  (:require [reitit.core :as r]))

(defn route-name->path
  "Given a request, the reitit route name, and an optional map of path-param or
  query-param keys to values, provides a path to the corresponding resource

  For example, given a route specified as:
  [\"/resource/:id\"
    {:name ::get-resource
     :parameters {:path {:id string?}
                  :query {:foo int?}}]

  (route-name->path req ::get-resource {:path-params {:id
  \"abcd\"} :query-params 12}) -> \"/resource/abcd?foo=12\""
  ([request route-name] (route-name->path request route-name nil))
  ([request route-name {:keys [path-params query-params]}]
   (-> (r/match-by-name (::r/router request)
                        route-name
                        (or path-params {}))
       (r/match->path (or query-params {})))))
