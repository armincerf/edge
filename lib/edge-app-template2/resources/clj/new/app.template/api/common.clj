(ns {{root-ns}}.api.common
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log])
  (:import java.lang.Exception))

(defn exception? [e]
  (instance? Exception e))

(s/fdef log-throw
  :args (s/alt :one (s/cat :e exception?)
               :two (s/cat :e exception?
                           :msg string?)
               :three (s/cat :e exception?
                             :msg string?
                             :extra-log-info (s/nilable map?))))

(defn log-throw
  "Takes an exception and an optional error message and an optional extra info
  map. Logs the ex-message and ex-data of the exception at the error level by
  default. Instead logs `msg` if provided. Merges `extra-log-info` over the
  ex-data if provided. Finally, throws exception `e`."
  ([e]
   (log-throw e (ex-message e)))
  ([e msg]
   (log-throw e msg nil)
   (throw e))
  ([e msg extra-log-info]
   (let [data (ex-data e)]
     (log/error e msg (-> (if (= :{{root-ns}}.api.errors/exception (:type data))
                            (:info data)
                            data)
                          (merge extra-log-info))))
   (throw e)))
