(ns behave-tree.core-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [behave-tree.core :refer [in-interval?  in-free-charge-time? in-cheap-charge-time? minutes-until-target forecast-soc?]]))

(deftest test-in-interval?
  (testing "in-interval? function"
    (is (true? (in-interval? (java.time.LocalTime/of 10 0) 9 11)))  ; 10:00 is within 9:00 and 11:00
    (is (false? (in-interval? (java.time.LocalTime/of 8 0) 9 11)))  ; 8:00 is before 9:00
    (is (false? (in-interval? (java.time.LocalTime/of 12 0) 9 11))) ; 12:00 is after 11:00
    (is (true? (in-interval? (java.time.LocalTime/of 9 0) 9 11)))   ; 9:00 is the start
    (is (false? (in-interval? (java.time.LocalTime/of 11 0) 9 11)))  ; 11:00 is the end
    (is (false? (in-interval? (java.time.LocalTime/of 8 59) 9 11))) ; 8:59 is before the start
    (is (false? (in-interval? (java.time.LocalTime/of 11 1) 9 11))))) ; 11:01 is after the end


(deftest test-in-free-charge-time?
  (is (true? (in-free-charge-time? (java.time.LocalTime/of 12 0)))) ; 12:00 is within free charge time
  (is (false? (in-free-charge-time? (java.time.LocalTime/of 9 0)))) ; 9:00 is outside
  (is (false? (in-free-charge-time? (java.time.LocalTime/of 13 0)))) ; 13:00 is outside
)

(deftest test-in-cheap-charge-time?
  (is (true? (in-cheap-charge-time? (java.time.LocalTime/of 5 0)))) ; 5:00 is within cheap charge time
  (is (false? (in-cheap-charge-time? (java.time.LocalTime/of 8 0)))) ; 8:00 is outside
  (is (false? (in-cheap-charge-time? (java.time.LocalTime/of 9 0)))) ; 9:00 is outside
)

(deftest test-minutes-until-target
  (testing "minutes-until-target function"
    (let [current-time (java.time.LocalTime/of 13 0)]
      (is (= (minutes-until-target current-time (java.time.LocalTime/of 14 0)) 60))  ; 1 hour ahead
      (is (= (minutes-until-target current-time (java.time.LocalTime/of 12 30)) 1410)) ; 30 minutes ahead
      (is (= (minutes-until-target current-time (java.time.LocalTime/of 13 0)) 0))   ; same time
      (is (= (minutes-until-target current-time (java.time.LocalTime/of 11 0)) 1320)) ; 2 hours behind
      (is (= (minutes-until-target current-time (java.time.LocalTime/of 15 0)) 120)) ; 2 hours ahead
      (is (= (minutes-until-target current-time (java.time.LocalTime/of 6 0)) 1020))  ; 6 AM is 7 hours behind
    )))

(deftest test-forecast-soc?
  (testing "forecast-soc? function"
    (let [current-soc 30
          target-soc 45
          charge-rate-mins (/ 5 60.0)
          current-time (java.time.LocalTime/of 5 0)]
      ;; Test case where forecasted SOC is within limit
      (is (true? (forecast-soc? 42 target-soc current-time (java.time.LocalTime/of 6 0) charge-rate-mins)))

      ;; Test case where forecasted SOC exceeds limit
      (is (false? (forecast-soc? 30 target-soc current-time (java.time.LocalTime/of 6 0) charge-rate-mins)))

      ;; Test case where forecasted SOC is equal to limit
      (is (true? (forecast-soc? 40 target-soc current-time (java.time.LocalTime/of 6 0) charge-rate-mins)))

      ;; Test case with a different charge rate
      (is (false? (forecast-soc? current-soc target-soc current-time (java.time.LocalTime/of 6 0) (/ 10 60.0))))

      ;; Test case with a target time that is not during charging hours
      (is (true? (forecast-soc? current-soc target-soc current-time (java.time.LocalTime/of 8 0) charge-rate-mins)))
    )))
