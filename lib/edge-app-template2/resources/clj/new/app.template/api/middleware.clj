(ns {{root-ns}}.api.middleware
  (:require [camel-snake-kebab.core :as case]
            [camel-snake-kebab.extras :as cske]
            [clojure.spec.alpha :as s]
            [manifold.deferred :as d]
            [ring.middleware.cookies :as ring.mw.cookies]
            [ring.middleware.proxy-headers :as proxy-headers]
            [ring.middleware.not-modified :as not-modified]
            [ring.middleware.x-headers :as x-headers]
            [ring.util.response :as resp]
            [ring.util.time :as ring.util.time]
            [{{root-ns}}.api.coercer :as {{root-ns}}.api.coercer]
            [{{root-ns}}.api.errors :as errors]
            [{{root-ns}}.api.utils.ring :as utils.ring])
  (:import java.time.Instant
           java.util.Date))

(defn wrap-no-cache
  "Add 'Cache-Control: no-cache' to every response.

   Used to avoid serving stale resources during development
   see: https://danielcompton.net/2018/03/21/how-to-serve-clojurescript"
  [handler]
  (fn [req respond raise]
    (handler req
             #(respond (update % :headers assoc "Cache-Control" "no-cache"))
             raise)))

(defn wrap-not-found
  [handler]
  (fn [req respond raise]
    (handler req
             #(respond (or % {:status 404}))
             raise)))

(defn remove-last-modified
  "Removes the Last-Modified header from a response

  ClojureScript uses the JS file modification times to detect changes to source files.
  This is not useful to provide when serving compiled ClojureScript, and can lead
  to both over and under-caching.
  See https://danielcompton.net/2018/03/21/how-to-serve-clojurescript for more details."
  [handler]
  (fn [req respond raise]
    (handler req
             (fn [resp] (respond (utils.ring/remove-header resp "Last-Modified")))
             raise)))

(def not-modified
  {:name :ring.middleware.not-modified/wrap-not-modified
   :wrap not-modified/wrap-not-modified})

(defn parse-last-modified
  [properties]
  (when-let [last-modified (or (:last-modified properties)
                               (when-let [updated-at (:updated-at properties)]
                                 (Date/from ^Instant updated-at)))]
    (ring.util.time/format-date last-modified)))

(defn last-modified-response
  [request properties]
  (let [last-modified (parse-last-modified properties)
        ;; we build a fake response since ring middleware expects you are doing this
        ;; after processing the handler
        fake-resp {:status 200
                   :headers (cond-> {}
                              last-modified (assoc "Last-Modified" last-modified)
                              (:etag properties) (assoc "ETag" (:etag properties)))}
        resp (not-modified/not-modified-response fake-resp request)]
    ;; request/response suggest using a 304
    (when (= 304 (:status resp))
      resp)))

(def request-properties
  {:name ::request-properties
   :compile (fn properties-compile [{:keys [properties]} _]
              (when properties
                (fn properties-mw [handler]
                  (fn
                    ([request]
                     (let [properties (properties request)]
                       (if (false? (:exists? properties))
                         (throw (errors/exception :not-found {:uri (:uri request)}))
                         (if-let [resp (last-modified-response request properties)]
                           resp ;; appropriate to use 304 response
                           (handler (assoc request :properties properties))))))
                    ([request respond raise]
                     (let [properties (properties request)]
                       (if (false? (:exists? properties))
                         (throw (errors/exception :not-found {:uri (:uri request)}))
                         (if-let [resp (last-modified-response request properties)]
                           (respond resp)
                           (handler (assoc request :properties properties)
                                    respond
                                    raise)))))))))})

(defn wrap-ring-async-handler
  ;; TODO: switch to aleph.http/wrap-ring-async-handler once we are using
  ;; 0.4.7, https://github.com/ztellman/aleph/commit/4f654b0c26128e156784109069de59dd532387f3
  "Converts given asynchronous Ring handler to Aleph-compliant handler.

    More information about asynchronous Ring handlers and middleware:
    https://www.booleanknot.com/blog/2016/07/15/asynchronous-ring.html"
  [handler]
  (fn [request]
    (let [response (d/deferred)]
      (handler request #(d/success! response %) #(d/error! response %))
      response)))

(def deferred-handler
  {:name ::deferred-handler
   :wrap (fn [handler]
           (fn [request respond raise]
             (try
               (let [response (handler request)]
                 (if (d/deferred? response)
                   (d/on-realized response respond raise)
                   (respond response)))
               (catch Throwable t
                 (raise t)))))})


(defn referrer-policy-response
  [response policy]
  (resp/header response "Referrer-Policy" (name policy)))

(defn wrap-referrer-policy
  "Sets a referrer policy for the response.
  See https://scotthelme.co.uk/a-new-security-header-referrer-policy/ for more details."
  [handler options]
  (fn
    ([request]
     (referrer-policy-response (handler request) options))
    ([request respond raise]
     (handler request #(respond (referrer-policy-response % options)) raise))))

(defn transform-snake-keys [m]
  (cske/transform-keys {{root-ns}}.api.coercer/memoized->snake_case m))

(def snake-case-response-middleware
  {:name ::snake-case-response
   :compile (fn snake-case-response-compile [{:keys [no-snake-case?]} _]
              (when-not no-snake-case?
                (fn case-converter-middleware [handler]
                  (fn case-converter
                    ([request]
                     (-> request
                         handler
                         (update :body transform-snake-keys)))
                    ([request respond raise]
                     (handler request
                              #(respond (update % :body transform-snake-keys))
                              raise))))))})

(defn ->kebab-case
  [coll]
  (cske/transform-keys case/->kebab-case-keyword coll))

(def kebab-case-params
  {:name ::kebab-case-params
   :compile
   (fn [_ _]
     (fn [handler]
       (fn
         ([request]
          (-> request
              (update :path-params ->kebab-case)
              (update :query-params ->kebab-case)
              (update :form-params ->kebab-case)
              (update :body-params ->kebab-case)
              (handler)))
         ([request respond raise]
          (handler
           (update request :path-params ->kebab-case)
           respond
           raise)))))})
