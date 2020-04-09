(ns {{root-ns}}.api.routes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [hiccup.page :as hiccup]
            [integrant.core :as ig]
            [reitit.ring :as ring]
            [{{root-ns}}.api.spec :as spec]
            [ring.util.http-response :as response]
            [spec-tools.data-spec :as ds]
            [spell-spec.alpha :as spell]))

(defn- index-html
  "main index."
  [request]
  (hiccup/html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta
     {:name "viewport"
      :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:link {:rel "icon" :href "/favicon.ico" :type "image/x-icon"}]
    [:title "Welcome to BoredGame"]
    (hiccup/include-css "/css/boredgame.css"
                        "/webjars/font-awesome/4.6.3/css/font-awesome.min.css"
                        "https://fonts.googleapis.com/css?family=Montserrat:200,300,400,500,600,800&display=swap")]
   [:body
    [:div#app
     [:div.pageloader.is-active
      [:span.title "Loading..."]]]
    [:script "localStorage.setItem(\"day8.re-frame-10x.show-panel\",\"\\\"false\\\"\")"]
    (hiccup/include-js "/frontend.js")]))

(s/def ::components
  (s/map-of keyword? some?))

(defmethod ig/pre-init-spec ::routes
  [_]
  (spell/keys :req-un [::components ::spec/middleware]))

(defmethod ig/init-key ::routes
  [_ {:keys [middleware components]
      :or {middleware []}}]
  ["/" {:no-doc true
        :middleware middleware}
   ["game/*" {:name :home
              :get {:handler (fn handle-index
                               [req]
                               (-> (response/ok (index-html req))
                                   (response/content-type "text/html")))}}]
   ["webjars/*"
    {:get {:handler (ring/create-resource-handler {:root "META-INF/resources/webjars"})
           :no-diffs true}}]])
