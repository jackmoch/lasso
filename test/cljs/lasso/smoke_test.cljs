(ns lasso.smoke-test
  "Smoke tests to verify test infrastructure"
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [re-frame.core :as rf]
            [lasso.db :as db]
            [lasso.events]  ; Load events
            [lasso.subs]    ; Load subs
            [lasso.test-utils :as tu]))

(use-fixtures :each tu/with-fresh-db)

(deftest basic-re-frame-test
  (testing "Can set and read db"
    (tu/set-db! {:test "value"})
    (is (= {:test "value"} (tu/get-db)))))

(deftest initialize-db-test
  (testing "Initialize-db event sets default state"
    (rf/dispatch-sync [:initialize-db])
    (let [db (tu/get-db)]
      (is (= db/default-db db))
      (is (false? (tu/get-in-db [:auth :authenticated?])))
      (is (= :not-started (tu/get-in-db [:session :state]))))))

(deftest event-db-updates-test
  (testing "Events update db state"
    (tu/set-db! tu/authenticated-db)
    (rf/dispatch-sync [:ui/clear-error])
    (is (nil? (tu/get-in-db [:ui :error])))))

(deftest test-infrastructure-works
  (testing "Test infrastructure is working"
    (is true "Basic assertion")
    (is (= 1 1) "Equality assertion")
    (is (not false) "Negation assertion")))
