(ns {{root-ns}}.api.utils.dates
  (:require [clojure.instant :as clojure.instant]
            [{{root-ns}}.api.utils.time :as time]
            [tick.alpha.api :as tick]
            [tick.format :as tick.format])
  (:import java.time.format.DateTimeFormatter
           java.time.LocalDate))

(def date-formatter (tick.format/formatter "yyyy/MM/dd")) ;; TODO add to config

;; e.g January 27, 2020
(def month-day-year-formatter (tick.format/formatter "MMMM dd, yyyy"))

(defn format-month-day-year
  "Return `date` in `month-day-year` format"
  [date]
  (tick/format month-day-year-formatter date))

(defn format-date
  "Return `date` in `date-format` format."
  [date]
  (tick/format date-formatter date))

(defn parse-date-str
  "Convert `date-str` into sql time for writting to the db"
  [date-str]
  (time/to-sql-time (clojure.instant/read-instant-date date-str)))

(defn first-day-of-month
  "Get the first day of the current month."
  []
  (tick/instant
    (.withDayOfMonth (tick/date-time) 1)))

(defn last-day-of-month
  "Get the last day of the current month."
  []
  (let [today (tick/date-time)]
    (tick/instant (.withDayOfMonth today (.lengthOfMonth (tick/date))))))

(defn record-str->date
  [record k]
  (if (k record)
    (assoc record k (parse-date-str (k record)))
    record))

(defn iso-format-date-str?
  "True if date-str parses to an ISO_DATE (w/ or w/out offset) formatted date.
   e.g. '2011-12-03+01:00'; '2011-12-03'"
  [date-str]
  (try (some? (LocalDate/parse date-str DateTimeFormatter/ISO_DATE))
       (catch java.time.format.DateTimeParseException _
         false)))
