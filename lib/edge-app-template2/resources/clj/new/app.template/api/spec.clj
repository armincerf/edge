(ns {{root-ns}}.api.spec
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str])
  (:import [java.time Duration Instant Period]
           java.time.format.DateTimeParseException))

(s/def ::origin string?)

(s/def ::middleware
  (st/spec {:description "Middleware"
            :spec (s/* (s/alt :mw keyword?
                              :mw-with-ref (s/tuple keyword? some?)))}))

(s/def ::port
  (st/spec {:description "Port"
            :spec pos-int?}))
