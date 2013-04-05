(ns utils.util-extraction
  (:require 
           [clojure.data.json :as json]
           [net.cgrand.enlive-html :as html]))

(defn get-links [articles] (map (fn [x]   
       
               (:href (:attrs (first (html/select x [:a]))))
                   )
                                  articles))

(defn pause
      ([a] (alter-meta! a assoc ::paused true)))
(defn paused? [agent] (::paused (meta agent)))