(ns db.db
  (:require [monger.core :as mg]
            [monger.collection :as mc])
   (:use [monger.operators])
   (:import (org.bson.types ObjectId)))

(defn db-init []
(mg/connect!)
(mg/set-db! (mg/get-db "my-app"))
)

(defn getIngredientCategories [] (mc/find-maps "ingredient"))

(defn addLinkHref [recipe] (let [id (.toString (:_id recipe))]
                             (assoc recipe :linkHref (str "http://localhost:8080/recipe/" id "/"))))

(defn addLinkHrefToRecipes [inp] (map #(addLinkHref %) inp))

(defn findRecipesByCategory [category] 
  (let [results (mc/find-maps "recipes" {:ingredient-categories (ObjectId. category)})
        results-with-href (addLinkHrefToRecipes results)]
    results-with-href))

(defn findRecipeById [id] (mc/find-map-by-id "recipes" (ObjectId. id)))


(defn login [username password]
  (mc/find-one-as-map "users" {:username username :password password}))

(defn register [username password name email]
  (mc/insert-and-return "users" {:username username :password password :name name
                      :email email}))

(defn addRecipeRatingForUser [userId recipeId rate]
  (mc/update-by-id "users" userId
                   {$set {(str "recipesRatings." recipeId) rate}}))