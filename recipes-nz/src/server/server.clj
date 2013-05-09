(ns server.server
  (:require [db.db :as db]
            [noir.server :as server]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.fetch.remotes :as remote]
            [views.views :as views]
            [noir.core :refer [defpage defpartial]]
            [utils.util :as util]
            [recommendation.recommendation :as rc])
  (:import (org.bson.types ObjectId)))

(def firstIngrId (atom ""))

(defn start-server []
  (server/start 8080)
  (db/db-init)
  (Thread/sleep 2000)
  (compare-and-set! firstIngrId "" (.toString (:_id (first(db/getIngredientCategories)))))
  (println "id" @firstIngrId))

(defpage "/login" {}
  (views/login-template "Login"))

(defpage "/search/:category/" {:keys [category]} 
  (let [user (session/get :user)
        recipes (db/findRecipesByCategory category)
        ingredients (db/getIngredientCategories)]
    (if (or (nil? user) (< (db/countRatesForUser (:_id user)) 3))
      (views/search-recipes-template "Recipes" recipes ingredients)
      (views/search-recipes-template 
        "Recipes"
        recipes
        ingredients
        (db/findRecipeByIdForRecommendation 
          (reduce (fn [v x] (conj v (ObjectId. x))) [] (rc/recommend (:username user))))))))

(defpage "/recipe/:id/" {:keys [id]} 
  (if-let [user (session/get :user)] 
    (views/recipe-template (db/findRecipeById id (:_id user)) (db/getIngredientCategories))
    (views/recipe-template (db/findRecipeById id) (db/getIngredientCategories))))


(defn successfulLogin [user] 
  (session/put! :user user) (resp/redirect (str "/search/" @firstIngrId "/")))


(defpage [:post "/login"] {:keys [username password]}
  (if-let [user (db/login username password)]
    (successfulLogin user)
    (resp/redirect "/login")))

(defpage [:post "/register"] {:keys [username password name email]}
  (if-let [user (db/register username password name email)] 
    (successfulLogin user)
    (views/registration-template 
      "Register"  
      (str "This username already exists '" username "', please choose another one."))))

(defpage "/register" {}
  (views/registration-template "Register"))

;(defpage [:post "/rate"] {:keys [amount recipeid]}
  ;(let [userId (:_id (session/get :user))
    ;    amnt (util/String->Number amount)]
  ;(db/addRecipeRatingForUser userId recipeid amnt) 
  ;(resp/json {"width" (* amnt 25), "status" (str "Your vote of " amount " was successful!")})))

(defn successfulRecipeRate [recipeid rate user]
  (db/addRecipeRatingForUser (:_id user) recipeid rate)
  (let [avg (db/averageRecipeRating recipeid)]
      {:status (str "Your vote of " rate " was successful!")
       :average avg 
       :width (str "width: " (* rate 25) "px")}))

(remote/defremote rateRecipe [recipeid rate]
  (if-let [user (session/get :user)]
    (successfulRecipeRate recipeid rate user)
    {:status (str "Please log in!") :width "width: 0px"}))
  

(defn -main [& args]
  (start-server))