(ns recommendation.recommendation-test
  (:use clojure.test
        utils.util
        recommendation.similarity))


(deftest test-euclidean
  (testing "FIXME, I fail."
           (is (= 0.2
                  (euclidean {:5184d57c2211eb056d913943 2 :5184d5842211eb056d913951 3
                              :5184d4e92211eb056d91381d 3 :5184d4c62211eb056d9137d9 4}
                             {:5184d57c2211eb056d913943 3 :5184d5842211eb056d913951 4
                              :5184d4e92211eb056d91381d 2  :5184d4c62211eb056d9137d9 5 
                              :5184d4fe2211eb056d913846 3})))))

(deftest test-pearson
  (testing "FIXME, I fail."
           (is (=  0.632 
                   (String->Number (format "%.3f" 
                                           (pearson {:5184d57c2211eb056d913943 2 :5184d5842211eb056d913951 3
                                                     :5184d4e92211eb056d91381d 3 :5184d4c62211eb056d9137d9 4}
                                                    {:5184d57c2211eb056d913943 3 :5184d5842211eb056d913951 4
                                                     :5184d4e92211eb056d91381d 2  :5184d4c62211eb056d9137d9 5 
                                                     :5184d4fe2211eb056d913846 3})))))))

           
(run-tests)