# Test Coverage Improvement Guide

Strategies and best practices for improving test coverage in the Lasso project.

## Table of Contents

- [Current Coverage](#current-coverage)
- [Coverage Goals](#coverage-goals)
- [Analyzing Coverage](#analyzing-coverage)
- [Improving Coverage](#improving-coverage)
- [Prioritization](#prioritization)
- [Common Patterns](#common-patterns)
- [Avoiding Coverage Traps](#avoiding-coverage-traps)

## Current Coverage

**Overall Coverage: 79.53% forms, 91.01% lines**

```
High Coverage (95-100%):
✅ lasso.auth.handlers: 100%
✅ lasso.auth.session: 96.51%
✅ lasso.lastfm.oauth: 100%
✅ lasso.middleware: 100%
✅ lasso.session.handlers: 94.71%
✅ lasso.session.manager: 96.42%
✅ lasso.session.store: 99.23%
✅ lasso.util.crypto: 100%
✅ lasso.util.http: 97.50%
✅ lasso.validation.schemas: 100%

Good Coverage (80-95%):
🟡 lasso.config: 94.81%
🟡 lasso.lastfm.client: 85.71%
🟡 lasso.lastfm.scrobble: 97.00%
🟡 lasso.polling.engine: 86.56%

Needs Improvement (<80%):
⚠️ lasso.routes: 75.24%
⚠️ lasso.polling.scheduler: 43.70%
🔴 lasso.server: 11.03%
```

## Coverage Goals

### Target Coverage Levels

- **Critical Business Logic**: 95%+ (auth, session management, scrobbling)
- **API Handlers**: 90%+ (request/response handling)
- **Utilities**: 90%+ (pure functions, helpers)
- **Infrastructure**: 60%+ (server startup, scheduling)
- **Overall**: 80%+ (current: 79.53%)

### What Not to Test

It's OK to have lower coverage for:
- Server startup/shutdown code
- Main entry points (defonce, -main)
- Logging statements
- Development-only code
- Generated code
- Simple getters/setters

## Analyzing Coverage

### Generate Coverage Report

```bash
# Generate HTML report
bb coverage

# Open report
open target/coverage/index.html
```

### Reading the Report

**Coverage metrics explained:**

- **Forms Coverage**: Percentage of code forms executed
- **Lines Coverage**: Percentage of lines executed
- **Green (100%)**: Fully covered
- **Yellow (80-99%)**: Mostly covered
- **Red (<80%)**: Needs attention

**In the HTML report:**

- 🟢 **Green lines**: Executed during tests
- 🔴 **Red lines**: Never executed
- 🟡 **Yellow highlight**: Partially covered branches

### Finding Uncovered Code

```bash
# Generate report
bb coverage

# Look for red lines in:
# - target/coverage/index.html (overview)
# - target/coverage/lasso/<namespace>.html (detailed)
```

## Improving Coverage

### Strategy 1: Test Uncovered Branches

**Problem:** Conditional code not fully tested

```clojure
;; Uncovered: else branch
(defn process-item [item]
  (if (valid? item)
    (process-valid item)   ; ✅ Covered
    (handle-error item)))  ; ❌ Not covered
```

**Solution:** Add test for error case

```clojure
(deftest process-item-test
  (testing "Processes valid items"
    (is (= expected (process-item valid-item))))

  (testing "Handles invalid items"
    (is (= error-result (process-item invalid-item)))))
```

---

### Strategy 2: Test Error Paths

**Problem:** Error handling not tested

```clojure
(defn fetch-data []
  (try
    (http/get url)         ; ✅ Covered
    (catch Exception e     ; ❌ Not covered
      (log/error e)
      {:error "Failed"})))
```

**Solution:** Mock to throw exception

```clojure
(deftest fetch-data-error-test
  (testing "Handles network errors"
    (with-redefs [http/get (fn [_] (throw (Exception. "Network error")))]
      (let [result (fetch-data)]
        (is (contains? result :error))
        (is (= "Failed" (:error result)))))))
```

---

### Strategy 3: Test Edge Cases

**Problem:** Boundary conditions not tested

```clojure
(defn process-list [items]
  (when (seq items)       ; ❌ Empty case not tested
    (map process-one items)))
```

**Solution:** Test empty, single, and multiple items

```clojure
(deftest process-list-test
  (testing "Empty list"
    (is (nil? (process-list []))))

  (testing "Single item"
    (is (= [result1] (process-list [item1]))))

  (testing "Multiple items"
    (is (= [result1 result2] (process-list [item1 item2])))))
```

---

### Strategy 4: Test All Code Paths

**Problem:** Case statements with uncovered cases

```clojure
(defn handle-event [event-type data]
  (case event-type
    :success (handle-success data)  ; ✅ Covered
    :error (handle-error data)      ; ✅ Covered
    :pending (handle-pending data)  ; ❌ Not covered
    (handle-unknown event-type)))   ; ❌ Not covered
```

**Solution:** Test all cases

```clojure
(deftest handle-event-test
  (testing "Success events"
    (is (= expected (handle-event :success data))))

  (testing "Error events"
    (is (= expected (handle-event :error data))))

  (testing "Pending events"
    (is (= expected (handle-event :pending data))))

  (testing "Unknown events"
    (is (= expected (handle-event :unknown data)))))
```

---

### Strategy 5: Integration Tests

**Problem:** Individual functions tested but not together

**Solution:** Add integration tests

```clojure
(deftest complete-workflow-test
  (testing "End-to-end session workflow"
    ;; Setup
    (let [{:keys [session-id]} (create-session "user" "key")]

      ;; Start session
      (start-session session-id "target")
      (is (= :active (get-session-state session-id)))

      ;; Poll and scrobble
      (poll-and-scrobble session-id)
      (is (>= (get-scrobble-count session-id) 1))

      ;; Stop session
      (stop-session session-id)
      (is (nil? (get-following-session session-id))))))
```

## Prioritization

### High Priority (Do First)

1. **Critical business logic**
   - Authentication and authorization
   - Session management
   - Scrobble tracking
   - Data validation

2. **Error handling**
   - API failures
   - Network errors
   - Invalid input
   - Edge cases

3. **User-facing features**
   - Session control (start/pause/resume/stop)
   - Activity feed
   - Error messages

### Medium Priority

1. **Utilities and helpers**
   - HTTP utilities
   - Crypto functions
   - Validation schemas

2. **API integration**
   - Last.fm client
   - OAuth flow
   - Rate limiting

### Low Priority (Optional)

1. **Infrastructure code**
   - Server startup
   - Scheduler initialization
   - Route definitions

2. **Development tools**
   - REPL utilities
   - Debug helpers

## Common Patterns

### Pattern 1: Parameterized Tests

**Instead of multiple similar tests:**

```clojure
(deftest validation-test
  (testing "Valid inputs"
    (is (valid? "input1"))
    (is (valid? "input2"))
    (is (valid? "input3"))))
```

**Use parameterized approach:**

```clojure
(deftest validation-test
  (doseq [input ["input1" "input2" "input3"]]
    (testing (str "Validates " input)
      (is (valid? input)))))
```

---

### Pattern 2: Test Helpers

**Extract common test setup:**

```clojure
(defn create-test-session []
  (let [{:keys [session-id]} (auth-session/create-session "user" "key")]
    (store/update-session
     session-id
     (fn [session]
       (assoc session :following-session {:state :active
                                          :target-username "target"
                                          :scrobble-count 0
                                          :scrobble-cache #{}
                                          :started-at 0})))
    session-id))

;; Use in tests
(deftest my-test
  (let [session-id (create-test-session)]
    ;; Test code
    ))
```

---

### Pattern 3: Fixture-Based Setup

**Use fixtures for common state:**

```clojure
(defn authenticated-session-fixture [f]
  (let [{:keys [session-id]} (create-session "user" "key")]
    (binding [*test-session-id* session-id]
      (f))))

(use-fixtures :each authenticated-session-fixture)

(deftest session-test
  ;; *test-session-id* is available
  )
```

---

### Pattern 4: Property-Based Testing

**For complex logic, use test.check:**

```clojure
(ns lasso.validation-test
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(defspec username-validation-property 100
  (prop/for-all [username gen/string-alphanumeric]
    (boolean? (validate-username username))))
```

## Avoiding Coverage Traps

### Trap 1: Testing Implementation

❌ **Don't test internal details:**

```clojure
(deftest bad-test
  (testing "Uses specific algorithm"
    (is (= 3 (count @internal-cache)))))
```

✅ **Test behavior:**

```clojure
(deftest good-test
  (testing "Returns cached items"
    (is (= expected-items (get-items)))))
```

---

### Trap 2: Chasing 100%

**Not all code needs 100% coverage:**

- Server startup code
- Logging statements
- Development helpers
- One-time initialization

**Focus on:**
- Business logic
- Error handling
- User interactions
- Data transformations

---

### Trap 3: Brittle Tests

❌ **Don't couple tests to implementation:**

```clojure
(deftest brittle-test
  (is (= [:span {:class "text-red-500"} "Error"] (render-error "Error"))))
```

✅ **Test behavior, not structure:**

```clojure
(deftest flexible-test
  (let [component (render-error "Error")]
    (is (contains-text? component "Error"))
    (is (has-error-styling? component))))
```

---

### Trap 4: Slow Tests

**Keep tests fast:**

```clojure
;; ❌ Slow: Real delays
(Thread/sleep 1000)

;; ✅ Fast: Mock time
(with-redefs [get-time (fn [] fixed-time)]
  ;; test code
  )

;; ❌ Slow: Real API calls
(http/get "https://api.example.com")

;; ✅ Fast: Mock API
(with-redefs [http/get (fn [_] mock-response)]
  ;; test code
  )
```

## Target Areas for Improvement

### lasso.server (11% → 60%)

**Current issues:**
- Server startup not tested
- Main entry point not exercised

**Improvements:**
```clojure
(deftest server-lifecycle-test
  (testing "Server starts and stops cleanly"
    (let [server (start-server {:port 8081})]
      (is (some? server))
      (is (server-running? 8081))
      (stop-server server)
      (is (not (server-running? 8081))))))
```

---

### lasso.polling.scheduler (43% → 70%)

**Current issues:**
- Scheduler lifecycle not tested
- Timer management not covered

**Improvements:**
```clojure
(deftest scheduler-test
  (testing "Starts and stops polling"
    (let [poll-count (atom 0)]
      (with-redefs [engine/execute-poll (fn [_] (swap! poll-count inc))]
        (scheduler/start-poller "test-id" 100)
        (Thread/sleep 250)
        (scheduler/stop-poller "test-id")
        (is (>= @poll-count 2))
        (is (<= @poll-count 3))))))
```

---

### lasso.routes (75% → 85%)

**Current issues:**
- Some route handlers not tested
- Error middleware not exercised

**Improvements:**
```clojure
(deftest routes-test
  (testing "All routes defined"
    (is (route-exists? :get "/api/session/status"))
    (is (route-exists? :post "/api/session/start"))
    (is (route-exists? :post "/api/session/pause")))

  (testing "404 for unknown routes"
    (is (= 404 (status-for :get "/api/nonexistent")))))
```

## Measuring Progress

### Track Coverage Over Time

```bash
# Generate coverage
bb coverage

# View summary
cat target/coverage/codecov.json | jq '.coverage_summary'

# Track in git
git add .
git commit -m "test: improve coverage from 79% to 82%"
```

### Set Coverage Goals

**In PR reviews:**
- ✅ New code must have 80%+ coverage
- ✅ PRs should not decrease overall coverage
- ⚠️ Exceptions require justification

**In CI:**
```yaml
# Future: Enforce coverage minimums
- name: Check coverage
  run: |
    COVERAGE=$(cat target/coverage/codecov.json | jq '.coverage')
    if [ $COVERAGE -lt 80 ]; then
      echo "Coverage $COVERAGE% is below 80% threshold"
      exit 1
    fi
```

## Next Steps

1. **Generate current report:**
   ```bash
   bb coverage
   open target/coverage/index.html
   ```

2. **Pick a low-coverage namespace:**
   - Start with medium priority (routes, scheduler)
   - Avoid infrastructure (server) initially

3. **Write missing tests:**
   - Focus on untested branches
   - Add error case tests
   - Add edge case tests

4. **Verify improvement:**
   ```bash
   bb coverage
   # Check new percentage
   ```

5. **Repeat** until target coverage achieved

## Resources

- [Cloverage Documentation](https://github.com/cloverage/cloverage)
- [Effective Unit Testing](https://github.com/mockito/mockito/wiki/How-to-write-good-tests)
- [Testing Guide](./TESTING_GUIDE.md)
- [Troubleshooting](./TROUBLESHOOTING.md)
