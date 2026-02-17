# E2E Testing with Mock Last.fm Server

This directory contains end-to-end tests for Lasso using Playwright and a mock Last.fm server.

## Overview

The E2E test suite now includes **22 tests** covering:
- Application loading (7 tests)
- Authentication flow (2 tests)
- Session lifecycle (13 tests)
  - Session start (5 tests)
  - Session controls (5 tests)
  - Activity feed (3 tests)
- Error handling (4 tests)

All tests use a **mock Last.fm server** instead of the real API, allowing:
- ✅ Fast, reliable tests without network dependencies
- ✅ Complete control over API responses
- ✅ Testing error scenarios easily
- ✅ No rate limiting concerns

## Quick Start

### Prerequisites

```bash
# Install Playwright browsers (one-time setup)
npx playwright install
```

### Running Tests

**Local Development (Recommended):**

1. Start the backend with mock server configuration:
   ```bash
   # Copy E2E environment
   cp .env.e2e .env

   # Start development environment
   bb dev
   ```

2. In another terminal, run E2E tests:
   ```bash
   bb test:e2e
   ```

**CI/Automated:**

Tests run automatically in CI with the mock server:
```bash
# CI automatically starts both mock server and backend
npx playwright test
```

### Test Modes

```bash
# Headless mode (default, fast)
bb test:e2e

# Headed mode (see browser)
bb test:e2e:headed

# UI mode (interactive test runner)
bb test:e2e:ui

# Debug mode (step through tests)
bb test:e2e:debug
```

## Architecture

### Mock Server

The mock Last.fm server (`mocks/lastfm-mock-server.js`) provides:

**OAuth Endpoints:**
- `GET /api/auth` - Initiates OAuth flow, redirects with token
- `POST /2.0/?method=auth.getSession` - Exchanges token for session

**User API Endpoints:**
- `GET /2.0/?method=user.getInfo` - Validates username
- `GET /2.0/?method=user.getRecentTracks` - Returns mock scrobbles

**Test Utility Endpoints:**
- `POST /test/add-user` - Add custom test users
- `POST /test/add-tracks` - Add mock scrobbles
- `POST /test/reset` - Reset mock data
- `GET /health` - Health check

### Test Helpers

The `helpers.js` file provides utilities for:

**App State:**
- `waitForAppReady(page)` - Wait for Re-frame initialization
- `isAuthenticated(page)` - Check auth status
- `getSessionState(page)` - Get current session state
- `clearAppState(page)` - Reset app state between tests

**Authentication:**
- `setupAuthenticatedContext(page)` - One-line authenticated setup
- `mockLastFmAuth(page, username)` - Complete OAuth flow

**Session Management:**
- `waitForSessionState(page, state)` - Wait for specific state
- `getScrobbleCount(page)` - Get current scrobble count
- `getRecentScrobbles(page)` - Get scrobble list
- `getErrorMessage(page)` - Get current error

### Global Setup/Teardown

Playwright automatically:
1. **Before all tests**: Starts mock Last.fm server on port 3456
2. **Run tests**: Backend connects to mock server
3. **After all tests**: Stops mock server

## Writing New Tests

### Basic Test Structure

```javascript
const { test, expect } = require('@playwright/test');
const { waitForAppReady } = require('./helpers');

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await waitForAppReady(page);
  });

  test('should do something', async ({ page }) => {
    // Test code
  });
});
```

### Authenticated Test

```javascript
const { setupAuthenticatedContext } = require('./helpers');

test.describe('Authenticated Feature', () => {
  test.beforeEach(async ({ page }) => {
    // One line to get authenticated state
    await setupAuthenticatedContext(page);
  });

  test('should work when logged in', async ({ page }) => {
    // User is already authenticated
    const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
    await expect(usernameInput).toBeVisible();
  });
});
```

### Testing Session Lifecycle

```javascript
const { setupAuthenticatedContext, waitForSessionState } = require('./helpers');

test('should start and stop session', async ({ page }) => {
  await setupAuthenticatedContext(page);

  // Start session
  const usernameInput = page.getByPlaceholder(/e\.g\., johndoe/i);
  const startButton = page.getByRole('button', { name: /start following/i });
  await usernameInput.fill('targetuser');
  await startButton.click();

  // Wait for active state
  await waitForSessionState(page, 'active');

  // Verify pause button visible
  const pauseButton = page.getByRole('button', { name: /pause session/i });
  await expect(pauseButton).toBeVisible();

  // Stop session
  const stopButton = page.getByRole('button', { name: /stop session/i });
  await stopButton.click();
  const confirmButton = page.getByRole('button', { name: /yes, stop/i });
  await confirmButton.click();

  // Wait for not-started state
  await waitForSessionState(page, 'not-started');
});
```

