(ns utils.core-test
  (:use clojure.test
        utils.util
        ))



(deftest test-string-to-number
  (testing "FIXME, I fail."
  (is (= (String->Number "5") 5))))




(run-tests)