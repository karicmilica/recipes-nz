(ns utils.util)

(defn String->Number [str]
  (let [n (read-string str)]
    (if (number? n) n nil)))

(defn ^String substring?
  "True if s contains the substring."
  [substring ^String s]
  (.contains s substring))

