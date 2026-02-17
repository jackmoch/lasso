/**
 * Mock Last.fm API Server for E2E Testing
 *
 * Provides a lightweight mock of Last.fm API endpoints needed for testing:
 * - OAuth flow (Web Authentication)
 * - User info validation
 * - Recent tracks retrieval
 *
 * This server runs on port 3456 during E2E tests and is automatically
 * configured via environment variables in the test backend.
 */

const express = require('express');
const crypto = require('crypto');

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Mock data store
const mockStore = {
  tokens: new Map(), // token -> { username, timestamp }
  sessions: new Map(), // username -> sessionKey
  users: new Map([
    // Pre-configured test users
    ['testuser', {
      name: 'testuser',
      realname: 'Test User',
      playcount: 1000,
      registered: { unixtime: '1234567890' }
    }],
    ['targetuser', {
      name: 'targetuser',
      realname: 'Target User',
      playcount: 5000,
      registered: { unixtime: '1234567890' }
    }],
  ]),
  recentTracks: new Map([
    // targetuser's mock recent tracks
    ['targetuser', [
      {
        artist: { '#text': 'The Beatles' },
        name: 'Hey Jude',
        album: { '#text': '1' },
        date: { uts: Math.floor(Date.now() / 1000) - 60 }
      },
      {
        artist: { '#text': 'Pink Floyd' },
        name: 'Comfortably Numb',
        album: { '#text': 'The Wall' },
        date: { uts: Math.floor(Date.now() / 1000) - 300 }
      },
    ]],
  ])
};

/**
 * Simulate Last.fm Web Authentication Flow
 *
 * In the real flow:
 * 1. App redirects to /api/auth with api_key and callback
 * 2. User authorizes on Last.fm
 * 3. Last.fm generates a token and redirects to callback with token
 * 4. App exchanges token for session key via auth.getSession
 */
app.get('/api/auth', (req, res) => {
  const { api_key, cb } = req.query;

  if (!api_key) {
    return res.status(400).send('Missing api_key');
  }

  if (!cb) {
    return res.status(400).send('Missing callback URL');
  }

  // Generate a mock token
  const token = crypto.randomBytes(16).toString('hex');

  // Store token with default test user
  mockStore.tokens.set(token, {
    username: 'testuser',
    timestamp: Date.now()
  });

  // Redirect to callback with token (mimics Last.fm behavior)
  const callbackUrl = new URL(cb);
  callbackUrl.searchParams.set('token', token);
  res.redirect(callbackUrl.toString());
});

/**
 * Last.fm API endpoint handler
 *
 * Handles all Last.fm API method calls via query params
 */
app.get('/2.0/', (req, res) => {
  const method = req.query.method;
  const format = req.query.format || 'json';

  if (format !== 'json') {
    return res.status(400).json({
      error: 1,
      message: 'Only JSON format is supported'
    });
  }

  switch (method) {
    case 'user.getInfo':
      return handleUserGetInfo(req, res);

    case 'user.getRecentTracks':
      return handleUserGetRecentTracks(req, res);

    default:
      return res.status(400).json({
        error: 3,
        message: `Unknown method: ${method}`
      });
  }
});

/**
 * Handle POST requests to Last.fm API
 * (used for signed requests like auth.getSession)
 */
app.post('/2.0/', (req, res) => {
  const method = req.body.method;

  switch (method) {
    case 'auth.getSession':
      return handleAuthGetSession(req, res);

    default:
      return res.status(400).json({
        error: 3,
        message: `Unknown method: ${method}`
      });
  }
});

/**
 * user.getInfo - Validate username exists
 */
function handleUserGetInfo(req, res) {
  const username = req.query.user;

  if (!username) {
    return res.status(400).json({
      error: 6,
      message: 'User parameter missing'
    });
  }

  const user = mockStore.users.get(username);

  if (!user) {
    return res.status(404).json({
      error: 6,
      message: 'User not found'
    });
  }

  res.json({
    user: user
  });
}

