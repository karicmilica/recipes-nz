(ns extraction.recipe-extractor
  (:require 
           [net.cgrand.enlive-html :as html]
           [monger.collection :as mc]
           [utils.util-extraction :as utile]
           [db.db :as db])
  (:use [clojure.java.io :only [reader]])
  (:import (org.bson.types ObjectId))
  (:import (java.util.concurrent LinkedBlockingQueue BlockingQueue)))

; 1 - get pagination links
; 2 - get recipe links
; 3 - get recipes and their rates


(def users (atom {}))
(def url-queue1 (LinkedBlockingQueue.))
(def url-queue2 (LinkedBlockingQueue.))
(def ingrs '({:name "chicken"} {:name "potato"} {:name "beef"} {:name "egg"} 
             {:name "bean"} {:name "pork"} {:name "pasta"}  {:name "bacon"} 
             {:name "fish"} {:name "chocolate"} )) 
(def crawled-urls (atom #{}))
(def flag1 (atom false))
(def flag2 (atom false))
(def agents-pag (set (repeatedly 3 #(agent {::t ::getUrlPag}))))
(def agents-search (set (repeatedly 5 #(agent {::t ::getUrlSearch :queue url-queue1}))))
(def agents (set (repeatedly 10 #(agent {::t ::getUrlRecipe :queue url-queue2}))))
(def urls (atom '()))
(def ingredient-categories (atom '()))

(declare run-pag-link-extraction)
(declare dequeue!) 

(defn read-urls []
  (apply list (map #(clojure.string/trim %) (line-seq (reader "urls.txt"))))) 



; start 1
(defn handle-results-pag [{:keys [links-recipes]}]
  (try
    (doseq [url links-recipes]
      (.put url-queue1 url))
    {::t ::getUrlPag}
    (finally (run-pag-link-extraction *agent*))))

(defn process-pag [{:keys [content url]}]
  (try
    (let [html-content (html/html-resource (java.io.StringReader. content))
          pags (html/select html-content [:p.pagination :a])]
      {::t ::handleResultsPag
       :links-recipes (utile/get-pag-links pags url)})
    (finally (run-pag-link-extraction *agent*))))

(defn get-url-pag [state]
  (let [url (dequeue! urls *agent*)]
    (try
      {:url url
       :content (slurp url)
       ::t ::processPag}
      (catch Exception e
        state)
      (finally (run-pag-link-extraction *agent*)))))

;end

(declare get-url-search)
(declare process-search)
(declare handle-results-search)
(declare get-url)
(declare process)
(declare handle-results)

(defmulti dispatch ::t)

(defmethod dispatch ::getUrlPag [transition]
  (fn [ag] (send-off ag get-url-pag))) 

(defmethod dispatch ::handleResultsPag [transition]
  (fn [ag] (send-off ag handle-results-pag)))

(defmethod dispatch ::processPag [transition]
  (fn [ag] (send ag process-pag)))

(defmethod dispatch ::getUrlSearch [transition]
  (fn [ag] (send-off ag get-url-search))) 

(defmethod dispatch ::handleResultsSearch [transition]
  (fn [ag] (send-off ag handle-results-search)))

(defmethod dispatch ::processSearch [transition]
  (fn [ag] (send ag process-search)))

(defmethod dispatch ::getUrlRecipe [transition]
  (fn [ag] (send-off ag get-url))) 

(defmethod dispatch ::handleResultsRecipe [transition]
  (fn [ag] (send-off ag handle-results)))

(defmethod dispatch ::processRecipe [transition]
  (fn [ag] (send ag process)))

(defn run-agents [ags]
  (fn [a] (when (ags a)
            (send a (fn [state]
                      (when-not (utile/paused? *agent*)
                        ((dispatch state) *agent*) )
                      state)))))


(declare get-url-search)

(defn set-flag-true! [arg] (compare-and-set! arg false true))



(defn stop [state flag] 
  (set-flag-true! flag)	
  state)

(defn dequeue! [queue ag]
  (if (empty? @queue)
    (utile/pause ag)
    (loop []
      (let [q     @queue
            value (peek q)
            nq    (pop q)]
        (if (compare-and-set! queue q nq)
          value
          (recur))))))




(defn run-pag-link-extraction
  ([] (doseq [a agents-pag] (run-pag-link-extraction a)))
  ([a] ((run-agents agents-pag) a)))

(defn run-recipe-link-extraction
  ([] (doseq [a agents-search] (run-recipe-link-extraction a)))
  ([a] ((run-agents agents-search) a)))

; start 2
(defn handle-results-search [{:keys [links-recipes]}]
  (try
    (doseq [url links-recipes]
      (.put url-queue2 url))
    {::t ::getUrlSearch :queue url-queue1}
    (finally (run-recipe-link-extraction *agent*))))

(defn process-search [{:keys [content]}]
  (try
    (let [html-content (html/html-resource (java.io.StringReader. content))
          articles (html/select html-content [:ul.search_results :li])]
      {::t ::handleResultsSearch
       :links-recipes (utile/get-links articles)})
    (finally (run-recipe-link-extraction *agent*))))

(defn get-url-search [{:keys [queue] :as state}]
  (if (= @flag1 true) 
    (utile/pause *agent*)
    (let [url (.take queue)]
      (try
        (if (= "END" url)
          (stop state flag1)
          {:url url
           :content (slurp url)
           ::t ::processSearch})
        (catch Exception e
          state)
        (finally (run-recipe-link-extraction *agent*))))))

;end 2


(defn prepare-link [link] 
  (str "http://www.w3.org/2012/pyMicrodata/extract?format=json&uri=" link))



(defn run-recipes-extraction
  ([] (doseq [a agents] (run-recipes-extraction a)))
  ([a] ((run-agents agents) a)))

(defn store-results [url recipe usersRating]
  (swap! crawled-urls conj url)
  (db/add-recipe recipe)
  (swap! users (partial merge-with concat) usersRating))

; start 3
(defn handle-results [{:keys [recipe usersRating url]}]
  (try
    (if-not (@crawled-urls url) 
      (store-results url recipe usersRating))
    {::t ::getUrlRecipe
     :queue url-queue2}
    (finally (run-recipes-extraction *agent*))))


(defn process [{:keys [content pageContent url]}]
  (try
    (let [html-content (html/html-resource (java.io.StringReader. pageContent))
          id (ObjectId.)
          recipe (utile/process-data content @ingredient-categories)]
      {:url url
       ::t ::handleResultsRecipe
       :usersRating (utile/get-rating (utile/get-rating-div html-content) (.toString id)) 
       :recipe (assoc recipe :_id id)})
    (finally (run-recipes-extraction *agent*))))


(defn  get-url [{:keys [^BlockingQueue queue] :as state}]
  (if (= @flag2 true) 
    (utile/pause *agent*) 
    (let [urlstr (.take queue)]
      (try
        (if (= "END" urlstr)
          (stop state flag2 )
          (let [url (clojure.java.io/as-url urlstr)] 
            (if (@crawled-urls url)
              {::t ::getUrlRecipe
               :queue queue}
              {:url url
               :content (slurp (prepare-link url))
               :pageContent (slurp url)
               ::t ::processRecipe})))
        (catch Exception e
          state)
        (finally (run-recipes-extraction *agent*))))))
; end 3

(defn prepare-users-for-db [users] 
  (reduce (fn [l x] (conj l {:username (key x) :recipeRatings (into {} (val x))} )) '() users))

(defn prepare-db [ingrs]
  (db/format-db)
  (db/insert-ingredients ingrs)) 

(defn run-scraper []
  (db/db-init)
  (prepare-db ingrs) 
  
  (compare-and-set! urls '() (read-urls))
  (compare-and-set! ingredient-categories '() (mc/find-maps "ingredient"))
  (Thread/sleep 2000)
  (println (count @ingredient-categories))
  (println (count @urls)) 
  (run-pag-link-extraction)
  (run-recipe-link-extraction) 
  (Thread/sleep 20000)
  (.put url-queue1 "END") 
  (run-recipes-extraction) 
  (Thread/sleep 100000)
  (.put url-queue2 "END")
  (Thread/sleep 330000)
  ;(doseq [a agents] (println (agent-error a)))
  (doseq [a agents] (utile/pause a))
  (println  url-queue1)
  (println "1" (.size url-queue1))
  (println "2" (.size url-queue2))
  (println "users: " (count @users))
  (println "recipes: " (count @crawled-urls))
  (doseq [u (prepare-users-for-db (filter #(> (count (val %)) 2) @users))] 
    (db/add-user u))
  (println "finished"))



(defn -main [& args]
  (run-scraper))