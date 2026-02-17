/**
 * Playwright Global Teardown
 *
 * Runs once after all tests complete to:
 * 1. Stop the mock Last.fm server
 * 2. Clean up resources
 */

async function globalTeardown() {
  console.log('[Global Teardown] Stopping mock Last.fm server...');

  if (global.__MOCK_SERVER__) {
    await new Promise((resolve) => {
      global.__MOCK_SERVER__.close(() => {
        console.log('[Global Teardown] Mock server stopped');
        resolve();
      });
    });
  }
}

module.exports = globalTeardown;
