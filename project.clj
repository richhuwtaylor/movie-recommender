(defproject movie-recommender "0.1.0-SNAPSHOT"
  :description "Makes movie recommendations for user in the MovieLens 100k dataset"
  :url "hhttps://github.com/richhuwtaylor/movie-recommender"
  :dependencies [[com.google.guava/guava "16.0"]
                 [expectations "2.2.0-rc3"]
                 [gorillalabs/sparkling "1.2.2"]
                 [incanter "1.5.7"]
                 [medley "1.0.0"]
                 [org.apache.mahout/mahout-core "0.9" :exclusions [com.google.guava/guava]]
                 [org.apache.mahout/mahout-examples "0.9" :exclusions [com.google.guava/guava]]
                 [org.apache.spark/spark-mllib_2.10 "1.1.0" :exclusions [com.google.guava/guava]]
                 [org.apache.spark/spark-core_2.10 "1.1.0" :exclusions [com.google.guava/guava com.thoughtworks.paranamer/paranamer]]
                 [org.clojure/clojure "1.8.0"]]
  :resource-paths ["data/ml-100k"]
  :jvm-opts ["-Xmx4g"])
