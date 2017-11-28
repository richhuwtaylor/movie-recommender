(ns movie-recommender.data
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(defn to-long
  [s]
  (Long/parseLong s))

(defn line->rating
  "Takes a line, splits it into fields on tab characters, converts each into
  a Long datatype, then puts the fields into a map with the supplied keys."
  [line]
  (->> (s/split line #"\t")
       (map to-long)
       (zipmap [:user :item :rating])))

(defn load-ratings
  "Takes a file and returns a vector of maps (of ratings)."
  [file]
  (with-open [rdr (io/reader (io/resource file))]
    (->> (line-seq rdr)
         (map line->rating)
         (into []))))

(defn line->item-tuple
  "Takes a line string containing id and name and returns them in a vector."
  [line]
  (let [[id name] (s/split line #"\|")]
    (vector (to-long id) name)))

(defn load-items
  "Reads in the file at the supplied path. 'with-open' binds the reader to rdr
   and ensures that the reader is closed at the end of the form."
  [path]
  (with-open [rdr (io/reader (io/resource path))]
    (->> (line-seq rdr)
         (map line->item-tuple)
         (into {}))))