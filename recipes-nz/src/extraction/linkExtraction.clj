(ns extraction.linkExtraction
  (:require [clojure.data.json :as json]
           [clj-http.client :as client]
           [net.cgrand.enlive-html :as html]
           [utils.util-extraction :as utile])
  (:import (java.util.concurrent LinkedBlockingQueue BlockingQueue)))

(def urls2 (atom 
      '("http://www.foodinaminute.co.nz/Recipe-Categories/Lasagne-Pasta-Recipes/"
        "http://www.foodinaminute.co.nz/Recipe-Categories/Bacon-Ham-Recipes"
        "http://www.foodinaminute.co.nz/Recipe-Categories/Cheese-Recipes"
        "http://www.foodinaminute.co.nz/Recipe-Categories/Noodle-Rice-Recipes"
        "http://www.foodinaminute.co.nz/Recipe-Categories/Turkey-Recipes")))

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



(def url-queue (LinkedBlockingQueue.))
(def agents-search (set (repeatedly 5 #(agent {::t #'get-url-search}))))



(defn run2
         ([] (doseq [a agents-search] (run2 a)))
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
         (finally (run2 *agent*))))

(defn process-search
            [{:keys [content]}]
            (try
   (let [html-content (html/html-resource (java.io.StringReader. content))
            articles (html/select html-content [:ul.search_results :li])]
            {::t #'handle-results-search
            :links-recipes (utile/get-links articles)
            }
   )
            (finally (run2 *agent*))))

(defn ^::blocking get-url-search
         [{:keys [] :as state}]
         (let [url (dequeue! urls2)]
         (try
         {:url url
         :content (slurp url)
         ::t #'process-search}
         (catch Exception e
         state
         )
         (finally (run2 *agent*)))))

(defn runCrawler []
  (println @urls2)
  (run2)
  (Thread/sleep 20000)
  (println (.size url-queue)))

(defn -main [& args]
(runCrawler))

