(ns {{root-ns}}.api.coercer
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.core.memoize :as memo]
            [clojure.walk :as walk]
            [reitit.coercion :as coercion]
            [reitit.coercion.spec :as coercion.spec]
            [spec-tools.core :as st]))

(def memoized->kebab-case
  (memo/fifo csk/->kebab-case-keyword
             {}
             :fifo/threshold 512))

(def memoized->snake_case
  (memo/fifo csk/->snake_case_keyword
             {}
             :fifo/threshold 512))

(defn kebab-keys
  [_ x]
  (if (map? x)
    (cske/transform-keys memoized->kebab-case x)
    x))

(def kebab-transformer
  "Transformer that transforms data to kebab-case"
  (st/type-transformer
   {:name ::kebab
    :decoders {:map kebab-keys}}))

(def json-transformer
  (st/type-transformer
   st/strip-extra-keys-transformer
   st/json-transformer
   kebab-transformer))

(def string-transformer
  (st/type-transformer
   st/string-transformer
   kebab-transformer))

(defn snake_case-required
  "Converts Swagger 'required' fields case (i.e. [foo-bar baz] to [foo_bar baz])"
  [m]
  (walk/postwalk (fn [x]
                   (cond-> x
                     (and (map? x)
                          (coll? (:required x)) ;; beware of required booleans
                          (:required x))
                     (update :required #(mapv memoized->snake_case %))))
                 m))

(defn snake_case-schema
  "Converts Swagger 'schema' keys to be snake_case"
  [param-or-resp]
  (cond-> param-or-resp
    (and (map? param-or-resp)
         (:schema param-or-resp))
    (update :schema #(cske/transform-keys memoized->snake_case %))))

(defn snake_case-parameters
  "Converts Swagger parameter definitions to be snake_case"
  [parameters]
  (map (comp #(update % :name memoized->snake_case)
             snake_case-schema
             snake_case-required)
       parameters))

(defn snake_case-responses
  "Converts Swagger response definitions to be snake_case"
  [responses]
  (zipmap (keys responses)
          (map (comp snake_case-schema snake_case-required)
               (vals responses))))

(def coercion
  "Custom coercer which allows us to modify JSON parsed requests before they are coerced
  using the provided spec"
  ^{:type ::coercion/coercion}
  (let [kebab-spec-coerce (coercion.spec/create {:coerce-response? coercion.spec/coerce-response?
                                                 :transformers {:body {:default st/strip-extra-keys-transformer
                                                                       :formats {"application/json" json-transformer}}
                                                                ;; query, form, header, path
                                                                :string {:default string-transformer}
                                                                :response {:default coercion.spec/no-op-transformer}}})]
    (reify coercion/Coercion
      (-get-name [_] :{{root-ns}})
      (-get-options [_] (coercion/-get-options kebab-spec-coerce))
      (-get-apidocs [_ specification specs]
        (let [{:keys [parameters responses]} (coercion/-get-apidocs kebab-spec-coerce specification specs)]
          {:responses (snake_case-responses responses)
           :parameters (snake_case-parameters parameters)}))
      (-compile-model [_ model name]
        (coercion/-compile-model kebab-spec-coerce model name))
      (-open-model [_ spec]
        (coercion/-open-model kebab-spec-coerce spec))
      (-encode-error [_ error]
        (coercion/-encode-error kebab-spec-coerce error))
      (-request-coercer [_ type spec]
        (coercion/-request-coercer kebab-spec-coerce type spec))
      (-response-coercer [_ spec]
        (coercion/-response-coercer kebab-spec-coerce spec)))))
