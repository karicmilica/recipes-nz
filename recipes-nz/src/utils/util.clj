(ns utils.util)

(defn String->Number [str]
  (let [n (read-string str)]
       (if (number? n) n nil)))

