(ns db.db
  (:require [monger.core :as mg]
            [monger.collection :as mc])
   (:use [monger.operators])
   (:import (org.bson.types ObjectId)))

(defn db-init []
(mg/connect!)
(mg/set-db! (mg/get-db "my-app"))
)