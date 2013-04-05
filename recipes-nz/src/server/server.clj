(ns server.server
  (:require [db.db :as db]
            [noir.server :as server]
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

(defn -main [& args]
  (start-server))