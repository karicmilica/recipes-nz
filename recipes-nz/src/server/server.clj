(ns server.server
  (:require [db.db :as db]
            [noir.server :as server]
            [noir.session :as session]
            [noir.response :as resp]
            [views.views :as views]
            [noir.core :refer [defpage defpartial]]))

(defn start-server [] (server/start 8080)
  (db/db-init))

(defpage "/login" {}
         (views/login-template "Login"))

(defpage "/search/:category/" {:keys [category]} 
  (views/search-recipes-template "Recipes"
                         (db/findRecipesByCategory category)
                         (db/getIngredientCategories)
                     ))

(defpage "/recipe/:id/" {:keys [id]} 
  (views/recipe-template (db/findRecipeById id) (db/getIngredientCategories)
                     ))

(defn successfulLogin [user] 
  (session/put! :user user) (resp/redirect "/search/5156d1be34876cd0db98aea3/"))


(defpage [:post "/login"] {:keys [username password]}
  (let [user (db/login username password)]
    (if (nil? user) (resp/redirect "/login")
       (successfulLogin user))))

(defpage [:post "/register"] {:keys [username password name email]}
  (let [user (db/register username password name email)]
    (if (nil? user) (resp/redirect "/register")
  (resp/redirect "/search/5156d1be34876cd0db98aea3/"))))

(defpage "/register" {}
  (views/registration-template "Register"))

(defn -main [& args]
  (start-server))