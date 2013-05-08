(ns recommendation.recommendation
  (:require [recommendation.similarity :as sim]
            [db.db :as db]))


(defn similarities [prefs user sim-algo]
  (let [m (dissoc prefs user)
        prefUser (prefs user)]
    (filter #(> (second %) 0)
            (apply assoc {} (interleave (keys m)  (map #(sim-algo prefUser  %) (vals m)))))))

(defn prepareUsersForAlg [users] 
  (reduce (fn [m x] (assoc m (:username x) (:recipeRatings x) )) {} users))


(defn weight-prefs [prefs sim-users user]
  (reduce 
    (fn [hm v]
      (let [other (first v)
            sim (second v)
            diff (filter #(nil? ((prefs user) (key %))) (prefs other))
            wpref (if (> (count diff) 0) (apply assoc {}
                                                (interleave (keys diff) 
                                                            (map #(* % sim) (vals diff)))) {})]
        (assoc hm other wpref))) {} sim-users))

(defn sum-scrs [wprefs]
  (reduce (fn [hm r] (merge-with #(+ %1 %2) hm r)) {} (vals wprefs)))

(defn sum-sims [wpref scores sim-users]
  (reduce (fn [hm x]
            (let [r (first x)
                  rated-users (reduce 
                                (fn [h u] 
                                  (if (nil? ((val u) r)) 
                                    h
                                    (conj h (key u))))
                                [] wpref)
                  similarities (reduce + (map #(sim-users %) rated-users))]
              (assoc hm r similarities) ) ) {} scores))

(defn recommend
  ([user]
    (let [cr (prepareUsersForAlg (db/findUsers))]
      (map #(name (first %)) (take 5 (sort-by second > (partition 2 (recommend cr user sim/euclidean)))))))
  ([prefs user algo]
    (let [similar-users (into {} (similarities prefs user algo))
          weighted-prefs (weight-prefs prefs similar-users  user)
          scores (sum-scrs weighted-prefs)
          sims (sum-sims weighted-prefs scores similar-users)]
      (interleave (keys scores) (map #(/ (second %) (sims (first %))) scores)))))



