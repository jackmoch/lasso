# Testing Guide for Contributors

Comprehensive guide for writing and maintaining tests in the Lasso project.

## Table of Contents

- [Getting Started](#getting-started)
- [Backend Testing](#backend-testing)
- [Frontend Testing](#frontend-testing)
- [Integration Testing](#integration-testing)
- [Test Organization](#test-organization)
- [Best Practices](#best-practices)
- [Common Patterns](#common-patterns)

## Getting Started

### Prerequisites

```bash
# Install dependencies
npm install
clj -P

# Verify test setup
bb test          # Backend tests
bb test:frontend # Frontend tests
```

### Running Tests

```bash
# Backend (fast feedback)
bb test                    # All backend tests
bb test:watch             # Watch mode
bb test:focus my-ns-test  # Single namespace

# Frontend
bb test:frontend          # All frontend tests
bb test:frontend:watch    # Watch mode

# All tests
bb test:all              # Backend + Frontend

# E2E (requires app running)
bb test:e2e              # Headless
bb test:e2e:headed       # Visible browser
bb test:e2e:ui           # Interactive UI
bb test:e2e:debug        # Debug mode

# Coverage
bb coverage              # Generate coverage report
```

## Backend Testing

### Test Structure

```clojure
(ns lasso.my-feature-test
  "Test description."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [lasso.my-feature :as feature]
            [lasso.test-helpers :as helpers]))

;; Fixtures
(defn reset-state-fixture [f]
  "Reset global state before each test."
  (helpers/reset-state!)
  (f))

(use-fixtures :each reset-state-fixture)

;; Tests
(deftest feature-test
  (testing "Feature behaves correctly"
    (let [result (feature/do-something {:input "data"})]
      (is (= expected result))
      (is (contains? result :key)))))
```

### Unit Testing

**Test pure functions directly:**

```clojure
(deftest parse-track-test
  (testing "Parses Last.fm track correctly"
    (let [lastfm-track {:artist {:#text "The Beatles"}
                       :name "Hey Jude"
                       :date {:uts "1234567890"}}
          parsed (engine/parse-lastfm-track lastfm-track)]
      (is (= "The Beatles" (:artist parsed)))
      (is (= "Hey Jude" (:track parsed)))
      (is (= 1234567890 (:timestamp parsed))))))
```

**Test with multiple cases:**

```clojure
(deftest validation-test
  (testing "Validates usernames"
    (is (true? (validate-username "validuser")))
    (is (false? (validate-username "")))
    (is (false? (validate-username nil)))
    (is (false? (validate-username "invalid user")))))
```

### Mocking Dependencies

**Use `with-redefs` for mocking:**

```clojure
(deftest api-call-test
  (testing "Handles API success"
    (with-redefs [client/api-request (fn [_] {:user {:name "testuser"}})]
      (let [result (feature/get-user-info "testuser")]
        (is (:success result))
        (is (= "testuser" (:username result))))))

  (testing "Handles API errors"
    (with-redefs [client/api-request (fn [_] {:error 6 :message "Not found"})]
      (let [result (feature/get-user-info "invalid")]
        (is (not (:success result)))
        (is (contains? result :error))))))
```

**Mock multiple functions:**

```clojure
(with-redefs [client/api-request (fn [req]
                                   (case (:method req)
                                     "user.getInfo" {:user {:name "user1"}}
                                     "user.getRecentTracks" {:recenttracks {:track []}}
                                     {:error "Unknown method"}))
              scrobble/scrobble-track (fn [_ _] {:success true})]
  ;; Test code using multiple mocked functions
  )
```

### Testing State Changes

**Session store tests:**

```clojure
(deftest session-update-test
  (testing "Updates session correctly"
    (let [{:keys [session-id]} (create-session "user" "key")
          updated (store/update-session
                   session-id
                   (fn [session]
                     (assoc-in session [:following-session :state] :active)))]
      (is (some? updated))
      (is (= :active (get-in updated [:following-session :state]))))))
```

### Testing HTTP Handlers

**Pedestal handler tests:**

```clojure
(deftest handler-test
  (testing "Returns success response"
    (with-redefs [api/call (fn [_] {:data "response"})]
      (let [request {:session {:session-id "test-id"}
                     :body {:input "data"}}
            response (handler request)]
        ;; Verify response structure
        (is (map? response))
        (is (= 200 (:status response)))
        (is (contains? response :body))

        ;; Verify response body
        (let [body (parse-json (:body response))]
          (is (= "response" (:data body)))))))

  (testing "Returns error response"
    (with-redefs [api/call (fn [_] {:error "API error"})]
      (let [response (handler request)]
        (is (= 500 (:status response)))))))
```

### Testing Async Code

**Use deref for promises/futures:**

```clojure
(deftest async-test
  (testing "Async operation completes"
    (let [result-future (future (expensive-operation))]
      ;; Wait for completion
      (is (= expected @result-future)))))
```

### Testing Edge Cases

**Boundary conditions:**

```clojure
(deftest boundary-test
  (testing "Empty input"
    (is (= [] (process-items []))))

  (testing "Single item"
    (is (= [1] (process-items [1]))))

  (testing "Large input"
    (let [large-list (range 10000)]
      (is (= 10000 (count (process-items large-list)))))))
```

**Error conditions:**

```clojure
(deftest error-handling-test
  (testing "Handles nil input"
    (is (thrown? NullPointerException (process nil))))

  (testing "Returns error map for invalid input"
    (let [result (process-safe nil)]
      (is (contains? result :error))
      (is (= "Invalid input" (:error result))))))
```

## Frontend Testing

### Re-frame Event Tests

```clojure
(ns lasso.events-test
  (:require [cljs.test :refer [deftest is testing]]
            [re-frame.core :as rf]
            [lasso.events :as events]
            [lasso.test-utils-simple :as tu]))

(deftest event-test
  (testing "Event updates db correctly"
    ;; Reset to known state
    (tu/set-db! tu/default-db)

    ;; Dispatch event synchronously
    (rf/dispatch-sync [:my-event {:data "value"}])

    ;; Verify db changes
    (is (= "value" (tu/get-in-db [:my-path :data])))
    (is (false? (tu/get-in-db [:ui :loading?])))))
```

**Testing event chains:**

```clojure
(deftest event-chain-test
  (testing "Event triggers subsequent events"
    (tu/set-db! tu/default-db)

    ;; First event
    (rf/dispatch-sync [:start-process])
    (is (true? (tu/get-in-db [:ui :loading?])))

    ;; Success event
    (rf/dispatch-sync [:process-success {:result "data"}])
    (is (false? (tu/get-in-db [:ui :loading?])))
    (is (= "data" (tu/get-in-db [:result])))))
```

### Re-frame Subscription Tests

```clojure
(deftest subscription-test
  (testing "Subscription returns correct value"
    (tu/set-db! (assoc tu/default-db :auth {:authenticated? true
                                             :username "user"}))

    ;; Test subscription manually (node env doesn't support reactive subscriptions)
    (let [result (get-in (tu/get-db) [:auth :authenticated?])]
      (is (true? result)))))
```

**Testing computed subscriptions:**

```clojure
(deftest computed-sub-test
  (testing "Computes derived value correctly"
    (tu/set-db! {:session {:state :active
                          :scrobble-count 5}})

    ;; Replicate subscription logic
    (let [db (tu/get-db)
          state (get-in db [:session :state])
          count (get-in db [:session :scrobble-count])
          can-pause? (and (= :active state) (> count 0))]
      (is (true? can-pause?)))))
```

### Component Tests

```clojure
(deftest component-test
  (testing "Component renders with correct structure"
    (tu/set-db! tu/authenticated-db)

    ;; Form-2 component
    (let [render-fn (my-component)
          component (render-fn)]
      (is (vector? component))
      (is (= :div (first component))))))

(deftest component-with-subscriptions-test
  (testing "Component uses subscriptions"
    (tu/set-db! tu/authenticated-db)

    (let [render-fn (my-component)
          component (render-fn)]
      ;; Check for subscription function references
      (is (some fn? (tree-seq coll? identity component))))))
```

## Integration Testing

### Handler Integration Tests

**Test full request/response cycle:**

```clojure
(deftest start-session-integration-test
  (testing "Complete start session flow"
    ;; Create authenticated session
    (let [{:keys [session-id]} (auth-session/create-session "user" "key")]

      ;; Mock API validation
      (with-redefs [client/api-request (fn [_] {:user {:name "target"}})]
        ;; Start session via handler
        (let [request {:session {:session-id session-id}
                       :body (json/write-str {:target_username "target"})}
              response (handlers/start-session-handler request)]

          ;; Verify response
          (is (= 200 (:status response)))

          ;; Verify session state
          (let [session (store/get-session session-id)]
            (is (= :active (get-in session [:following-session :state])))
            (is (= "target" (get-in session [:following-session :target-username])))))))))
```

### End-to-End Backend Tests

**Test complete workflows:**

```clojure
(deftest polling-workflow-test
  (testing "Complete polling and scrobbling workflow"
    ;; Setup
    (let [{:keys [session-id]} (create-session "user" "key")]

      ;; Start session
      (with-redefs [client/api-request (fn [_] {:user {:name "target"}})]
        (manager/start-session session-id "target"))

      ;; Poll and scrobble
      (with-redefs [client/api-request (fn [req]
                                        {:recenttracks
                                         {:track [{:artist {:#text "Artist"}
                                                  :name "Track"
                                                  :date {:uts "100"}}]}})
                    scrobble/scrobble-track (fn [_ _] {:success true})]
        (let [result (engine/execute-poll session-id)]
          (is (some? result))
          (is (= 1 (get-in result [:following-session :scrobble-count])))))

      ;; Stop session
      (let [result (manager/stop-session session-id)]
        (is (:success result))
        (is (nil? (get-in result [:session :following-session])))))))
```

## Test Organization

### File Structure

```
test/clj/lasso/
├── feature/
│   ├── core_test.clj        # Core feature tests
│   └── utils_test.clj       # Utility function tests
└── integration/
    └── feature_flow_test.clj # Integration tests
```

### Test Naming Conventions

- **Test files**: `<namespace>_test.clj`
- **Test functions**: `<feature>-test`
- **Testing blocks**: Descriptive strings
  - ✅ "Validates username format"
  - ❌ "Test 1"

### Grouping Tests

```clojure
(deftest feature-test
  (testing "Success cases"
    (testing "Valid input returns success"
      (is (= expected (feature/process valid-input))))

    (testing "Empty input returns empty result"
      (is (empty? (feature/process [])))))

  (testing "Error cases"
    (testing "Invalid input returns error"
      (is (contains? (feature/process invalid) :error)))

    (testing "Nil input returns error"
      (is (contains? (feature/process nil) :error)))))
```

## Best Practices

### 1. Test Behavior, Not Implementation

```clojure
;; ❌ Bad: Testing implementation details
(deftest bad-test
  (is (= 3 (count internal-cache))))

;; ✅ Good: Testing behavior
(deftest good-test
  (let [result (get-cached-items)]
    (is (= expected-items result))))
```

### 2. Use Descriptive Assertions

```clojure
;; ❌ Bad: Unclear what's being tested
(is (= 5 result))

;; ✅ Good: Clear intent
(is (= 5 (:scrobble-count result)) "Should have 5 scrobbles")
```

### 3. Test One Thing Per Test

```clojure
;; ❌ Bad: Testing multiple things
(deftest bad-test
  (is (= expected1 (feature1)))
  (is (= expected2 (feature2)))
  (is (= expected3 (feature3))))

;; ✅ Good: One thing per test
(deftest feature1-test
  (is (= expected1 (feature1))))

(deftest feature2-test
  (is (= expected2 (feature2))))
```

### 4. Make Tests Fast

```clojure
;; ❌ Bad: Unnecessary delays
(Thread/sleep 1000)

;; ✅ Good: Mock time-dependent code
(with-redefs [get-time (fn [] fixed-timestamp)]
  ;; test code
  )
```

### 5. Clean Up After Tests

```clojure
;; ✅ Use fixtures for cleanup
(defn cleanup-fixture [f]
  (try
    (f)
    (finally
      (cleanup-state!))))

(use-fixtures :each cleanup-fixture)
```

### 6. Don't Test External Services

```clojure
;; ❌ Bad: Real API call
(deftest bad-test
  (let [result (http/get "https://api.example.com")]
    (is (= 200 (:status result)))))

;; ✅ Good: Mock external calls
(deftest good-test
  (with-redefs [http/get (fn [_] {:status 200 :body "mock"})]
    (let [result (feature/fetch-data)]
      (is (= expected result)))))
```

## Common Patterns

### Testing Stateful Code

```clojure
(deftest stateful-test
  (testing "State changes correctly"
    ;; Capture initial state
    (let [initial (get-state)]

      ;; Perform operation
      (update-state! new-value)

      ;; Verify change
      (is (not= initial (get-state)))
      (is (= new-value (get-state))))))
```

### Testing Error Messages

```clojure
(deftest error-message-test
  (testing "Returns helpful error message"
    (let [result (validate invalid-data)]
      (is (not (:valid? result)))
      (is (string? (:error result)))
      (is (.contains (:error result) "username")))))
```

### Testing Collections

```clojure
(deftest collection-test
  (testing "Returns expected items"
    (let [result (get-items)]
      ;; Check type
      (is (vector? result))

      ;; Check count
      (is (= 3 (count result)))

      ;; Check contents
      (is (every? #(contains? % :id) result))
      (is (= #{1 2 3} (set (map :id result)))))))
```

### Testing Timestamps

```clojure
(deftest timestamp-test
  (testing "Uses current timestamp"
    (let [before (System/currentTimeMillis)
          result (create-record)
          after (System/currentTimeMillis)]
      (is (>= (:timestamp result) before))
      (is (<= (:timestamp result) after)))))
```

## Next Steps

- Read [E2E Testing Guide](./E2E_TESTING.md) for Playwright tests
- Check [Troubleshooting Guide](./TROUBLESHOOTING.md) for common issues
- See [Coverage Guide](./COVERAGE.md) for improving coverage
