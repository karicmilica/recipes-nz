(ns recipes-nz.db.db
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:use [monger.operators])
  (:import (org.bson.types ObjectId)))

(defn db-init []
  (mg/connect!)
  (mg/set-db! (mg/get-db "my-app")))

(defn format-db []
  (mc/remove "ingredient")
  (mc/remove "users")
  (mc/remove "recipes"))

(defn insert-ingredients [ingrList]
  (doseq [ingr ingrList] 
    (mc/insert "ingredient" ingr)))

(defn get-ingredient-categories [] (mc/find-maps "ingredient"))

(defn add-link-href [recipe] 
  (let [id (.toString (:_id recipe))]
    (assoc recipe :linkHref (str "http://localhost:8080/recipe/" id "/"))))

(defn add-link-href-to-recipes [inp] (map #(add-link-href %) inp))

(defn find-recipes-by-category [category] 
  (let [results (mc/find-maps "recipes" {:ingredient-categories (ObjectId. category)} ["title" "src" "summary"])
        results-with-href (add-link-href-to-recipes results)]
    results-with-href))

(defn get-recipe-rating [user-id recipe-id]
  (let [k (keyword recipe-id)
        user (mc/find-one-as-map "users" {(str "recipeRatings." recipe-id) { $gt 0} :_id user-id} [(str "recipeRatings." recipe-id)])
        rating (k (:recipeRatings user))]
    (if (nil? rating)
      0
      rating)))

(defn average-recipe-rating [recipe-id]
  (let [k (keyword recipe-id)
        users (mc/find-maps "users" {(str "recipeRatings." recipe-id) { $gt 0}} ["username" (str "recipeRatings." recipe-id)])
        c (count users)
        sum (reduce #(+ %1 (k (:recipeRatings %2))) 0 users)]
    (if (= c 0)
      0
      (double(/ sum c)))))

(defn find-recipe-by-id 
  ([id]  (let [recipe (mc/find-map-by-id "recipes" (ObjectId. id))
               ar (average-recipe-rating id)]
           (assoc recipe :avRating ar)))
  ([id user-id] (let [ar (find-recipe-by-id id)
                     r-rating (get-recipe-rating user-id id)]
                 (assoc ar :userRecipeRating  r-rating))))


(defn login [username password]
  (mc/find-one-as-map "users" {:username username :password password}))

(defn create-user [username password name email]
  (mc/insert-and-return "users" {:username username :password password :name name :email email}))

(defn add-recipe-rating-for-user [user-id recipe-id rating]
  (mc/update-by-id "users" user-id {$set {(str "recipeRatings." recipe-id) rating}}))

(defn add-user [user] 
  (mc/insert "users" user))

(defn add-recipe [recipe]
  (mc/insert "recipes" recipe))

(defn find-users []
  (mc/find-maps "users"))

(defn find-recipes-by-ids [ids] 
  (let [results (mc/find-maps "recipes" {:_id {$in ids}} ["title" "src" "summary"])
        results-with-href (add-link-href-to-recipes results)]
    results-with-href))

(defn count-ratings-for-user [id]
  (let [ratings (:recipeRatings (mc/find-map-by-id "users" id ["recipeRatings"]))]
    (if (nil? ratings)
      0
      (count ratings))))

(defn exist-user? [username]
  (let [l (mc/find-maps "users" {:username username})]
    (> (count l) 0)))

(defn register [username password name email]
  (if-not (exist-user? username)
    (create-user username password name email)))





