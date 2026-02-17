/**
 * Playwright Global Setup
 *
 * Runs once before all tests to:
 * 1. Start the mock Last.fm server
 * 2. Set up environment variables for test mode
 * 3. Verify services are ready
 */

const { app } = require('./mocks/lastfm-mock-server');

const MOCK_SERVER_PORT = 3456;

async function globalSetup() {
  console.log('[Global Setup] Starting mock Last.fm server...');

  // Start mock server
  return new Promise((resolve, reject) => {
    const server = app.listen(MOCK_SERVER_PORT, () => {
      console.log(`[Global Setup] Mock Last.fm server running on http://localhost:${MOCK_SERVER_PORT}`);

      // Store server reference for teardown
      global.__MOCK_SERVER__ = server;

      // Wait a bit to ensure server is ready
      setTimeout(resolve, 1000);
    });

    server.on('error', (error) => {
      console.error('[Global Setup] Failed to start mock server:', error);
      reject(error);
    });
  });
}

module.exports = globalSetup;
