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
  // Wait for Re-frame to be defined
  await page.waitForFunction(() => window.re_frame !== undefined);

  // Wait for main app container to be present
  await page.waitForSelector('#app', { timeout: 10000 });

  // Give it a moment to render initial state
  await page.waitForTimeout(500);
}

/**
 * Check if user is authenticated by inspecting Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<boolean>}
 */
async function isAuthenticated(page) {
  return page.evaluate(() => {
    if (!window.re_frame || !window.re_frame.db) return false;
    const db = window.re_frame.db.app_db.cljs$core$IDeref$_deref$arity$1();
    return db?.auth?.authenticated === true;
  });
}

/**
 * Get current session state from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string>} - "not-started", "active", "paused", or null
 */
async function getSessionState(page) {
  return page.evaluate(() => {
    if (!window.re_frame || !window.re_frame.db) return null;
    const db = window.re_frame.db.app_db.cljs$core$IDeref$_deref$arity$1();
    const state = db?.session?.state;
    // Convert keyword to string
    if (state && typeof state === 'object' && state.name) {
      return state.name;
    }
    return state ? String(state) : null;
  });
}

/**
 * Get scrobble count from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<number>}
 */
async function getScrobbleCount(page) {
  return page.evaluate(() => {
    if (!window.re_frame || !window.re_frame.db) return 0;
    const db = window.re_frame.db.app_db.cljs$core$IDeref$_deref$arity$1();
    return db?.session?.scrobble_count || 0;
  });
}

/**
 * Get recent scrobbles from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<Array>}
 */
async function getRecentScrobbles(page) {
  return page.evaluate(() => {
    if (!window.re_frame || !window.re_frame.db) return [];
    const db = window.re_frame.db.app_db.cljs$core$IDeref$_deref$arity$1();
    return db?.session?.recent_scrobbles || [];
  });
}

/**
 * Get current error message from Re-frame db
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string|null>}
 */
async function getErrorMessage(page) {
  return page.evaluate(() => {
    if (!window.re_frame || !window.re_frame.db) return null;
    const db = window.re_frame.db.app_db.cljs$core$IDeref$_deref$arity$1();
    return db?.ui?.error || null;
  });
}

/**
 * Mock Last.fm OAuth callback
 * This simulates a successful OAuth flow by directly setting session cookie
 * @param {import('@playwright/test').Page} page
 * @param {string} username - Mock username
 */
async function mockLastFmAuth(page, username = 'testuser') {
  // This would need to be implemented based on your session structure
  // For now, this is a placeholder showing the concept
  await page.evaluate((user) => {
    // Set a mock session in localStorage or dispatch a mock event
    console.log('Mock auth for:', user);
  }, username);
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
  waitForSessionState,
  clearAppState,
};
