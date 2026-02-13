# E2E Tests with Playwright

End-to-end tests for the Lasso application using Playwright.

## Setup

```bash
# Install Playwright (one-time)
npm install

# Install Playwright browsers (one-time)
npx playwright install chromium
```

## Running Tests

### All Tests
```bash
# Run all E2E tests (headless)
bb test:e2e

# Or directly:
npx playwright test
```

### Interactive Mode
```bash
# Run tests with UI mode (recommended for development)
bb test:e2e:ui

# Run tests in headed mode (visible browser)
bb test:e2e:headed

# Debug specific test
bb test:e2e:debug
```

### Specific Tests
```bash
# Run a specific test file
npx playwright test test/e2e/01-app-loads.spec.js

# Run tests matching a pattern
npx playwright test --grep "should load"
```

## Test Organization

- `01-app-loads.spec.js` - Basic application loading and initial state
- `02-session-lifecycle.spec.js` - Session management (start, pause, resume, stop)
- `03-error-handling.spec.js` - Error scenarios and recovery

## Test Helpers

The `helpers.js` file provides utilities for:
- `waitForAppReady()` - Wait for Re-frame to initialize
- `getSessionState()` - Get current session state from Re-frame
- `getScrobbleCount()` - Get scrobble count
- `clearAppState()` - Reset app state between tests

## Authentication in Tests

**Note:** Most session-related tests are currently skipped because they require authentication.

To test authenticated flows, you have several options:

### Option 1: Mock Backend (Recommended for CI)
Create a test endpoint that sets a session cookie without OAuth:
```javascript
await context.addCookies([{
  name: 'lasso-session',
  value: 'test-session-id',
  domain: 'localhost',
  path: '/',
}]);
```

### Option 2: Use Test Last.fm Account
Set up a dedicated Last.fm test account and use real OAuth:
```bash
# Set environment variable
export TEST_LASTFM_TOKEN=your-test-token

# Run tests with authentication
npx playwright test --grep "@auth"
```

### Option 3: Stub Last.fm API
Use Playwright's request interception to mock Last.fm responses:
```javascript
await page.route('**/last.fm/api/**', route => {
  route.fulfill({
    status: 200,
    body: JSON.stringify({ ... })
  });
});
```

## Debugging

### View Test Report
```bash
npx playwright show-report
```

### Generate Trace
```bash
npx playwright test --trace on
npx playwright show-trace trace.zip
```

### Take Screenshots
Screenshots are automatically taken on failure and saved to `test-results/`.

### Debug with Playwright Inspector
```bash
npx playwright test --debug
```

## CI Integration

The tests are configured to:
- Run headlessly in CI
- Retry failed tests 2x
- Capture traces on first retry
- Generate HTML report

Configuration is in `playwright.config.js`.

## Writing New Tests

### Basic Structure
```javascript
const { test, expect } = require('@playwright/test');
const { waitForAppReady } = require('./helpers');

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);
  });

  test('should do something', async ({ page }) => {
    // Test logic here
    const element = page.getByRole('button', { name: /click me/i });
    await expect(element).toBeVisible();
  });
});
```

### Best Practices
1. Use semantic selectors (`getByRole`, `getByLabel`, `getByText`)
2. Wait for elements to be ready before interacting
3. Use `waitForAppReady()` after navigation
4. Clean up state between tests
5. Use descriptive test names
6. Keep tests focused and independent

## Troubleshooting

### Tests timing out
- Increase timeout in `playwright.config.js`
- Check if app is starting correctly (`bb dev` in another terminal)
- Look for errors in browser console

### App not loading
- Ensure backend and frontend are both running
- Check `http://localhost:8080` manually
- Verify shadow-cljs compilation succeeded

### Flaky tests
- Add explicit waits: `await page.waitForSelector(...)`
- Use `waitForAppReady()` after navigation
- Check for race conditions in test logic

## Resources

- [Playwright Documentation](https://playwright.dev/docs/intro)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [Selectors](https://playwright.dev/docs/selectors)
- [Assertions](https://playwright.dev/docs/test-assertions)
