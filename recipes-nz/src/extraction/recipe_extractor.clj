(ns extraction.recipe_extractor
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

(declare runPagLinkExtraction)
(declare dequeue!) 

(defn readUrls []
  (apply list (map #(clojure.string/trim %) (line-seq (reader "urls.txt"))))) 



; start 1
(defn handle-results-pag [{:keys [links-recipes]}]
  (try
    (doseq [url links-recipes]
      (.put url-queue1 url))
    {::t ::getUrlPag}
    (finally (runPagLinkExtraction *agent*))))

(defn process-pag [{:keys [content url]}]
  (try
    (let [html-content (html/html-resource (java.io.StringReader. content))
          pags (html/select html-content [:p.pagination :a])]
      {::t ::handleResultsPag
       :links-recipes (utile/get-pag-links pags url)})
    (finally (runPagLinkExtraction *agent*))))

(defn get-url-pag [state]
  (let [url (dequeue! urls *agent*)]
    (try
      {:url url
       :content (slurp url)
       ::t ::processPag}
      (catch Exception e
        state)
      (finally (runPagLinkExtraction *agent*)))))

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

(defn setFlagTrue [arg] (compare-and-set! arg false true))



(defn stop [state flag] 
  (setFlagTrue flag)	
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




(defn runPagLinkExtraction
  ([] (doseq [a agents-pag] (runPagLinkExtraction a)))
  ([a] ((run-agents agents-pag) a)))

(defn runRecipeLinkExtraction
  ([] (doseq [a agents-search] (runRecipeLinkExtraction a)))
  ([a] ((run-agents agents-search) a)))

; start 2
(defn handle-results-search [{:keys [links-recipes]}]
  (try
    (doseq [url links-recipes]
      (.put url-queue2 url))
    {::t ::getUrlSearch :queue url-queue1}
    (finally (runRecipeLinkExtraction *agent*))))

(defn process-search [{:keys [content]}]
  (try
    (let [html-content (html/html-resource (java.io.StringReader. content))
          articles (html/select html-content [:ul.search_results :li])]
      {::t ::handleResultsSearch
       :links-recipes (utile/get-links articles)})
    (finally (runRecipeLinkExtraction *agent*))))

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
        (finally (runRecipeLinkExtraction *agent*))))))

;end 2


(defn prepareLink [link] 
  (str "http://www.w3.org/2012/pyMicrodata/extract?format=json&uri=" link))



(defn runRecipesExtraction
  ([] (doseq [a agents] (runRecipesExtraction a)))
  ([a] ((run-agents agents) a)))

(defn store-results [url recipe usersRating]
  (swap! crawled-urls conj url)
  (db/addRecipe recipe)
  (swap! users (partial merge-with concat) usersRating))

; start 3
(defn handle-results [{:keys [recipe usersRating url]}]
  (try
    (if-not (@crawled-urls url) 
      (store-results url recipe usersRating))
    {::t ::getUrlRecipe
     :queue url-queue2}
    (finally (runRecipesExtraction *agent*))))


(defn process [{:keys [content pageContent url]}]
  (try
    (let [html-content (html/html-resource (java.io.StringReader. pageContent))
          id (ObjectId.)
          recipe (utile/processData content @ingredient-categories)]
      {:url url
       ::t ::handleResultsRecipe
       :usersRating (utile/get-rating (utile/get-rating-div html-content) (.toString id)) 
       :recipe (assoc recipe :_id id)})
    (finally (runRecipesExtraction *agent*))))


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
               :content (slurp (prepareLink url))
               :pageContent (slurp url)
               ::t ::processRecipe})))
        (catch Exception e
          state)
        (finally (runRecipesExtraction *agent*))))))
; end 3

(defn prepareUsersForDB [users] 
  (reduce (fn [l x] (conj l {:username (key x) :recipeRatings (into {} (val x))} )) '() users))

(defn prepareDB [ingrs]
  (db/formatDB)
  (db/insertIngredients ingrs)) 

(defn runScraper []
  (db/db-init)
  (prepareDB ingrs) 
  
  (compare-and-set! urls '() (readUrls))
  (compare-and-set! ingredient-categories '() (mc/find-maps "ingredient"))
  (Thread/sleep 2000)
  (println (count @ingredient-categories))
  (println (count @urls)) 
  (runPagLinkExtraction)
  (runRecipeLinkExtraction) 
  (Thread/sleep 20000)
  (.put url-queue1 "END") 
  (runRecipesExtraction) 
  (Thread/sleep 100000)
  (.put url-queue2 "END")
  (Thread/sleep 230000)
  ;(doseq [a agents] (println (agent-error a)))
  (doseq [a agents] (utile/pause a))
  (println  url-queue1)
  (println "1" (.size url-queue1))
  (println "2" (.size url-queue2))
  (println "users: " (count @users))
  (println "recipes: " (count @crawled-urls))
  (doseq [u (prepareUsersForDB (filter #(> (count (val %)) 2) @users))] 
    (db/addUser u))
  (println "finished"))

;(defn runCrawler []
  ;(def urls (atom (readUrls)))
  ;(println (count @urls)) 

  ;(runPagLinkExtraction)
 ; (runRecipeLinkExtraction) 
 ; (Thread/sleep 20000)
  ;(.put url-queue1 "END") 
  ;(Thread/sleep 20000)
 ; (println (count @urls))
  
  ;(println url-queue1)
;(println "url-queue1" (.size url-queue1))
  ;(println "url-queue2" (.size url-queue2))
  ;(doseq [a agents-search] (utile/pause a))
  ;(doseq [a agents-search]
    ;(println (utile/paused? a)))
 ; )

(defn -main [& args]
  (runScraper))