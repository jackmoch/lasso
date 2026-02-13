# Testing Documentation

Comprehensive testing guides for the Lasso project.

## Quick Start

```bash
# Run all backend tests
bb test

# Run all frontend tests
bb test:frontend

# Run E2E tests (requires app running)
bb test:e2e

# Run all tests
bb test:all

# Generate coverage report
bb coverage
```

## Test Overview

### Test Suites

1. **Backend Tests** (90 tests, 482 assertions)
   - Unit tests for business logic
   - Integration tests for API handlers
   - Edge case tests for error scenarios
   - Coverage: 79.53% forms, 91.01% lines

2. **Frontend Tests** (66 tests, 197 assertions)
   - Event handler tests (Re-frame events)
   - Subscription tests (Re-frame subscriptions)
   - Component tests (Reagent components)

3. **E2E Tests** (7 tests, 15 skipped)
   - Full application flow testing
   - Browser-based testing with Playwright
   - Pending: Authentication mock implementation

### Test Structure

```
test/
├── clj/                    # Backend tests
│   └── lasso/
│       ├── auth/           # Authentication tests
│       ├── lastfm/         # Last.fm API tests
│       ├── polling/        # Polling engine tests
│       ├── session/        # Session management tests
│       ├── util/           # Utility function tests
│       ├── validation/     # Schema validation tests
│       └── integration/    # Integration tests
│           ├── edge_cases_test.clj
│           └── manual_testing_issues_test.clj
├── cljs/                   # Frontend tests
│   └── lasso/
│       ├── test_utils_simple.cljs
│       ├── smoke_test.cljs
│       ├── events_test.cljs
│       ├── subs_test.cljs
│       └── components_test.cljs
└── e2e/                    # E2E tests
    ├── helpers.js
    ├── 01-app-loads.spec.js
    ├── 02-session-lifecycle.spec.js
    └── 03-error-handling.spec.js
```

## Documentation

- **[Testing Guide](./TESTING_GUIDE.md)** - Comprehensive guide for contributors
- **[E2E Testing](./E2E_TESTING.md)** - End-to-end testing with Playwright
- **[Troubleshooting](./TROUBLESHOOTING.md)** - Common issues and solutions
- **[Coverage Guide](./COVERAGE.md)** - Improving test coverage

## CI Integration

Tests run automatically on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

CI includes:
- Backend test execution
- Frontend test execution
- Coverage report generation
- Artifact uploads (coverage reports, JUnit XML)

View test results and coverage in:
- GitHub Actions workflow runs
- PR status comments
- [Codecov.io](https://codecov.io/gh/jackmoch/lasso)

## Test Conventions

### Backend Tests

- **Location**: `test/clj/lasso/`
- **Framework**: Kaocha (clojure.test)
- **Naming**: `<namespace>_test.clj`
- **Fixtures**: Use `use-fixtures` for setup/teardown
- **Mocking**: Use `with-redefs` for mocking dependencies

### Frontend Tests

- **Location**: `test/cljs/lasso/`
- **Framework**: cljs.test
- **Environment**: Node.js (shadow-cljs :node-test)
- **Naming**: `<namespace>_test.cljs`
- **Utilities**: `test_utils_simple.cljs` for Re-frame helpers

### E2E Tests

- **Location**: `test/e2e/`
- **Framework**: Playwright
- **Naming**: `<number>-<description>.spec.js`
- **Browser**: Chromium (configurable)
- **Helpers**: `helpers.js` for common operations

## Best Practices

### Writing Tests

1. **Test one thing at a time** - Each test should verify a single behavior
2. **Use descriptive names** - Test names should clearly describe what is being tested
3. **Arrange-Act-Assert** - Structure tests with setup, execution, and verification
4. **Mock external dependencies** - Don't make real API calls in unit tests
5. **Clean up after tests** - Use fixtures to reset state between tests

### Test Organization

1. **Group related tests** - Use `testing` blocks to organize related assertions
2. **Use fixtures** - Extract common setup/teardown logic
3. **Keep tests fast** - Unit tests should run in milliseconds
4. **Avoid test interdependence** - Tests should be able to run in any order

### Coverage Guidelines

1. **Aim for 80%+ coverage** - Current: 79.53% forms, 91.01% lines
2. **Focus on critical paths** - Prioritize business logic and user flows
3. **Don't chase 100%** - Some code (server startup, logging) is hard to test
4. **Test edge cases** - Error handling, boundary conditions, race conditions

## Quick Reference

### Backend Testing

```clojure
;; Basic test
(deftest my-test
  (testing "Description"
    (is (= expected actual))))

;; With fixtures
(use-fixtures :each reset-state-fixture)

;; With mocking
(with-redefs [external/fn (fn [] {:mock "data"})]
  ;; test code
  )

;; Integration test
(let [response (handler request)]
  (is (= 200 (:status response)))
  (is (= expected (parse-body response))))
```

### Frontend Testing

```clojure
;; Re-frame event test
(deftest event-test
  (tu/set-db! tu/default-db)
  (rf/dispatch-sync [:my-event])
  (is (= expected (tu/get-in-db [:path]))))

;; Component test
(deftest component-test
  (tu/set-db! tu/authenticated-db)
  (let [component (my-component)]
    (is (vector? component))))
```

### E2E Testing

```javascript
// Basic E2E test
test('should load app', async ({ page }) => {
  await page.goto('/');
  await waitForAppReady(page);
  await expect(page.locator('#app')).toBeVisible();
});
```

## Further Reading

- [Clojure Testing Documentation](https://clojure.org/guides/test)
- [Kaocha Documentation](https://cljdoc.org/d/lambdaisland/kaocha/)
- [Re-frame Testing](https://day8.github.io/re-frame/Testing/)
- [Playwright Documentation](https://playwright.dev)
