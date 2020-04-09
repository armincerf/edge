(ns {{root-ns}}.api.router
  (:require [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            reitit.ring.spec
            [spell-spec.alpha :as spell]
            {{root-ns}}.api.coercer
            [{{root-ns}}.api.middleware :as mw]
            [{{root-ns}}.api.middleware.errors :as mw.errors]
            [{{root-ns}}.api.middleware.logging :as mw.logging]))

(s/def ::routes (s/coll-of vector?))

(s/def ::middleware (s/coll-of some?))

(s/def ::request-diffs #{:trace :debug :info :none})

(s/def ::pretty-router-exceptions? boolean?)

(defmethod ig/pre-init-spec :{{root-ns}}.api/router
  [_]
  (spell/keys :req-un [::routes]
              :opt-un [::middleware ::request-diffs
                       ::pretty-router-exceptions?]))

(defmethod ig/init-key :{{root-ns}}.api/router
  [_ {:keys [routes middleware request-diffs pretty-router-exceptions?]
      :or {middleware []}}]
  (ring/router
   [routes]
   (cond-> {:reitit.middleware/registry
            {;; query-params & form-params
             :parameters parameters/parameters-middleware
             ;; content-negotiation
             :format-negotiate muuntaja/format-negotiate-middleware
             ;; encoding response and error body
             :format-response muuntaja/format-response-middleware
             ;; custom error middleware
             :{{root-ns}}-errors mw.errors/{{root-ns}}-errors
             ;; decoding request body
             :format-request muuntaja/format-request-middleware
             ;; coercing response bodies
             :coerce-response coercion/coerce-response-middleware
             ;; coercing request parameters
             :coerce-request coercion/coerce-request-middleware
             ;; handling manifold
             :deferred mw/deferred-handler
             ;; allow properties fns
             :properties mw/request-properties
             ;; allows not-modified responses
             :not-modified mw/not-modified
             ;; converts snake response
             :snake-case-response mw/snake-case-response-middleware}
            :validate reitit.ring.spec/validate
            :data {:muuntaja m/instance
                   :coercion {{root-ns}}.api.coercer/coercion
                   :middleware middleware}}
     ;; pretty diffs
     (not= request-diffs :none)
     (assoc :reitit.middleware/transform
            (partial mw.logging/print-request-diffs request-diffs))
     ;; pretty router creation exceptions
     pretty-router-exceptions? (assoc :exception pretty/exception))))
