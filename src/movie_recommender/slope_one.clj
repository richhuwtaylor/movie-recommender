(ns movie-recommender.slope-one
  (:require [incanter.stats :as stats]
            [medley.core :refer [map-vals]]))

(defn conj-item-difference
  "Updates the difference in the ratings for items i and j in
  the dictionary."
  [dict [i j]]
  (let [difference (- (:rating j) (:rating i))]
    (update-in dict [(:item i) (:item j)] conj difference)))

(defn collect-item-differences
  "Takes each users list of ratings and calculates the difference
  between the ratings for each pair of rated items."
  [dict items]
  (reduce conj-item-difference dict
          (for [i items
                j items
                :when (not= i j)]
            [i j])))

(defn item-differences
  "Reduces over all users to build up a sequence of pairwise differences
  between items for users who have rated both items."
  [user-ratings]
  (reduce collect-item-differences {} user-ratings))

(defn summarize-item-differences
  "Summarize the difference between items into a mean and count
  of ratings on which the mean is based."
  [related-items]
  (let [f (fn [differences]
            {:mean (stats/mean differences)
             :count (count differences)})]
    (map-vals f related-items)))

(defn slope-one-recommender
  "Calculates the differences in ratings for pairs of items and
  summarises these differences."
  [ratings]
  (->> (item-differences ratings)
       (map-vals summarize-item-differences)))

(defn candidates
  "Returns the candidates used to calculate the weighted rating."
  [recommender {:keys [rating item]}]
  (->> (get recommender item)
       (map (fn [[id {:keys [mean count]}]]
              {:item id
               :rating (+ rating mean)
               :count count}))))

(defn weighted-rating
  "Returns the weighted average rating for each candidate."
  [[id candidates]]
  (let [ratings-count   (reduce + (map :count candidates))
        sum-rating      (map #(* (:rating %) (:count %)) candidates)
        weighted-rating (/ (reduce + sum-rating) ratings-count)]
    {:item id
     :rating weighted-rating
     :count  ratings-count}))

(defn slope-one-recommend
  "Removes from the candidate pool any items that have already been
  rated and orders the remainder in descending order.
  Takes the highest top-n recommendations."
  [recommender rated top-n]
  (let [already-rated   (set (map :item rated))
        already-rated?  (fn [{:keys [id]}]
                          (contains? already-rated id))
        recommendations (->> (mapcat #(candidates recommender %)
                                     rated)
                             (group-by :item)
                             (map weighted-rating)
                             (remove already-rated?)
                             (sort-by :rating >))]
    (take top-n recommendations)))
