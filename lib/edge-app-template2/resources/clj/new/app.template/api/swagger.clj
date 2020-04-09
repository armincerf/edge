(ns {{root-ns}}.api.swagger
  (:require [integrant.core :as ig]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [spell-spec.alpha :as spell]
            [{{root-ns}}.api.spec :as spec]))

(defmethod ig/pre-init-spec :{{root-ns}}.api/swagger
  [_]
  (spell/keys :opt-un [::spec/middleware]))

(defmethod ig/init-key :{{root-ns}}.api/swagger
  [_ {:keys [middleware]
      :or {middleware []}}]
  ["" {:no-doc true
       :no-diffs true
       :middleware middleware}
   ["/swagger.json" {:name ::swagger-json
                     :swagger {:info {:title "Boredgame API"
                                      :version "1.0.0"}}
                     :get (swagger/create-swagger-handler)}]
   ["/api-docs/*" {:name ::swagger-html
                   :get (swagger-ui/create-swagger-ui-handler)}]])
