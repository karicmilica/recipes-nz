(ns recipes_nz.core
  (:require [db.db :as db]))

(defn foo
  "I don't do a whole lot."
  [x]
  (db/db-init)
  (println x "Hello, World!"))
(defn -main [& args]
  (foo 111))