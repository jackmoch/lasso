/**
 * E2E Test Helpers for Lasso
 *
 * Provides utilities for testing the full application stack
 */

/**
 * Wait for Re-frame to initialize and app to be ready
 * @param {import('@playwright/test').Page} page
 */
async function waitForAppReady(page) {
  // Wait for #app div to be attached (exists in DOM)
  await page.waitForSelector('#app', { state: 'attached', timeout: 15000 });
  // Wait for React to mount content inside #app
  await page.waitForFunction(
    () => {
      const app = document.querySelector('#app');
      return app && app.children.length > 0;
    },
    { timeout: 15000 }
  );

  // Give it a moment for Re-frame events to process
  await page.waitForTimeout(500);
}

/**
 * Check if user is authenticated by inspecting Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<boolean>}
 */
async function isAuthenticated(page) {
  return page.evaluate(() => {
    try {
      if (typeof re_frame === 'undefined' || !re_frame.db || !re_frame.db.app_db) return false;
      const db = cljs.core.deref(re_frame.db.app_db);
      const auth = cljs.core.get(db, cljs.core.keyword('auth'));
      const authenticatedKw = cljs.core.keyword('authenticated?');
      return cljs.core.get(auth, authenticatedKw) === true;
    } catch(e) { return false; }
  });
}

/**
 * Get current session state from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string>} - "not-started", "active", "paused", or null
 */
async function getSessionState(page) {
  return page.evaluate(() => {
    try {
      if (typeof re_frame === 'undefined' || !re_frame.db || !re_frame.db.app_db) return null;
      const db = cljs.core.deref(re_frame.db.app_db);
      const session = cljs.core.get(db, cljs.core.keyword('session'));
      const state = cljs.core.get(session, cljs.core.keyword('state'));
      if (state && state.fqn) return state.fqn;
      if (state && state.name) return state.name;
      return state ? String(state) : null;
    } catch(e) { return null; }
  });
}

/**
 * Get scrobble count from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<number>}
 */
async function getScrobbleCount(page) {
  return page.evaluate(() => {
    try {
      if (typeof re_frame === 'undefined' || !re_frame.db || !re_frame.db.app_db) return 0;
      const db = cljs.core.deref(re_frame.db.app_db);
      const session = cljs.core.get(db, cljs.core.keyword('session'));
      return cljs.core.get(session, cljs.core.keyword('scrobble-count')) || 0;
    } catch(e) { return 0; }
  });
}

/**
 * Get recent scrobbles from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<Array>}
 */
async function getRecentScrobbles(page) {
  return page.evaluate(() => {
    try {
      if (typeof re_frame === 'undefined' || !re_frame.db || !re_frame.db.app_db) return [];
      const db = cljs.core.deref(re_frame.db.app_db);
      const session = cljs.core.get(db, cljs.core.keyword('session'));
      const scrobbles = cljs.core.get(session, cljs.core.keyword('recent-scrobbles'));
      return scrobbles ? cljs.core.clj__GT_js(scrobbles) : [];
    } catch(e) { return []; }
  });
}

/**
 * Get current error message from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string|null>}
 */
async function getErrorMessage(page) {
  return page.evaluate(() => {
    try {
      if (typeof re_frame === 'undefined' || !re_frame.db || !re_frame.db.app_db) return null;
      const db = cljs.core.deref(re_frame.db.app_db);
      const ui = cljs.core.get(db, cljs.core.keyword('ui'));
      return cljs.core.get(ui, cljs.core.keyword('error')) || null;
    } catch(e) { return null; }
  });
}

/**
 * Authenticate user via mock OAuth flow
 *
 * This simulates the complete Last.fm OAuth flow by:
 * 1. Clicking the login button
 * 2. Following the redirect to mock Last.fm
 * 3. Completing the OAuth callback
 * 4. Waiting for authentication to complete
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} username - Mock username (default: 'testuser')
 * @returns {Promise<void>}
 */
async function mockLastFmAuth(page, username = 'testuser') {
  // Listen for the auth callback request completing (full redirect chain)
  const callbackComplete = page.waitForResponse(
    resp => resp.url().includes('/api/auth/callback') && resp.status() === 302,
    { timeout: 20000 }
  );

  // Click login button (triggers /api/auth/init -> redirect to mock -> redirect to callback)
  const loginButton = page.getByRole('button', { name: /login with last\.fm/i });
  await loginButton.click();

  // Wait for callback to complete
  await callbackComplete;

  // Wait for Re-frame to process the auth state after redirect back to /
  await page.waitForTimeout(1500);

  // Verify authentication succeeded
  const authenticated = await isAuthenticated(page);
  if (!authenticated) {
    throw new Error('Authentication failed - user not authenticated after OAuth flow');
  }
}

/**
 * Set up an authenticated context for testing
 * Use this in beforeEach hooks to start with an authenticated state
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} username - Mock username
 * @returns {Promise<void>}
 */
async function setupAuthenticatedContext(page, username = 'testuser') {
  await page.goto('/');
  await waitForAppReady(page);
  await mockLastFmAuth(page, username);
}

/**
 * Wait for a specific session state
 * @param {import('@playwright/test').Page} page
 * @param {string} expectedState
 * @param {number} timeout - Timeout in milliseconds
 */
async function waitForSessionState(page, expectedState, timeout = 10000) {
  const startTime = Date.now();
  while (Date.now() - startTime < timeout) {
    const currentState = await getSessionState(page);
    if (currentState === expectedState) {
      return true;
    }
    await page.waitForTimeout(500);
  }
  throw new Error(`Timeout waiting for session state: ${expectedState}`);
}

/**
 * Clear all application state (useful between tests)
 * @param {import('@playwright/test').Page} page
 */
async function clearAppState(page) {
  await page.evaluate(() => {
    // Clear localStorage
    localStorage.clear();

    // Clear sessionStorage
    sessionStorage.clear();

    // Clear cookies
    document.cookie.split(";").forEach((c) => {
      document.cookie = c
        .replace(/^ +/, "")
        .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/");
    });
  });

  // Reload to reset Re-frame state
  await page.reload();
  await waitForAppReady(page);
}

module.exports = {
  waitForAppReady,
  isAuthenticated,
  getSessionState,
  getScrobbleCount,
  getRecentScrobbles,
  getErrorMessage,
  mockLastFmAuth,
  setupAuthenticatedContext,
  waitForSessionState,
  clearAppState,
};
