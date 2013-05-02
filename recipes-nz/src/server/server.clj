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
(defn start-server [] (server/start 8080)
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
      (views/search-recipes-template "Recipes"
                         recipes
                         ingredients
                         )
      (views/search-recipes-template "Recipes"
                         recipes
                         ingredients
                         (db/findRecipeByIdForRecommendation 
                            (reduce (fn [v x] (conj v (ObjectId. x))) []  
                                    (rc/recommend (:username user))))
                         
                     ))))

(defpage "/recipe/:id/" {:keys [id]} 
  (let [user (session/get :user)]
    (if (nil? user) 
  (views/recipe-template (db/findRecipeById id) (db/getIngredientCategories))
  (views/recipe-template (db/findRecipeById id (:_id user)) (db/getIngredientCategories))
  )
  )
  )


(defn successfulLogin [user] 
  (println @firstIngrId) 
  (session/put! :user user) (resp/redirect (str "/search/" @firstIngrId "/")))


(defpage [:post "/login"] {:keys [username password]}
  (let [user (db/login username password)]
    (if (nil? user) (resp/redirect "/login")
       (successfulLogin user))))

(defpage [:post "/register"] {:keys [username password name email]}
  (let [user (db/register username password name email)]
   (if (nil? user) 
   (views/registration-template "Register"  
                                (str "This username already exists '" username "', please choose another one.") )
   (successfulLogin user)
   )
  ))

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
      { :status (str "Your vote of " rate " was successful!")
       :average avg 
         :width (str "width: " (* rate 25) "px")}))

(remote/defremote rateRecipe [recipeid rate]
  (let [user (session/get :user)]
    (if (nil? user)
      { :status (str "Please log in!")
         :width "width: 0px"}
      (successfulRecipeRate recipeid rate user))
    ))
  

(defn -main [& args]
  (start-server)
  )