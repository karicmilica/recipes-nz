(ns recipes-nz.server.server
  (:require [recipes-nz.db.db :as db]
            [noir.server :as server]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.fetch.remotes :as remote]
            [recipes-nz.views.views :as views]
            [noir.core :refer [defpage defpartial]]
            [recipes-nz.utils.util :as util]
            [recipes-nz.recommendation.recommendation :as rc])
  (:import (org.bson.types ObjectId)))

(def first-ingridient-id (atom ""))

(defn start-server []
  (server/start 8080)
  (db/db-init)
  (Thread/sleep 2000)
  (compare-and-set! first-ingridient-id "" (.toString (:_id (first(db/get-ingredient-categories)))))
  (println "id" @first-ingridient-id))

(defpage "/login" {}
  (views/login-template "Login"))

(defpage "/search/:category/" {:keys [category]} 
  (let [user (session/get :user)
        recipes (db/find-recipes-by-category category)
        ingredients (db/get-ingredient-categories)]
    (if (or (nil? user) (< (db/count-ratings-for-user (:_id user)) 3))
      (views/search-recipes-template "Recipes" recipes ingredients)
      (views/search-recipes-template 
        "Recipes"
        recipes
        ingredients
        (db/find-recipes-by-ids 
          (reduce (fn [v x] (conj v (ObjectId. x))) [] (rc/recommend (:username user))))))))

(defpage "/recipe/:id/" {:keys [id]} 
  (if-let [user (session/get :user)] 
    (views/recipe-template (db/find-recipe-by-id id (:_id user)) (db/get-ingredient-categories))
    (views/recipe-template (db/find-recipe-by-id id) (db/get-ingredient-categories))))


(defn successfulLogin [user] 
  (session/put! :user user) (resp/redirect (str "/search/" @first-ingridient-id "/")))


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

;(defpage [:post "/rating"] {:keys [amount recipe-id]}
  ;(let [userId (:_id (session/get :user))
    ;    amnt (util/String->Number amount)]
  ;(db/add-recipe-rating-for-user userId recipe-id amnt) 
  ;(resp/json {"width" (* amnt 25), "status" (str "Your vote of " amount " was successful!")})))

(defn successful-recipe-rated [recipe-id rating user]
  (db/add-recipe-rating-for-user (:_id user) recipe-id rating)
  (let [avg (db/average-recipe-rating recipe-id)]
      {:status (str "Your vote of " rating " was successful!")
       :average avg 
       :width (str "width: " (* rating 25) "px")}))

(remote/defremote rate-recipe [recipe-id rating]
  (if-let [user (session/get :user)]
    (successful-recipe-rated recipe-id rating user)
    {:status (str "Please log in!") :width "width: 0px"}))
  

(defn -main [& args]
  (start-server))