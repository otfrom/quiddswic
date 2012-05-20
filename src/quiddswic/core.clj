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

(stats/mean (incanter/$ :beer s2))

(def
  sm
  (incanter/col-names
   (incanter/$rollup (fn [coll] (stats/mean coll)) :beer :dow s2)
   [:season :mean]))

(def sm-ord (incanter/$order :season :asc sm))

(def sm-sd
  (let [sd1 (stats/sd (incanter/$ :beer s2))
        sd1-high (incanter/$map (fn [m] (+ sd1 m)) [:mean] sm-ord)
        sd1-low (incanter/$map (fn [m] (- m sd1)) [:mean] sm-ord)
        sd2-high (incanter/$map (fn [m] (+ (* 2 sd1) m)) [:mean] sm-ord)
        sd2-low (incanter/$map (fn [m] (- m (* 2 sd1))) [:mean] sm-ord)]
    (-> (incanter/conj-cols sm-ord sd1-high sd1-low sd2-high sd2-low)
        (incanter/col-names [:season :mean :sd1-high :sd1-low :sd2-high :sd2-low]))))

(def s3 (incanter/$join [:season :dow] sm-sd s2))

(incanter/head s3)

(def s-alerts
  (-> (incanter/$map
       (fn [m high low]
         (cond (> m high) 1
               (< m low) -1
               :else 0))
       [:beer :sd1-high :sd1-low] s3)
      (incanter/col-names [:alert])
      (incanter/conj-cols s3)))

(def april-2011
  (incanter/$where
   {:millis
    {:$gte (coerce/to-long (time/date-time 2011 04 01))
     :$lte (coerce/to-long (time/date-time 2011 04 30))}}
   s-alerts))

(defn ts-plot [ts theme]
  (doto
      (charts/time-series-plot
       (incanter/$ :millis ts)
       (incanter/$ :beer ts)
       :x-label "Date"
       :y-label "Pints of Beer"
       :legend true
       :series-label "Pints over time")
    (charts/set-theme theme)))

(incanter/view (ts-plot s-alerts :dark))

(incanter/view (ts-plot april-2011 :dark))

(defn plot-alert-chart [ts theme]
  (doto
      (charts/time-series-plot
       :millis :beer
       :data ts
       :x-label "Date" :y-label "Pints of Beer"
       :series-label "Pints over time"
       :legend true)
    (charts/set-theme theme)
    (charts/add-lines
     (incanter/$ [:millis] ts)
     (incanter/$ [:sd1-low] ts)
     :series-label "Low Threshold")
    (charts/add-lines
     (incanter/$ [:millis] ts)
     (incanter/$ [:sd1-high] ts)
     :series-label "High Threshold")
    (charts/add-points
     :millis :beer :data
     (incanter/$where {:alert -1} ts)
     :series-label "Low beer alert!")
    ;; Main Metric
    (charts/set-stroke :dataset 0 :width 5)
    (charts/set-stroke-color java.awt.Color/yellow :dataset 0)
    ;; High Threshold
    (charts/set-stroke :dataset 1 :width 2 :dash 5)
    (charts/set-stroke-color java.awt.Color/green :dataset 1)
    ;; Low Threshold
    (charts/set-stroke :dataset 2 :width 2 :dash 5)
    (charts/set-stroke-color java.awt.Color/red :dataset 2)
    ;; Low Beer Alerts
    (charts/set-stroke :dataset 3 :width 10)
    (charts/set-stroke-color java.awt.Color/red :dataset 3)))

(incanter/view (plot-alert-chart s-alerts :dark))

(incanter/view (plot-alert-chart april-2011 :dark))
