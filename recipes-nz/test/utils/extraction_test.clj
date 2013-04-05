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


(run-tests)