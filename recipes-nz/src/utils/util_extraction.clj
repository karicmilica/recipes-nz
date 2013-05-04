(ns utils.util-extraction
  (:require 
           [clojure.data.json :as json]
           [net.cgrand.enlive-html :as html]
           [utils.util :as util]))

(defn get-links [articles] (map 
                             (fn [x]   (:href (:attrs (first (html/select x [:a])))))
                                  articles))

(defn pause
      [a] (alter-meta! a assoc ::paused true))

(defn paused? [agent] (::paused (meta agent)))

(defn num-filter [exp] (apply str (filter #(#{\0,\1,\2,\3,\4,\5} %) exp)))


(defn get-rating-div [pageContent]
           (html/select pageContent [:div.review_wrapper :p.author]))


(defn get-rating [divs id]  (reduce (fn [m x]   
             (let [k (clojure.string/replace (clojure.string/trim (first (:content (first(html/select x [:b]))                        
                         ))) "'s" "")
                   v (num-filter (:alt (:attrs (first(html/select x [:span])))))
                   ] 
               (if (= "" v)
                 m
                    (assoc m                  
                       k
                   { 
                   id   (util/String->Number v)}
                     ))))
                                        {} divs) )

(defn defineIngredientCategory [s l] (map #(:_id %) (filter #(util/substring? (:name %) s) l)))



(defn getData [jsonText] (first (:list (:md:item jsonText))))

(defn prepareJson [body] (json/read-str 
               (clojure.string/replace (clojure.string/replace body "@" "") "http://data-vocabulary.org/" "") :key-fn keyword))

(defn extractRecipe [data ingredientcategories] (assoc {} :title  (:name data)
                                                 :src (:photo data)
                                                 :summary (:summary data)
                                                 :recipeYield (:yield data)
                                                 :instructions (:instructions data)
                                                 :ingredients (map #(str (:amount %)  (:name %)) (:ingredient data))
             :ingredient-categories  (defineIngredientCategory (clojure.string/lower-case (apply str (map #(:name %) (:ingredient data)))) ingredientcategories)))

(defn processData [data ingredientcategories] (extractRecipe (getData (prepareJson data)) ingredientcategories))

(defn get-pag-links [pags url]
  (let [l  (filter #(nil? (:class (:attrs %))) pags)
        l1 (map #(:href (:attrs %)) l)
        l2 (map #(util/String->Number (first (:content %))) l)
        last-num (last l2)
         ]
    (if (< (count l1) 3)
            (conj l1 url)
           (let [b-last-num (nth l2 (- (count l) 2))
                ]
             (if (= 1 (- last-num b-last-num))
               (conj l1 url)
                (let [range-nums(range (* b-last-num 20) (* (- last-num 1) 20) 20)
                      other-urls (map #(str "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/" %) range-nums)]
                    (conj (concat l1 other-urls) url)
                  )
               )
             )) 

  )
  )