### Custom Mock Data

```javascript
test('should handle user with many scrobbles', async ({ page, request }) => {
  // Add custom user to mock server
  await request.post('http://localhost:3456/test/add-user', {
    data: {
      username: 'poweruser',
      realname: 'Power User',
      tracks: [
        {
          artist: { '#text': 'Artist 1' },
          name: 'Track 1',
          date: { uts: Math.floor(Date.now() / 1000) - 60 }
        },
        // More tracks...
      ]
    }
  });

  // Now test with this user
  await setupAuthenticatedContext(page);
  // ...
});
```

## Test Data

### Default Test Users

The mock server provides:

**`testuser`** (authenticated user)
- Default user for authenticated sessions
- Used by `setupAuthenticatedContext()`

**`targetuser`** (target to follow)
- Has 2 mock recent tracks
- Used in session start tests

### Adding Custom Users

```bash
curl -X POST http://localhost:3456/test/add-user \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customuser",
    "realname": "Custom User",
    "tracks": [...]
  }'
```

## Troubleshooting

### Mock Server Not Starting

If tests fail with connection errors:

```bash
# Check if port 3456 is in use
lsof -i :3456

# Kill process using the port
kill -9 <PID>

# Or use different port (update playwright.config.js)
```

### Authentication Failing

If `setupAuthenticatedContext` fails:

1. Check mock server is running:
   ```bash
   curl http://localhost:3456/health
   ```

2. Check backend is using mock server:
   ```bash
   # Ensure .env has:
   LASTFM_API_BASE_URL=http://localhost:3456
   LASTFM_AUTH_URL=http://localhost:3456
   ```

3. Check browser console for errors (use headed mode):
   ```bash
   bb test:e2e:headed
   ```

### Tests Timing Out

If tests timeout waiting for state changes:

1. Increase timeout in test:
   ```javascript
   await waitForSessionState(page, 'active', 30000); // 30 seconds
   ```

2. Check polling is enabled (should be in E2E mode):
   ```bash
   # .env should have:
   POLLING_INTERVAL_MS=2000  # Fast polling for tests
   ```

3. Run in debug mode to step through:
   ```bash
   bb test:e2e:debug
   ```

## CI Integration

Tests run automatically in GitHub Actions:

**.github/workflows/ci.yml:**
```yaml
- name: Run E2E tests
  run: npx playwright test
  env:
    # Playwright config handles mock server startup
    CI: true
```

The Playwright `webServer` configuration:
- Starts mock server (global setup)
- Starts backend with mock environment
- Runs all tests
- Stops everything (global teardown)

## Test Coverage

Current coverage:

| Category | Tests | Status |
|----------|-------|--------|
| App Loading | 7 | ✅ All passing |
| Authentication | 2 | ✅ All passing |
| Session Start | 5 | ✅ All passing |
| Session Controls | 5 | ✅ All passing |
| Activity Feed | 3 | ✅ All passing |
| Error Handling | 4 | ✅ All passing |
| **Total** | **22** | **✅ 22 passing, 0 skipped** |

## Best Practices

1. **Use helpers**: Don't manually implement auth or state checks
2. **Wait for state**: Use `waitForSessionState` instead of arbitrary timeouts
3. **Reset between tests**: Use `clearAppState` or `beforeEach` to isolate tests
4. **Test user flows**: Write tests that match real user behavior
5. **Mock edge cases**: Use custom users/data to test error scenarios
6. **Check Re-frame state**: Use `getSessionState` to verify internal state
7. **Keep tests focused**: One behavior per test

## Next Steps

Potential improvements:

- [ ] Add network failure simulation tests
- [ ] Add mobile viewport tests
- [ ] Add tests for concurrent sessions
- [ ] Add visual regression testing
- [ ] Parallelize tests for faster runs
- [ ] Add performance benchmarks

## Resources

- [Playwright Documentation](https://playwright.dev/)
- [Re-frame Testing Guide](https://day8.github.io/re-frame/testing/)
- [Last.fm API Documentation](https://www.last.fm/api)
