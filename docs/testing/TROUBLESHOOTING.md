# Testing Troubleshooting Guide

Common issues and solutions when running tests in the Lasso project.

## Table of Contents

- [Backend Tests](#backend-tests)
- [Frontend Tests](#frontend-tests)
- [E2E Tests](#e2e-tests)
- [Coverage Issues](#coverage-issues)
- [CI Issues](#ci-issues)
- [General Tips](#general-tips)

## Backend Tests

### Tests Won't Run

**Problem:** `bb test` fails with "No such task"

**Solution:**
```bash
# Verify babashka is installed
bb --version

# Reinstall if needed
brew install babashka

# Or update deps.edn paths
clj -M:test
```

---

**Problem:** "Could not find namespace"

**Solution:**
```bash
# Clear caches
rm -rf .cpcache .shadow-cljs target/

# Redownload dependencies
clj -P

# Try again
bb test
```

---

### Test Failures

**Problem:** "session-id nil" errors

**Solution:** Check middleware is attaching session to `[:request :session]`, not `[:session]`

```clojure
;; ❌ Wrong
(get context :session)

;; ✅ Correct
(get-in context [:request :session])
```

---

**Problem:** "No method in multimethod" errors

**Solution:** Ensure all required namespaces are loaded before tests run

```clojure
;; In test namespace
(:require [lasso.routes :as routes]  ; Force route registration
          [lasso.handlers :as handlers])
```

---

**Problem:** Tests pass locally but fail in CI

**Solution:**
- Check for timing issues (use fixtures, not delays)
- Verify environment variables are set in CI
- Check for hardcoded localhost URLs
- Look for file path issues (absolute vs relative)

```clojure
;; ❌ Bad: Hardcoded path
(def config-file "/Users/me/project/config.edn")

;; ✅ Good: Relative path
(def config-file (io/resource "config.edn"))
```

---

### Fixtures Not Working

**Problem:** State persists between tests

**Solution:**
```clojure
;; Ensure fixture is applied
(use-fixtures :each reset-state-fixture)

;; Fixture must take a function
(defn reset-state-fixture [f]
  (reset-state!)
  (f)  ; Don't forget to call f!
  )
```

---

### Mocking Issues

**Problem:** Mocked functions not being called

**Solution:**
```clojure
;; ❌ Wrong: Mocking after function is captured
(let [fn-ref my-ns/my-fn]
  (with-redefs [my-ns/my-fn (fn [] :mock)]
    (fn-ref)))  ; Calls original, not mock

;; ✅ Correct: Reference function within with-redefs
(with-redefs [my-ns/my-fn (fn [] :mock)]
  (my-ns/my-fn))  ; Calls mock
```

---

**Problem:** "Cannot redefine var" errors

**Solution:** Don't use `with-redefs` on private functions

```clojure
;; ❌ Bad: Redefining private fn
(with-redefs [my-ns/private-fn ...])

;; ✅ Good: Make it public or mock the public interface
(with-redefs [my-ns/public-fn ...])
```

---

## Frontend Tests

### Shadow-cljs Issues

**Problem:** "Build failed: namespace not found"

**Solution:**
```bash
# Clear shadow-cljs cache
rm -rf .shadow-cljs/

# Clear node cache
rm -rf node_modules/.cache/

# Rebuild
bb test:frontend
```

---

**Problem:** Tests hang or never complete

**Solution:**
```bash
# Kill existing shadow-cljs processes
pkill -f shadow-cljs

# Clear caches
bb clean:frontend

# Try again
bb test:frontend
```

---

### Re-frame Test Issues

**Problem:** "Cannot read property 'deref' of undefined"

**Solution:** Re-frame isn't initialized. Use test utilities:

```clojure
;; ❌ Wrong: Direct re-frame access
@(rf/subscribe [:my-sub])

;; ✅ Correct: Use test utilities
(tu/get-in-db [:path])
```

---

**Problem:** Subscriptions return nil in tests

**Solution:** Node.js environment doesn't support reactive subscriptions

```clojure
;; ❌ Won't work: Reactive subscriptions in Node
(let [result @(rf/subscribe [:my-sub])]
  (is (= expected result)))

;; ✅ Correct: Manual subscription logic
(let [result (get-in (tu/get-db) [:path])]
  (is (= expected result)))
```

---

**Problem:** Events not updating db

**Solution:** Use `rf/dispatch-sync` in tests

```clojure
;; ❌ Wrong: Async dispatch
(rf/dispatch [:my-event])
(is (= expected (tu/get-in-db [:path])))  ; Might not be updated yet

;; ✅ Correct: Sync dispatch
(rf/dispatch-sync [:my-event])
(is (= expected (tu/get-in-db [:path])))  ; Definitely updated
```

---

### Component Test Issues

**Problem:** "Component is not a vector"

**Solution:** Handle Form-2 components correctly

```clojure
;; Form-2 component returns a function
(let [render-fn (my-component)  ; Returns function
      component (render-fn)]    ; Call to get hiccup
  (is (vector? component)))
```

---

**Problem:** "Cannot read property 'call' of undefined"

**Solution:** Mock subscriptions or set up db state

```clojure
;; Setup db before rendering
(tu/set-db! tu/authenticated-db)

(let [component (my-component)]
  ;; Test component
  )
```

---

## E2E Tests

### Playwright Installation Issues

**Problem:** "Chromium not found"

**Solution:**
```bash
# Install browsers
npx playwright install chromium

# Or install all dependencies
npx playwright install --with-deps chromium
```

---

**Problem:** "ECONNREFUSED" errors

**Solution:** Ensure app is running before tests

```bash
# Terminal 1: Start app
bb dev

# Terminal 2: Wait for app, then run tests
# Wait for "Backend ready" message
bb test:e2e
```

---

### Test Timeouts

**Problem:** Tests timeout waiting for elements

**Solution:** Increase timeouts or fix element selectors

```javascript
// Increase timeout for specific assertion
await expect(page.locator('#slow-element'))
  .toBeVisible({ timeout: 10000 });

// Or in config
// playwright.config.js
expect: { timeout: 10000 }
```

---

**Problem:** "waitForAppReady" times out

**Solution:**
```javascript
// Check if Re-frame is loaded
await page.goto('/');
const hasReframe = await page.evaluate(() => {
  return typeof window.re_frame !== 'undefined';
});

if (!hasReframe) {
  console.error('Re-frame not loaded. Check app build.');
}
```

---

### Element Not Found

**Problem:** "Locator not found"

**Solution:** Verify element exists and selector is correct

```javascript
// Debug: Print all elements
const elements = await page.locator('button').all();
for (const el of elements) {
  console.log(await el.textContent());
}

// Use more specific selector
await page.locator('button:has-text("Exact Text")');
await page.locator('[data-testid="my-button"]');
```

---

### Flaky Tests

**Problem:** Tests pass sometimes, fail sometimes

**Solution:**
```javascript
// ❌ Bad: Race conditions
await page.click('#button');
await page.locator('#result').textContent(); // Might not be updated yet

// ✅ Good: Wait for stable state
await page.click('#button');
await expect(page.locator('#result')).toHaveText('Expected');
```

---

## Coverage Issues

### Cloverage Hangs

**Problem:** Coverage generation hangs indefinitely

**Solution:**
```bash
# Kill hanging processes
pkill -f cloverage
pkill -f java

# Try with limited namespaces
clj -M:coverage --src-ns-path src/clj/lasso/auth
```

---

**Problem:** "Out of memory" errors

**Solution:** Increase JVM memory

```bash
# Increase memory
export JAVA_OPTS="-Xmx4g"
clj -M:coverage
```

---

### Low Coverage Warnings

**Problem:** Coverage below expectations

**Solution:** See [Coverage Guide](./COVERAGE.md) for strategies to improve coverage

---

### Coverage Report Empty

**Problem:** No coverage data generated

**Solution:**
```bash
# Ensure tests run successfully first
bb test

# Then generate coverage
bb coverage

# Check output directory
ls -la target/coverage/
```

---

## CI Issues

### CI Fails But Local Passes

**Problem:** Tests pass locally but fail in CI

**Common causes:**

1. **Missing environment variables**
   ```yaml
   # .github/workflows/ci.yml
   env:
     LASTFM_API_KEY: ${{ secrets.LASTFM_API_KEY }}
   ```

2. **Timing issues**
   ```clojure
   ;; Use fixtures instead of delays
   (use-fixtures :each reset-state-fixture)
   ```

3. **File path issues**
   ```clojure
   ;; Use io/resource for files
   (io/resource "config.edn")
   ```

4. **Cache issues**
   ```yaml
   # Clear caches in CI
   - name: Clear caches
     run: rm -rf .cpcache .shadow-cljs target/
   ```

---

### CI Takes Too Long

**Problem:** CI runs exceed time limits

**Solution:**
```yaml
# Use caching
- uses: actions/cache@v4
  with:
    path: |
      ~/.m2/repository
      ~/.gitlibs
      .cpcache
    key: ${{ runner.os }}-clojure-${{ hashFiles('deps.edn') }}

# Run tests in parallel (if possible)
jobs:
  backend-tests:
    # ...
  frontend-tests:
    # ...
```

---

### Codecov Upload Fails

**Problem:** Coverage upload to Codecov fails

**Solution:**
```bash
# Verify codecov.json exists
ls -la target/coverage/codecov.json

# Check token is set in GitHub secrets
# Settings > Secrets > CODECOV_TOKEN

# Upload manually (for debugging)
bash <(curl -s https://codecov.io/bash) -f target/coverage/codecov.json
```

---

## General Tips

### Debugging Test Failures

1. **Run single test:**
   ```bash
   bb test:focus lasso.my-feature-test
   ```

2. **Add debug output:**
   ```clojure
   (println "Debug:" (pr-str data))
   ```

3. **Use REPL:**
   ```bash
   clj -M:dev:test
   user=> (require 'lasso.my-feature-test)
   user=> (clojure.test/run-tests 'lasso.my-feature-test)
   ```

4. **Check fixtures:**
   ```clojure
   ;; Print state in fixture
   (defn debug-fixture [f]
     (println "Before:" (get-state))
     (f)
     (println "After:" (get-state)))
   ```

---

### Performance Tips

1. **Slow tests:**
   - Profile with `time` command
   - Remove unnecessary delays
   - Mock external calls
   - Use fixtures efficiently

2. **Memory issues:**
   - Increase JVM memory: `-Xmx4g`
   - Clear caches between runs
   - Close resources in finally blocks

3. **Test isolation:**
   - Use `:each` fixtures for clean state
   - Don't share mutable state between tests
   - Reset global atoms/refs

---

### Getting Help

1. **Check logs:**
   ```bash
   # Backend tests
   clj -M:test 2>&1 | tee test-output.log

   # Frontend tests
   npx shadow-cljs compile test 2>&1 | tee test-output.log
   ```

2. **Minimal reproduction:**
   - Create minimal test case
   - Remove unrelated code
   - Share error message and stack trace

3. **Resources:**
   - Project docs: `docs/testing/`
   - Clojure docs: https://clojure.org/guides/test
   - Playwright docs: https://playwright.dev
   - Re-frame docs: https://day8.github.io/re-frame/

---

## Quick Fixes Checklist

When tests fail, try these steps in order:

```bash
# 1. Clear all caches
bb clean
rm -rf node_modules/.cache

# 2. Reinstall dependencies
clj -P
npm ci

# 3. Run single test to isolate issue
bb test:focus problematic-test

# 4. Check recent changes
git diff HEAD~1

# 5. Check CI logs
gh run view --log

# 6. Still stuck? Check documentation
ls docs/testing/
```

---

## Common Error Messages

| Error | Likely Cause | Solution |
|-------|-------------|----------|
| "namespace not found" | Missing dependency | `clj -P` to download deps |
| "session-id nil" | Middleware issue | Check session attachment |
| "cannot read property" | Re-frame not initialized | Use test utilities |
| "ECONNREFUSED" | App not running | Start app with `bb dev` |
| "timeout exceeded" | Slow assertion | Increase timeout |
| "out of memory" | JVM heap size | Export `JAVA_OPTS="-Xmx4g"` |

---

For more help, see:
- [Testing Guide](./TESTING_GUIDE.md)
- [E2E Testing](./E2E_TESTING.md)
- [Coverage Guide](./COVERAGE.md)