/**
 * user.getRecentTracks - Get user's recent listening history
 */
function handleUserGetRecentTracks(req, res) {
  const username = req.query.user;
  const limit = parseInt(req.query.limit) || 10;

  if (!username) {
    return res.status(400).json({
      error: 6,
      message: 'User parameter missing'
    });
  }

  const user = mockStore.users.get(username);
  if (!user) {
    return res.status(404).json({
      error: 6,
      message: 'User not found'
    });
  }

  const tracks = mockStore.recentTracks.get(username) || [];

  res.json({
    recenttracks: {
      track: tracks.slice(0, limit),
      '@attr': {
        user: username,
        page: '1',
        perPage: String(limit),
        totalPages: '1',
        total: String(tracks.length)
      }
    }
  });
}

/**
 * auth.getSession - Exchange token for session key
 */
function handleAuthGetSession(req, res) {
  const token = req.body.token;

  if (!token) {
    return res.status(400).json({
      error: 4,
      message: 'Invalid parameters - no token supplied'
    });
  }

  const tokenData = mockStore.tokens.get(token);

  if (!tokenData) {
    return res.status(401).json({
      error: 14,
      message: 'Invalid token'
    });
  }

  // Check token age (tokens expire after 60 seconds in Last.fm)
  const tokenAge = Date.now() - tokenData.timestamp;
  if (tokenAge > 60000) {
    mockStore.tokens.delete(token);
    return res.status(401).json({
      error: 14,
      message: 'Token has expired'
    });
  }

  // Generate session key
  const sessionKey = crypto.randomBytes(16).toString('hex');
  mockStore.sessions.set(tokenData.username, sessionKey);

  res.json({
    session: {
      name: tokenData.username,
      key: sessionKey,
      subscriber: 0
    }
  });
}

/**
 * Test utility endpoint - Add custom user for testing
 * POST /test/add-user
 */
app.post('/test/add-user', (req, res) => {
  const { username, realname, tracks } = req.body;

  if (!username) {
    return res.status(400).json({ error: 'Username required' });
  }

  mockStore.users.set(username, {
    name: username,
    realname: realname || username,
    playcount: 100,
    registered: { unixtime: String(Math.floor(Date.now() / 1000)) }
  });

  if (tracks && Array.isArray(tracks)) {
    mockStore.recentTracks.set(username, tracks);
  }

  res.json({ success: true, username });
});

/**
 * Test utility endpoint - Add tracks to user
 * POST /test/add-tracks
 */
app.post('/test/add-tracks', (req, res) => {
  const { username, tracks } = req.body;

  if (!username || !tracks) {
    return res.status(400).json({ error: 'Username and tracks required' });
  }

  const user = mockStore.users.get(username);
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  const currentTracks = mockStore.recentTracks.get(username) || [];
  mockStore.recentTracks.set(username, [...tracks, ...currentTracks]);

  res.json({ success: true, trackCount: tracks.length });
});

/**
 * Test utility endpoint - Reset mock data
 * POST /test/reset
 */
app.post('/test/reset', (req, res) => {
  mockStore.tokens.clear();
  mockStore.sessions.clear();
  mockStore.recentTracks.clear();

  // Reset to default test users
  mockStore.users.clear();
  mockStore.users.set('testuser', {
    name: 'testuser',
    realname: 'Test User',
    playcount: 1000,
    registered: { unixtime: '1234567890' }
  });
  mockStore.users.set('targetuser', {
    name: 'targetuser',
    realname: 'Target User',
    playcount: 5000,
    registered: { unixtime: '1234567890' }
  });

  res.json({ success: true, message: 'Mock data reset' });
});

/**
 * Health check endpoint
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'lastfm-mock-server',
    uptime: process.uptime()
  });
});

// Start server if run directly
if (require.main === module) {
  const PORT = process.env.MOCK_LASTFM_PORT || 3456;
  app.listen(PORT, () => {
    console.log(`Mock Last.fm server running on http://localhost:${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health`);
  });
}

module.exports = { app, mockStore };
