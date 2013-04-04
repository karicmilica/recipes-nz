(ns server.server
  (:require [db.db :as db]
            [noir.server :as server]))

(defn start-server [] (server/start 8080)
  (db/db-init))

(defn -main [& args]
  (start-server))