(ns views.views
  (:require [clojure.data.json :as json]
           [net.cgrand.enlive-html :as html]))

(html/deftemplate login-template "views/login.html"
 [title]
   [:title] (html/content title))

(html/defsnippet menu-snippet "views/menu.html"
  [[:#menu]] 
  [ingrCategories]
     [:div.menu_item] (html/clone-for [item ingrCategories] 
                   [:a.mlink]  (html/content (clojure.string/capitalize (:name item)))
                   [:a.mlink] (html/set-attr :href (str "http://localhost:8080/search/" (:_id item) "/"))))



(html/defsnippet search-snippet "views/searchView.html"
  [[:#results]]  
  [title recipes]
      [:h2.results] (html/content title)
      [:div.no-msg] (if (empty? recipes)
                   identity
                   (html/substitute nil))
      [:#double :li] (html/clone-for [{:keys [title summary src linkHref]} recipes]
                          [:h3 :a] (html/content title)
                          [:p] (html/content summary)
                          [:img] (html/set-attr :src src)
                          [:a] (html/set-attr :href linkHref)))

(html/defsnippet recommedation-snippet "views/recommendationlist.html"
  [[:#recommendation]] 
  [recommendations]
     [:div.recommendation_item] (html/clone-for [{:keys [title description src linkHref]} recommendations]
                          [:a.rlink] (html/content title)
                          [:img] (html/set-attr :src src)
                          [:a.rlink] (html/set-attr :href linkHref)))

(html/deftemplate search-recipes-template "views/layout.html"
  ([title recipes categories recommended]
    [:title] (html/content title)
    [:#left] (html/content (menu-snippet categories))
    [:#right] (html/content (recommedation-snippet recommended))
    [:#content] (html/content (search-snippet title recipes)))
  ([title recipes categories]
    [:title] (html/content title)
    [:#left] (html/content (menu-snippet categories))
    [:#content] (html/content (search-snippet title recipes)))
    )

(html/defsnippet recipe-snippet "views/recipeView.html"
   [[:#recipe]] 
  [recipe]
     [:h2.title] (html/content (:title recipe))
     [:div.no-msg] (if (empty? recipe)
                   identity
                   (html/substitute nil))
                          [:#recipeId] (html/set-attr :value (:_id recipe)) 
                          [:p.description] (html/content (:description recipe))
                          [:span.arating] (html/append (str (:avRating recipe)))
                          [:p.recipeYield] (html/append (:recipeYield recipe))
                          [:p.instructions] (html/content (:instructions recipe))
                          [:img.recipeimg] (html/set-attr :src (:src recipe))
                          [:#current-rating] (if (not (nil? (:userRecipeRating recipe))) (html/set-attr :style (str "width: " (* (:userRecipeRating recipe) 25) "px;"))) 
                          [:ul.ingredients :li] (html/clone-for [item (:ingredients recipe)] 
                                                                (html/content item)))










(html/deftemplate recipe-template "views/layout.html"
  [recipe categories]
    [:title] (html/content (:title recipe))
    [:#left] (html/content (menu-snippet categories))
    [:#content] (html/content (recipe-snippet recipe))
    )

(html/deftemplate registration-template "views/register.html"
 [title]
   [:title] (html/content title))

