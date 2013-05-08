(ns utils.core-test
  (:use clojure.test
        utils.util))



(deftest test-string-to-number
  (testing "FIXME, I fail."
           (is (= (String->Number "5") 5))))



(deftest test-substring?
  (is (= (substring? "chicken" "chicken breasts, boneless, skinless with fillet removed") true)))


(run-tests)