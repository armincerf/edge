(ns {{root-ns}}-test
  "End to end tests"
  (:require [{{root-ns}}.fixtures :as f :refer [*system*]]
            [clojure.test :as t]
            [yada.yada :as yada]))

(t/use-fixtures :once f/with-system)

(t/deftest api-requests
  (t/testing "Not much"
    (t/is (= 1 1))))
