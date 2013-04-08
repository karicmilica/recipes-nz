(ns utils.extraction-test
  (:use clojure.test
        utils.util-extraction))



(deftest test-get-links
  (testing "FIXME, I fail."
  (is (= (get-links '({:tag :li, :attrs nil, 
             :content ("\n    " {:tag :a, :attrs {:href "http://www.foodinaminute.co.nz/Recipes/Apricot-Roast-Turkey"}, 
                :content ("\n    \n        \n    \n                                                                                                \n                \n                \n                "
                           {:tag :img, :attrs {:title "Apricot Roast Turkey",                 
                              :alt "Apricot Roast Turkey", :style "", :height "100", :width "100", 
                               :src "http://www.foodinaminute.co.nz/var/fiam/storage/images/recipes/apricot-roast-turkey/1282857-3-eng-US/Apricot-Roast-Turkey_searchthumbnailimagecropped.jpg"}, :content nil} "\n            \n    \n    \n    ")} "\n    " {:tag :div, :attrs {:class "search_copy"}, :content ("\n        " {:tag :h3, :attrs nil, :content ({:tag :a, :attrs {:href "http://www.foodinaminute.co.nz/Recipes/Apricot-Roast-Turkey"}, :content ("Apricot Roast Turkey")})} "\n\n                            " {:tag :p, :attrs nil, :content ("A mouthwatering Turkey Roast served with Wattie's Rosemary and Garlic Roasters\n.")} "\n                    ")} "\n    " {:tag :div, :attrs {:class "search_link"}, :content ("\n        " {:tag :a, :attrs {:class "see", :href "http://www.foodinaminute.co.nz/Recipes/Apricot-Roast-Turkey"}, :content ("» See Recipe")} "\n    ")} "\n")}))
       '("http://www.foodinaminute.co.nz/Recipes/Apricot-Roast-Turkey") ))))

(deftest test-pause-agent
  (testing "FIXME, I fail."
  (let [a (agent 4)]
    (pause a)
    (is (= (:utils.util-extraction/paused (meta a)) true)))))


(deftest test-get-ratings
  (testing "FIXME, I fail."
  (is (= (get-rating '({:tag :p, :attrs {:class "author comments"}, :content ("\n        " {:tag :b, :attrs nil, :content ("            robert's        ")} " \n                    Review | Rating " {:tag :span, :attrs {:alt "5 star rating", :class "rating_star_5 star-display"}, :content nil} "\n")} {:tag :p, :attrs {:class "author comments"}, :content ("\n        " {:tag :b, :attrs nil, :content ("            Maryanne's        ")} " \n                    Review | Rating " {:tag :span, :attrs {:alt "3 star rating", :class "rating_star_3 star-display"}, :content nil} "\n")}) 36) 
       {"Maryanne" {36 3}, "robert" {36 5}} ))))


(deftest test-num-filter
  (is (= (num-filter "5 star rating") "5")))

(deftest test-prepare-json
  (testing "FIXME, I fail."
  (is (= (prepareJson "{\n                        
                 \"@type\": \"http://data-vocabulary.org/RecipeIngredient\", \n                        
                  \"http://data-vocabulary.org/amount\": \"250 - 300g  \", \n                       
                   \"http://data-vocabulary.org/name\": \"Tegel Thin Cut Boneless Skinless Chicken Breast \"\n                    }") 
       {:type "RecipeIngredient", :amount "250 - 300g  ", :name "Tegel Thin Cut Boneless Skinless Chicken Breast "} ))))

(deftest test-definerecipe-ingredient-categories
  (testing "FIXME, I fail."
  (is (= (defineIngredientCategory "sesame seeds (optional) lite sour cream tegel thin cut boneless 
                                    skinless chicken breast sheets filo pastry finely chopped parsley 
                                    butter, melted spring onions, sliced or ½ onion, diced wattie's cream style corn "
                                    '({:_id 1, :name "chicken"} 
                                       {:_id 3, :name "potato"} 
                                         {:_id 5, :name "tomato"})) 
                                    '(1)
         ))))
           
(run-tests)