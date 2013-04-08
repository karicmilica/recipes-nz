(ns extraction.recipe_extractor
  (:require 
           [net.cgrand.enlive-html :as html]
           [monger.collection :as mc]
           [utils.util-extraction :as utile]
           [db.db :as db])
  (:import (org.bson.types ObjectId))
  (:import (java.util.concurrent LinkedBlockingQueue BlockingQueue)))

(def users (atom {}))
(def url-queue (LinkedBlockingQueue.))


(declare get-url)


;; start

(def urls2 (atom 
      '("http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/20"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/40"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/60"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/80"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/100"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/120"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/140"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/160"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/180"
           "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/200"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Healthy-Recipes/%28offset%29/220"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Chinese-Recipes"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Chinese-Recipes/%28offset%29/20"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Mexican-Recipes"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Mexican-Recipes/%28offset%29/20" 
            "http://www.foodinaminute.co.nz/Recipe-Categories/Gluten-Free-Recipes" 
            "http://www.foodinaminute.co.nz/Recipe-Categories/Gluten-Free-Recipes/%28offset%29/20" 
            "http://www.foodinaminute.co.nz/Recipe-Categories/Gluten-Free-Recipes/%28offset%29/40" 
            "http://www.foodinaminute.co.nz/Recipe-Categories/Gluten-Free-Recipes/%28offset%29/60" 
            "http://www.foodinaminute.co.nz/Recipe-Categories/Vegetarian-Recipes"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Vegetarian-Recipes/%28offset%29/20"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Vegetarian-Recipes/%28offset%29/40"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Vegetarian-Recipes/%28offset%29/60"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Vegetarian-Recipes/%28offset%29/80"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Vegetarian-Recipes/%28offset%29/100"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Seafood-Fish-Recipes"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Seafood-Fish-Recipes/%28offset%29/20"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Seafood-Fish-Recipes/%28offset%29/40"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Seafood-Fish-Recipes/%28offset%29/60"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Easy-Dessert-Recipes"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Easy-Dessert-Recipes/%28offset%29/20"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Easy-Dessert-Recipes/%28offset%29/40"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Easy-Dessert-Recipes/%28offset%29/60"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Easy-Dessert-Recipes/%28offset%29/80"
            "http://www.foodinaminute.co.nz/Recipe-Categories/Easy-Dessert-Recipes/%28offset%29/100")))

(declare get-url-search)

(defn dequeue!
        [queue]
        (if (empty? @queue)
          (utile/pause *agent*)
         (loop []
          (let [q     @queue
                value (peek q)
                nq    (pop q)]
            (if (compare-and-set! queue q nq)
              value
              (recur))))))


(def agents-search (set (repeatedly 5 #(agent {::t #'get-url-search}))))

(defn runLinkExtraction
         ([] (doseq [a agents-search] (runLinkExtraction a)))
         ([a]
         (when (agents-search a)
         (send a (fn [{transition ::t :as state}]
         (when-not (utile/paused? *agent*)
         (let [dispatch-fn (if (-> transition meta ::blocking)
         send-off
         send)]
         (dispatch-fn *agent* transition)))
         state)))))


(defn ^::blocking handle-results-search
         [{:keys [links-recipes]}]
         (try
         (doseq [url links-recipes]
   (.put url-queue url))
         {::t #'get-url-search}
         (finally (runLinkExtraction *agent*))))

(defn process-search
            [{:keys [content]}]
            (try
   (let [html-content (html/html-resource (java.io.StringReader. content))
            articles (html/select html-content [:ul.search_results :li])]
            {::t #'handle-results-search
            :links-recipes (utile/get-links articles)
            }
   )
            (finally (runLinkExtraction *agent*))))

(defn ^::blocking get-url-search
         [{:keys [] :as state}]
         (let [url (dequeue! urls2)]
         (try
         {:url url
         :content (slurp url)
         ::t #'process-search}
         (catch Exception e
         ;; skip any URL we failed to load
         )
         (finally (runLinkExtraction *agent*)))))

;; end.





(def flag (atom false))

(def crawled-urls (atom #{}))



(defn setFlagTrue [arg] (compare-and-set! arg false true))

(defn prepareLink [link] (str "http://www.w3.org/2012/pyMicrodata/extract?format=json&uri="
                                    link))




(def agents (set (repeatedly 10 #(agent {::t #'get-url :queue url-queue}))))

(defn stop [] 
              (setFlagTrue flag)	
                          {::t #'get-url :queue url-queue})

(declare ingredient-category)








(defn runRecipesExtraction
      ([] (doseq [a agents] (runRecipesExtraction a)))
      ([a]
      (when (agents a)
      (send a (fn [{transition ::t :as state}]
      (when-not (utile/paused? *agent*)
      (let [dispatch-fn (if (-> transition meta ::blocking)
      send-off
      send)]
      (dispatch-fn *agent* transition)))
      state)))))





(defn store-results [url recipe usersRating]
  (swap! crawled-urls conj url)
           (db/addRecipe recipe)
         (swap! users (partial merge-with concat) usersRating))

(defn ^::blocking handle-results
         [{:keys [recipe usersRating url]}]
         (try
           (if-not (@crawled-urls url) 
           (store-results url recipe usersRating))
         {::t #'get-url
          :queue url-queue}
         (finally (runRecipesExtraction *agent*))))


(defn process
         [{:keys [content pageContent url]}]
         (try
           (let [html-content (html/html-resource (java.io.StringReader. pageContent))
                 id (ObjectId.)
                 recipe (utile/processData content @ingredient-category)]
         {:url url
          ::t #'handle-results
          :usersRating (utile/get-rating (utile/get-rating-div html-content) (.toString id)) 
         :recipe (assoc recipe :_id id) 
         }
         )(finally (runRecipesExtraction *agent*))))


(defn ^::blocking get-url
               [{:keys [^BlockingQueue queue] :as state}]
               (if (= @flag true) 
                 (utile/pause *agent*) 
               (let [urlstr (.take queue)]
               (try
                 (if (= "END" urlstr)
                   (stop)
                   (let [url (clojure.java.io/as-url urlstr)] 
                     (if (@crawled-urls url)
                   {::t #'get-url
                    :queue queue}
               {:url url
               :content (slurp (prepareLink url))
               :pageContent (slurp url)
               ::t #'process})))
               (catch Exception e
                  state
               )
               (finally (runRecipesExtraction *agent*))))))

(defn prepareUsersForDB [users] 
     (reduce (fn [l x] (conj l {:username (key x) :recipeRatings (into {} (val x))} )) '() users))


(defn runScraper []
  
  (db/db-init)
  (def ingredient-category (atom (mc/find-maps "ingredient")))
  (Thread/sleep 3000)
  (println "cat:" @ingredient-category)
  (runLinkExtraction) 
   (runRecipesExtraction)
  (Thread/sleep 100000)
  (.put url-queue "END")
  (Thread/sleep 300000)
  (println (.size url-queue))
  (println "users: " (count @users))
  (println "crawled: " (count @crawled-urls))
  
  (doseq [u (prepareUsersForDB (filter #(> (count (val %)) 2) @users))] 
    (db/addUser u)))

(defn -main [& args]
(runScraper))