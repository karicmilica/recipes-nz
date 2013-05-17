(ns recipes-nz.cljs.main
(:require [fetch.remotes])
  (:use [jayq.core :only [$ delegate append data bind val text]])
  (:require-macros [fetch.macros :as fm]))

(def $cr ($ :#current-rating))
(defn display-result [result]
  (let [el (.getElementById js/document "current-rating")]
    (text ($ :#current-rating-result) (:status result))
    (text ($ :.arating) (:average result))
    (.setAttribute el "style" (:width result))))

(defn push-recipe-rating [rating]
  (let [$r ($ :#recipeId)] 
    (fm/remote (rate-recipe (val $r) rating) [result]
      (display-result result))))


(bind ($ :.one-star)
  :click
  (fn [e] 
    (push-recipe-rating 1)))

(bind ($ :.two-stars)
  :click
  (fn [e] 
    (push-recipe-rating 2)))

(bind ($ :.three-stars)
  :click
  (fn [e] 
    (push-recipe-rating 3)))

(bind ($ :.four-stars)
  :click
  (fn [e] 
    (push-recipe-rating 4)))

(bind ($ :.five-stars)
  :click
  (fn [e] 
    (push-recipe-rating 5)))

