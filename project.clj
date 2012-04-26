(defproject quiddswic "0.0.3"
    :description "Quick and Dirty Data Science with Incanter and Clojure"
    :dependencies [[org.clojure/clojure "1.3.0"]
                   [incanter "1.3.0"
                    :exclusions [swank-clojure
                                 org.clojure/clojure
                                 org.clojure/clojure-contrib
                                 org.clojars.bmabey/congomongo
                                 congomongo]]
                   [clj-time "0.3.3"
                    :exclusions [org.clojure/clojure
                                 org.clojure/clojure-contrib]]
                   [congomongo "0.1.7"
                    :exclusions [org.clojure/clojure
                                 org.clojure/clojure-contrib]]])
