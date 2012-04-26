;; Just copy and paste these lines into a repl.

(require '[clj-time.format :as tformat])
(require '[clj-time.core :as time])
(require '[clj-time.coerce :as coerce])
(require '[incanter.core :as incanter])
(require '[incanter.stats :as stats])
(require '[incanter.charts :as charts])
(require '[incanter.io :as io])
(use 'clojure.repl)

(def series
  (io/read-dataset "./beer.csv" :header true))

(def dts
  (incanter/dataset
   [:dts]
   (incanter/$map
    (fn [d] (tformat/parse (:year-month-day tformat/formatters) d))
    [:date]
    series)))

(def t
  (incanter/dataset
   [:millis]
   (incanter/$map
    (fn [d] (coerce/to-long d))
    [:dts]
    dts)))

(def s1 (incanter/conj-cols series dts t))

(incanter/head s1)

(incanter/view s1)

(def dow (incanter/dataset [:dow] (incanter/$map (fn [d] (time/day-of-week d)) [:dts] s1)))

(def s2 (incanter/conj-cols s1 dow))

(def
  sm
  (incanter/col-names
   (incanter/$rollup (fn [coll] (stats/mean coll)) :beer :dow s2)
   [:season :mean]))

(def sm-ord (incanter/$order :season :asc sm))

(def s3 (incanter/$join [:season :dow] sm s2))

(incanter/head s3)

(stats/mean (incanter/$ :beer s3))

(def ts-plot
  (charts/time-series-plot (incanter/$ :millis s1) (incanter/$ :beer s1)))

(incanter/view ts-plot)
