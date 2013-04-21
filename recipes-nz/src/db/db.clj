(ns db.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [utils.util :as util])
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
  (let [results (mc/find-maps "recipes" {:ingredient-categories (ObjectId. category)} ["title" "src" "summary"])
        results-with-href (addLinkHrefToRecipes results)]
    results-with-href))

(defn userRecipeRating [userId recipeId]
  (let [k (keyword recipeId)
        user (mc/find-one-as-map "users" {(str "recipeRatings." recipeId) { $gt 0}
                          :_id userId} 
                 [(str "recipeRatings." recipeId)])
        
        rate (k (:recipeRatings user))
        ]
     (if (nil? rate)
       0
       rate)
    )
  )

(defn averageRecipeRating [recipeId]
  (let [k (keyword recipeId)
        users (mc/find-maps "users" {(str "recipeRatings." recipeId) { $gt 0}} 
                 ["username" (str "recipeRatings." recipeId)])
        c (count users)
        sum (reduce #(+ %1 (k (:recipeRatings %2))) 0 users)
        ]
     (if (= c 0)
       0
       (double(/ sum c)))
    )
  )

(defn findRecipeById ([id]  (let [recipe (mc/find-map-by-id "recipes" (ObjectId. id))
                                 ar (averageRecipeRating id)]
                                (assoc recipe :avRating ar)
                            ))
                      ([id userId] (let [ar (findRecipeById id)
                                         userRate (userRecipeRating userId id)]
                                     (assoc ar :userRecipeRating  userRate))))


(defn login [username password]
  (mc/find-one-as-map "users" {:username username :password password}))

(defn register [username password name email]
  (mc/insert-and-return "users" {:username username :password password :name name
                      :email email}))

(defn addRecipeRatingForUser [userId recipeId rate]
  (mc/update-by-id "users" userId
                   {$set {(str "recipeRatings." recipeId) rate}}))

(defn addUser [user] 
  (mc/insert "users" user))

(defn addRecipe [recipe]
  (mc/insert "recipes" recipe))

(defn findUsers []
  (mc/find-maps "users"))

(defn findRecipeByIdForRecommendation [ids] 
  (let [results (mc/find-maps "recipes"
                {:_id {$in ids}} ["title" "src" "summary"])
        results-with-href (addLinkHrefToRecipes results)
        ]
    results-with-href))

(defn countRatesForUser [id]
  (let [ratings (:recipeRatings 
          (mc/find-map-by-id "users" id ["recipeRatings"]))]
       (if (nil? ratings)
         0
         (count ratings)))  
  )





