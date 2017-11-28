# movie-recommender

Makes movie recommendations for users in the MovieLens 100k dataset:

https://grouplens.org/datasets/movielens/

Contains code for making recommendations using:
- A Slope One collaborative filtering recommender (see https://en.wikipedia.org/wiki/Slope_One)
- A user based recommender using Mahout's GenericUserBasedRecommender (see https://mahout.apache.org/)
- A boolean preferences recommender using Mahout's GenericBooleanPrefUserBasedRecommender
- Spark MLlib's alternating least squares algorithm