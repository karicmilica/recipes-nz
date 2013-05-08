(ns recommendation.similarity)

(defn euclidean [user1-prefs user2-prefs]
  (let [shared-items (filter user1-prefs (keys user2-prefs))
        result (reduce (fn[s r]
                         (+ s (Math/pow (- (user1-prefs r) (user2-prefs r)) 2)))
                       0 shared-items)]
    (if (= (count shared-items) 0)
      0
      (/ 1 (+ 1 result)))))


(defn pearson [user1-prefs user2-prefs]
  (let [shared-items (filter user1-prefs (keys user2-prefs))] 
    (if (= 0 (count shared-items))
      0
      (let [sum1  (reduce (fn[s r] (+ s (user1-prefs r))) 0 shared-items)
            sum2  (reduce (fn[s r] (+ s (user2-prefs r))) 0 shared-items)
            sum1-sq  (reduce (fn[s r] (+ s (Math/pow (user1-prefs r) 2))) 0 shared-items)
            sum2-sq  (reduce (fn[s r] (+ s (Math/pow (user2-prefs r) 2))) 0 shared-items)
            psum (reduce (fn[s r] (+ s (* (user1-prefs r) (user2-prefs r)))) 0 shared-items)
            num (- psum (/ (* sum1 sum2) (count shared-items)))
            den (Math/sqrt (* 
                            (- sum1-sq (/ (Math/pow sum1 2) (count shared-items)))
                            (- sum2-sq (/ (Math/pow sum2 2) (count shared-items)))))]
        (if (= den 0)
          0
          (double (/ num den))) ))))


