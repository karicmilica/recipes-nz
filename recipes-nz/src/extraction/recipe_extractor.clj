(ns extraction.recipe_extractor
  (:require 
           [net.cgrand.enlive-html :as html]
           [monger.collection :as mc]
           [utils.util-extraction :as utile]
           [db.db :as db])
  (:use [clojure.java.io :only [reader]])
  (:import (org.bson.types ObjectId))
  (:import (java.util.concurrent LinkedBlockingQueue BlockingQueue)))

(def users (atom {}))
(def url-queue (LinkedBlockingQueue.))




(defn readUrls []
  (apply list (map #(clojure.string/trim %) (line-seq (reader "urls.txt"))))
  ) 

(declare get-url-search)
(declare process-search)
(declare handle-results-search)
(declare get-url)
(declare process)
(declare handle-results)

(defmulti dispatch ::t)
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
            (send a (fn [{transition ::t :as state}]
            (when-not (utile/paused? *agent*)
            ((dispatch state) *agent*) )
            state)))))


(declare get-url-search)
(declare urls2)

(defn dequeue!
        [queue ag]
        (if (empty? @queue)
          (utile/pause ag)
         (loop []
          (let [q     @queue
                value (peek q)
                nq    (pop q)]
            (if (compare-and-set! queue q nq)
              value
              (recur))))))


(def agents-search (set (repeatedly 5 #(agent {::t ::getUrlSearch}))))

(defn runLinkExtraction
         ([] (doseq [a agents-search] (runLinkExtraction a)))
         ([a] ((run-agents agents-search) a)))


(defn handle-results-search
         [{:keys [links-recipes]}]
         (try
         (doseq [url links-recipes]
         (.put url-queue url))
         {::t ::getUrlSearch}
         (finally (runLinkExtraction *agent*))))

(defn process-search
            [{:keys [content]}]
            (try
   (let [html-content (html/html-resource (java.io.StringReader. content))
            articles (html/select html-content [:ul.search_results :li])]
            {::t ::handleResultsSearch
            :links-recipes (utile/get-links articles)
            }
   )
            (finally (runLinkExtraction *agent*))))

(defn get-url-search [{:keys [] :as state}]
         (let [url (dequeue! urls2 *agent*)]
         (try
         {:url url
         :content (slurp url)
         ::t ::processSearch}
         (catch Exception e
           state 
         )
         (finally (runLinkExtraction *agent*)))))



(def flag (atom false))

(def crawled-urls (atom #{}))



(defn setFlagTrue [arg] (compare-and-set! arg false true))

(defn prepareLink [link] (str "http://www.w3.org/2012/pyMicrodata/extract?format=json&uri="
                                    link))




(def agents (set (repeatedly 10 #(agent {::t ::getUrlRecipe :queue url-queue}))))

(defn stop [] 
              (setFlagTrue flag)	
                          {::t ::getUrlRecipe :queue url-queue})

(declare ingredient-category)








(defn runRecipesExtraction
      ([] (doseq [a agents] (runRecipesExtraction a)))
      ([a] ((run-agents agents) a)))





(defn store-results [url recipe usersRating]
  (swap! crawled-urls conj url)
           (db/addRecipe recipe)
         (swap! users (partial merge-with concat) usersRating))

(defn handle-results
         [{:keys [recipe usersRating url]}]
         (try
           (if-not (@crawled-urls url) 
           (store-results url recipe usersRating))
         {::t ::getUrlRecipe
          :queue url-queue}
         (finally (runRecipesExtraction *agent*))))


(defn process
         [{:keys [content pageContent url]}]
         (try
           (let [html-content (html/html-resource (java.io.StringReader. pageContent))
                 id (ObjectId.)
                 recipe (utile/processData content @ingredient-category)]
         {:url url
          ::t ::handleResultsRecipe
          :usersRating (utile/get-rating (utile/get-rating-div html-content) (.toString id)) 
         :recipe (assoc recipe :_id id) 
         }
         )(finally (runRecipesExtraction *agent*))))


(defn  get-url
               [{:keys [^BlockingQueue queue] :as state}]
               (if (= @flag true) 
                 (utile/pause *agent*) 
               (let [urlstr (.take queue)]
               (try
                 (if (= "END" urlstr)
                   (stop)
                   (let [url (clojure.java.io/as-url urlstr)] 
                     (if (@crawled-urls url)
                   {::t ::getUrlRecipe
                    :queue queue}
               {:url url
               :content (slurp (prepareLink url))
               :pageContent (slurp url)
               ::t ::processRecipe})))
               (catch Exception e
                  state
               )
               (finally (runRecipesExtraction *agent*))))))

(defn prepareUsersForDB [users] 
     (reduce (fn [l x] (conj l {:username (key x) :recipeRatings (into {} (val x))} )) '() users))


(defn runScraper []
  (def urls2 (atom (readUrls)))
  (db/db-init)
  (def ingredient-category (atom (mc/find-maps "ingredient")))
  (Thread/sleep 3000)
  (println (count @urls2))
  (runLinkExtraction) 
   (runRecipesExtraction)
  (Thread/sleep 100000)
  (.put url-queue "END")
   (Thread/sleep 330000)
  
  
   
  
  (doseq [a agents] (utile/pause a))
  
  (println (.size url-queue))
  (println agents) 
  (println "users: " (count @users))
  (println "crawled: " (count @crawled-urls))
  
  (doseq [u (prepareUsersForDB (filter #(> (count (val %)) 2) @users))] 
    (db/addUser u))
  )

(defn runCrawler []
  (def urls2 (atom (readUrls)))
  (println (count @urls2))
  (runLinkExtraction)
  (Thread/sleep 20000)
  
  (println (count @urls2))
  (println (.size url-queue)))

(defn -main [& args]
(runScraper))