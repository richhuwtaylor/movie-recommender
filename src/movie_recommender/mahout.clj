(ns movie-recommender.mahout
  (:require [clojure.java.io :as io]
            [incanter.charts :as c]
            [incanter.core :as i])
  (:import [org.apache.mahout.cf.taste.eval RecommenderBuilder]
           [org.apache.mahout.cf.taste.impl.eval GenericRecommenderIRStatsEvaluator RMSRecommenderEvaluator]
           [org.apache.mahout.cf.taste.impl.model GenericBooleanPrefDataModel]
           [org.apache.mahout.cf.taste.impl.model.file FileDataModel]
           [org.apache.mahout.cf.taste.impl.neighborhood NearestNUserNeighborhood]
           [org.apache.mahout.cf.taste.impl.recommender GenericBooleanPrefUserBasedRecommender GenericUserBasedRecommender]))

(defn load-model
  "Use FileDataModel utility class to load the data."
  [path]
  (-> (io/resource path)
      (io/file)
      (FileDataModel.)))

(defn recommender-builder
  "Create an anonymous RecommenderBuilder type that uses Mahout's
  buildRecommender interface."
  [n sim]
  (reify RecommenderBuilder
    (buildRecommender [this model]
      (let [nhood (NearestNUserNeighborhood. n sim model)]
        (GenericUserBasedRecommender. model nhood sim)))))

(defn evaluate-rmse
  "Construct a root-mean-square error evaluator using Mahout's
  RMSRecomenderEvaluator class for the supplied recommender builder
  and model.
  - 'nil' is passed so that the evaluate function uses the default model builder
  - 0.7 is the proportion of data used for training (70%)
  - 1.0 is the proportion of the remaining data to evaluate on (100%)
  Calculates the square root of the mean square error between the
  predicted and actual ratings for each of the test data."
  [builder model]
  (-> (RMSRecommenderEvaluator.)
      (.evaluate builder nil model 0.7 1.0)))

(defn evaluate-ir
  "Construct a GenericRecommenderIRStatsEvaluator. Evaluators checks
   how many of some quantity of each user's top-rated items were
   recommended.
   - 'nil' is passed so that the evaluate function uses the default model builder
   - 5 is the number of of top-rated items that are checked
   - (bean) call converts the org.apache.mahout.cf.taste.eval.\nIRStatistics
   instance returned into a Clojure map"
  [builder model]
  (-> (GenericRecommenderIRStatsEvaluator.)
      (.evaluate builder nil model nil 5
                 GenericRecommenderIRStatsEvaluator/CHOOSE_THRESHOLD
                 1.0)
      (bean)))

(defn plot-ir
  "Plots the stats of the GenericRecommenderIRStatsEvaluator."
  [xs stats]
  (-> (c/xy-plot xs (map :recall stats)
                 :x-label "Neighbourhood Size"
                 :y-label "IR Statistic"
                 :series-label "Recall"
                 :legend true)
      (c/add-lines xs (map :precision stats)
                   :series-label "Precision")
      (c/add-lines xs
                   (map :normalizedDiscountedCumulativeGain stats)
                   :series-label "NDCG")
      (i/view)))

(defn to-boolean-preferences
  "Converts the given model into a boolean preferences model."
  [model]
  (-> (GenericBooleanPrefDataModel/toDataMap model)
      (GenericBooleanPrefDataModel.)))

(defn boolean-recommender-builder
  "Create an anonymous RecommenderBuilder type that uses Mahout's
  buildRecommender interface."
  [n sim]
  (reify RecommenderBuilder
    (buildRecommender [this model]
      (let [nhood (NearestNUserNeighborhood. n sim model)]
        (GenericBooleanPrefUserBasedRecommender.
          model nhood sim)))))