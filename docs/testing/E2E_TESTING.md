# End-to-End Testing with Playwright

Comprehensive guide for E2E testing in the Lasso project using Playwright.

## Table of Contents

- [Overview](#overview)
- [Setup](#setup)
- [Running E2E Tests](#running-e2e-tests)
- [Writing E2E Tests](#writing-e2e-tests)
- [Test Helpers](#test-helpers)
- [Best Practices](#best-practices)
- [Debugging](#debugging)
- [CI Integration](#ci-integration)

## Overview

E2E tests verify the complete application flow from the user's perspective. We use Playwright for browser automation and testing.

**Current Status:**
- ✅ 7 tests passing (app loads, UI elements)
- ⏸️ 15 tests skipped (pending auth mock)
- 📍 Location: `test/e2e/`

## Setup

### Prerequisites

```bash
# Install Playwright and browsers
npm install
npx playwright install chromium
```

### Configuration

**playwright.config.js:**
```javascript
module.exports = defineConfig({
  testDir: './test/e2e',
  timeout: 30 * 1000,        // 30s per test
  expect: { timeout: 5000 }, // 5s per assertion
  use: {
    baseURL: 'http://localhost:8080',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: devices['Desktop Chrome'] },
  ],
});
```

## Running E2E Tests

### Local Development

```bash
# 1. Start the app
bb dev

# 2. Run tests (in another terminal)
bb test:e2e           # Headless mode
bb test:e2e:headed    # Visible browser
bb test:e2e:ui        # Interactive UI mode
bb test:e2e:debug     # Debug mode (step through)
```

### Quick Commands

```bash
# Run all tests
npx playwright test

# Run specific test file
npx playwright test 01-app-loads

# Run specific test
npx playwright test -g "should load app"

# Run with UI
npx playwright test --ui

# Run in headed mode
npx playwright test --headed

# Run in debug mode
npx playwright test --debug

# Generate test report
npx playwright show-report
```

## Writing E2E Tests

### Basic Test Structure

```javascript
const { test, expect } = require('@playwright/test');
const { waitForAppReady } = require('./helpers');

test.describe('Feature Name', () => {
  test('should do something', async ({ page }) => {
    // Navigate to page
    await page.goto('/');

    // Wait for app to be ready
    await waitForAppReady(page);

    // Perform actions
    await page.click('#my-button');

    // Make assertions
    await expect(page.locator('#result')).toBeVisible();
    await expect(page.locator('#result')).toHaveText('Expected');
  });
});
```

### Test File Organization

```
test/e2e/
├── helpers.js                    # Shared helper functions
├── 01-app-loads.spec.js         # Basic app loading
├── 02-session-lifecycle.spec.js # Session management
└── 03-error-handling.spec.js    # Error scenarios
```

**Numbering Convention:**
- Prefix tests with numbers for execution order
- Lower numbers = more fundamental tests
- Tests run in alphabetical order

### Example Test: App Loads

```javascript
test.describe('App Loading', () => {
  test('should load app without errors', async ({ page }) => {
    const errors = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.goto('/');
    await waitForAppReady(page);

    expect(errors).toHaveLength(0);
  });

  test('should display login button when not authenticated', async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);

    const loginButton = page.locator('button:has-text("Login with Last.fm")');
    await expect(loginButton).toBeVisible();
  });
});
```

### Example Test: Session Lifecycle

```javascript
test.describe('Session Lifecycle', () => {
  test.skip('should start following session', async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);

    // Wait for authenticated state
    await page.waitForFunction(() =>
      window.re_frame.db.deref()['auth']['authenticated?'] === true
    );

    // Enter target username
    await page.fill('#target-username', 'testuser');

    // Click start button
    await page.click('button:has-text("Start Following")');

    // Wait for session to start
    await page.waitForFunction(() =>
      window.re_frame.db.deref()['session']['state'] === 'active'
    );

    // Verify UI updates
    await expect(page.locator('button:has-text("Pause")')).toBeVisible();
    await expect(page.locator('.session-status')).toHaveText('Active');
  });
});
```

## Test Helpers

### helpers.js

**Available helpers:**

```javascript
// Wait for app to be ready
await waitForAppReady(page);

// Check authentication status
const isAuth = await isAuthenticated(page);

// Get current session state
const state = await getSessionState(page);

// Get scrobble count
const count = await getScrobbleCount(page);

// Get recent scrobbles
const scrobbles = await getRecentScrobbles(page);

// Check if button is enabled
const enabled = await isButtonEnabled(page, buttonText);

// Wait for notification
await waitForNotification(page, message);
```

### Creating Custom Helpers

```javascript
// helpers.js
async function waitForAppReady(page) {
  // Wait for Re-frame to be available
  await page.waitForFunction(() => window.re_frame !== undefined);

  // Wait for app container
  await page.waitForSelector('#app', { timeout: 10000 });

  // Small delay for React rendering
  await page.waitForTimeout(500);
}

async function getReframeDb(page) {
  return await page.evaluate(() => {
    return window.re_frame.db.deref();
  });
}

async function isAuthenticated(page) {
  const db = await getReframeDb(page);
  return db['auth']['authenticated?'] === true;
}

module.exports = {
  waitForAppReady,
  getReframeDb,
  isAuthenticated,
};
```

### Re-frame Integration

**Accessing Re-frame state from tests:**

```javascript
// Get entire db
const db = await page.evaluate(() => window.re_frame.db.deref());

// Get specific path
const username = await page.evaluate(() => {
  const db = window.re_frame.db.deref();
  return db['auth']['username'];
});

// Dispatch event (for testing)
await page.evaluate((eventVec) => {
  window.re_frame.core.dispatch(eventVec);
}, ['my-event', { data: 'value' }]);
```

## Best Practices

### 1. Wait for Elements Properly

```javascript
// ❌ Bad: Hard-coded delays
await page.waitForTimeout(5000);

// ✅ Good: Wait for specific conditions
await page.waitForSelector('#my-element');
await page.waitForFunction(() => window.appReady === true);
await expect(page.locator('#my-element')).toBeVisible();
```

### 2. Use Descriptive Locators

```javascript
// ❌ Bad: Fragile selectors
await page.click('div > button.btn');

// ✅ Good: Semantic selectors
await page.click('button:has-text("Start Following")');
await page.click('[data-testid="start-button"]');
await page.click('[aria-label="Start following session"]');
```

### 3. Test User Flows, Not Implementation

```javascript
// ❌ Bad: Testing implementation
test('should update state', async ({ page }) => {
  await page.evaluate(() => {
    window.re_frame.db.reset({ session: { state: 'active' } });
  });
});

// ✅ Good: Testing user actions
test('should start session when user clicks button', async ({ page }) => {
  await page.fill('#target-username', 'testuser');
  await page.click('button:has-text("Start Following")');
  await expect(page.locator('.session-status')).toHaveText('Active');
});
```

### 4. Handle Asynchronous Operations

```javascript
// ✅ Wait for API calls to complete
test('should display scrobbles after polling', async ({ page }) => {
  // Start session
  await startSession(page, 'testuser');

  // Wait for first poll (20s interval + processing)
  await page.waitForFunction(
    () => window.re_frame.db.deref()['session']['scrobble_count'] > 0,
    { timeout: 30000 }
  );

  // Verify UI updated
  await expect(page.locator('.scrobble-count')).toHaveText('1');
});
```

### 5. Clean Up Between Tests

```javascript
test.beforeEach(async ({ page }) => {
  // Clear localStorage
  await page.evaluate(() => localStorage.clear());

  // Reset app state
  await page.goto('/');
  await waitForAppReady(page);
});

test.afterEach(async ({ page }) => {
  // Stop any active sessions
  const sessionActive = await page.evaluate(() => {
    const db = window.re_frame.db.deref();
    return db['session']['state'] === 'active';
  });

  if (sessionActive) {
    await page.click('button:has-text("Stop")');
  }
});
```

### 6. Use Test Fixtures

```javascript
// Setup authenticated state
test.describe('Authenticated Tests', () => {
  test.use({
    storageState: 'auth-state.json', // Saved auth state
  });

  test('should access protected features', async ({ page }) => {
    await page.goto('/');
    // Already authenticated
  });
});
```

## Debugging

### Visual Debugging

```bash
# Run with headed browser
npx playwright test --headed

# Run with UI mode (interactive)
npx playwright test --ui

# Debug specific test
npx playwright test --debug 01-app-loads
```

### Screenshots and Videos

```javascript
// Take screenshot on failure (automatic)
// Configured in playwright.config.js

// Manual screenshot
await page.screenshot({ path: 'screenshot.png' });

// Full page screenshot
await page.screenshot({ path: 'full.png', fullPage: true });
```

### Console Logs

```javascript
test('should log errors', async ({ page }) => {
  const logs = [];

  page.on('console', msg => {
    logs.push({ type: msg.type(), text: msg.text() });
  });

  await page.goto('/');

  // Check logs
  console.log('Browser logs:', logs);
});
```

### Trace Viewer

```bash
# Run with trace
npx playwright test --trace on

# View trace
npx playwright show-trace trace.zip
```

### Debugging Tips

1. **Pause execution:**
   ```javascript
   await page.pause(); // Opens Playwright Inspector
   ```

2. **Step through:**
   ```bash
   npx playwright test --debug
   ```

3. **Check element state:**
   ```javascript
   const button = page.locator('#my-button');
   console.log(await button.isVisible());
   console.log(await button.isEnabled());
   console.log(await button.textContent());
   ```

4. **Evaluate in browser:**
   ```javascript
   const result = await page.evaluate(() => {
     // Any JavaScript code
     return window.someValue;
   });
   ```

## CI Integration

### GitHub Actions Configuration

```yaml
# .github/workflows/ci.yml
e2e-tests:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
    - name: Install dependencies
      run: npm ci
    - name: Install Playwright browsers
      run: npx playwright install --with-deps chromium
    - name: Start app
      run: bb dev &
    - name: Wait for app
      run: npx wait-on http://localhost:8080
    - name: Run E2E tests
      run: npx playwright test
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: playwright-report
        path: playwright-report/
```

### Running E2E Tests in CI

**Currently:** E2E tests are run separately (not in main CI)

**Planned:** Integrate E2E tests after auth mock implementation

## Known Issues and Limitations

### Skipped Tests

**15 tests skipped pending:**
- Authentication mock implementation
- Session lifecycle tests
- Error handling tests

**To enable:**
1. Implement auth mock in test environment
2. Remove `.skip` from test definitions
3. Update test expectations

### Performance Considerations

- E2E tests are slower than unit tests (~5-30s per test)
- Run E2E tests less frequently (pre-merge, CI only)
- Use `test.describe.serial()` for dependent tests
- Consider parallelization for independent tests

### Browser Support

**Current:** Chromium only

**To add browsers:**
```javascript
// playwright.config.js
projects: [
  { name: 'chromium', use: devices['Desktop Chrome'] },
  { name: 'firefox', use: devices['Desktop Firefox'] },
  { name: 'webkit', use: devices['Desktop Safari'] },
],
```

## Next Steps

- Implement authentication mock for full E2E coverage
- Add visual regression testing
- Add mobile browser testing
- Add accessibility testing with axe-core
- Consider adding load/performance testing

## Further Reading

- [Playwright Documentation](https://playwright.dev)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [Playwright API](https://playwright.dev/docs/api/class-playwright)
- [Test Assertions](https://playwright.dev/docs/test-assertions)
