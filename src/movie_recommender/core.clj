(ns movie-recommender.core
  (:require [incanter.charts :as c]
            [incanter.core :as i]
            [incanter.stats :as stats]
            [movie-recommender.data :as data]
            [movie-recommender.mahout :as mahout]
            [movie-recommender.slope-one :as slope]
            [movie-recommender.sparkling :as sparkling]
            [sparkling.conf :as conf]
            [sparkling.core :as spark])
  (:import [org.apache.mahout.cf.taste.impl.eval RMSRecommenderEvaluator]
           [org.apache.mahout.cf.taste.impl.neighborhood NearestNUserNeighborhood]
           [org.apache.mahout.cf.taste.impl.recommender GenericUserBasedRecommender]
           [org.apache.mahout.cf.taste.impl.similarity EuclideanDistanceSimilarity TanimotoCoefficientSimilarity SpearmanCorrelationSimilarity PearsonCorrelationSimilarity]))

(defn first-user-top-ten
  "Builds a slope one recommender for the first user in the dataset.
  Returns the top ten recommendations for that user."
  []
  (let [user-ratings (->> (data/load-ratings "ua.base")
                          (group-by :user)
                          (vals))
        user-1       (first user-ratings)
        recommender  (->> (rest user-ratings)
                          (slope/slope-one-recommender))
        items        (data/load-items "u.item")
        item-name    (fn [item]
                       (get items (:item item)))]
    (->> (slope/slope-one-recommend recommender user-1 10)
         (map item-name))))

(defn evaluate-euclidean-similarity-performance
  []
  "Evaluate the performance of Euclidean similarity."
  (let [model   (mahout/load-model "ua.base")
        builder (mahout/recommender-builder
                  10
                  (EuclideanDistanceSimilarity. model))]
    (mahout/evaluate-rmse builder model)))

(defn evaluate-euclidean-similarly-performance-ir
  "Evaluate the performance of Euclidean similarity using GenericRecommenderIRStatsEvaluator.
  Plot the resulting stats."
  []
  (let [model (mahout/load-model "ua.base")
        sim   (EuclideanDistanceSimilarity. model)
        xs    (range 1 10)
        stats (for [n xs]
                (let [builder (mahout/recommender-builder n sim)]
                  (mahout/evaluate-ir builder model)))]
    (mahout/plot-ir xs stats)))

(defn plot-neighbourhood-size
  "Produce a plot of root mean square error as a function of neighbourhood size
  for the Euclidean distance similarity."
  []
  (let [model (mahout/load-model "ua.base")
        sim   (EuclideanDistanceSimilarity. model)
        ns    (range 1 10)
        stats (for [n ns]
                (let [builder (mahout/recommender-builder n sim)]
                  (mahout/evaluate-rmse builder model)))]
    (-> (c/scatter-plot ns stats
                        :x-label "Neighbourhood size"
                        :y-label "RMSE")
        (i/view))))

(defn evaluate-pearson-similarity-performance
  "Evaluate the performance of Pearson correlation similarity."
  []
  (let [model   (mahout/load-model "ua.base")
        builder (mahout/recommender-builder
                  10
                  (PearsonCorrelationSimilarity. model))]
    (mahout/evaluate-rmse builder model)))

(defn evaluate-spearman-similarity-performance
  "Evaluate the performance of the Spearman Rank correlation similarity."
  []
  (let [model   (mahout/load-model "ua.base")
        builder (mahout/recommender-builder
                  10
                  (SpearmanCorrelationSimilarity. model))]
    (mahout/evaluate-rmse builder model)))

(defn evaluate-boolean-preferences-performance
  "Evalute the performance of the Tanimoto Coefficient similarity.
   Plot the resulting stats."
  []
  (let [model (mahout/to-boolean-preferences (mahout/load-model "ua.base"))
        sim   (TanimotoCoefficientSimilarity. model)
        xs    (range 1 10)
        stats (for [n xs]
                (let [builder (mahout/boolean-recommender-builder n sim)]
                  (mahout/evaluate-ir builder model)))]
    (mahout/plot-ir xs stats)))

(defn recommend-for-user-1
  "Calculates an MLlib MatrixFactorisationModel function. Options are:
   rank - number of features to use for the factor matrices
   num-iter - number of iterations to train on
   lambda - regularisation parameter, helps to reduce overfitting
   Makes 3 predictions for user 1 using the .recommendProducts method."
  []
  (spark/with-context sc (-> (conf/spark-conf)
                             (conf/master "local")
                             (conf/app-name "movie-recommender"))
                      (let [items    (data/load-items "u.item")
                            id->name (fn [id] (get items id))
                            options  {:rank     10
                                      :num-iter 10
                                      :lambda   1.0}
                            model    (-> (sparkling/parse-ratings sc)
                                         (sparkling/training-ratings)
                                         (sparkling/alternating-least-squares options))]
                        (->> (.recommendProducts model 1 3)
                             (map (comp id->name #(.product %)))))))

(defn plot-als-rmse-for-ranks
  "Produce a plot of root mean square error as a function of rank of the factor matrix.
  Uses the training or test portion of the data as determined by the key train-test.
  Rank - number of features to use in the factor matrix."
  [training-test]
  (spark/with-context sc (-> (conf/spark-conf)
                             (conf/master "local")
                             (conf/app-name "ch7"))
                      (let [options  {:num-iter 10
                                      :lambda   0.1}
                            parsed   (spark/cache (sparkling/parse-ratings sc))
                            training (spark/cache (sparkling/training-ratings parsed))
                            test     (spark/cache (sparkling/test-ratings parsed))
                            ranks    (range 2 50 2)
                            errors   (for [rank ranks]
                                        (-> (sparkling/alternating-least-squares training (assoc options :rank rank))
                                            (sparkling/root-mean-square-error (case training-test
                                                                                :train training
                                                                                :test test
                                                                                :default test))))]
                        (-> (c/scatter-plot ranks errors
                                            :x-label "Rank"
                                            :y-label "RMSE")
                            (i/view)))))