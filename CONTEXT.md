# AI Context - Lasso Project

**Universal context file for all LLM tools (Claude Code, Cursor, Copilot, etc.)**

---

## 🚀 Quick Start (For Any AI Tool)

**New session? Read these 3 files:**

1. **`STATUS.md`** → Current project state
2. **`NEXT.md`** → What to work on next
3. **`MEMORY.md`** → Gotchas and patterns

**Then read this file for full project context.**

---

## Project Overview

**Lasso** is a web application that enables Last.fm users to track their listening history during Spotify Jam sessions.

**Problem:** When you join a Spotify Jam as a guest (not the owner), your listening activity isn't scrobbled to Last.fm.

**Solution:** Lasso lets you temporarily "follow" another Last.fm user and mirror their scrobbles in real-time during the Jam session.

---

## Technology Stack

**Backend:**
- Clojure with Pedestal web framework
- Jetty server
- tools.deps for dependency management

**Frontend:**
- ClojureScript with Reagent (React wrapper)
- Re-frame for state management
- shadow-cljs for build
- Tailwind CSS for styling

**Infrastructure:**
- Docker containers
- Google Cloud Run (target deployment)
- GitHub Actions for CI/CD

**Key Libraries:**
- clj-http (HTTP client)
- Malli (validation)
- Buddy (encryption)
- timbre (logging)
- core.async (concurrent polling)

---

## Architecture

### High-Level Flow

```
User → OAuth Login → Last.fm
  ↓
User specifies target username
  ↓
Backend polls target's recent tracks (every 15-30s)
  ↓
Identifies new scrobbles
  ↓
Submits to authenticated user's Last.fm account
```

### Session States
```
not-started → active → paused → stopped
```

### Project Structure

```
src/
├── clj/lasso/              # Backend
│   ├── server.clj          # Entry point
│   ├── config.clj          # Environment config
│   ├── routes.clj          # API routes
│   ├── middleware.clj      # Interceptors
│   ├── auth/               # OAuth & sessions
│   ├── lastfm/             # Last.fm API integration
│   ├── session/            # Session management
│   ├── polling/            # Scrobble polling engine
│   └── validation/         # Malli schemas
│
└── cljs/lasso/             # Frontend
    ├── core.cljs           # App entry
    ├── events.cljs         # Re-frame events
    ├── subs.cljs           # Re-frame subscriptions
    ├── views.cljs          # UI components
    ├── routes.cljs         # Client routing
    └── api.cljs            # Backend API client
```

---

## API Design

### Authentication Routes
- `POST /api/auth/init` - Start OAuth flow
- `GET /api/auth/callback` - OAuth callback
- `POST /api/auth/logout` - End session

### Session Management Routes
- `POST /api/session/start` - Start following target user
- `POST /api/session/pause` - Pause active session
- `POST /api/session/resume` - Resume session
- `POST /api/session/stop` - Stop session
- `GET /api/session/status` - Get current status

---

## Development Commands

### Backend
```bash
clj -M:dev:repl              # Start REPL
# In REPL:
(user/start)                 # Start server
(user/stop)                  # Stop server
(user/restart)               # Restart
```

### Frontend
```bash
npx shadow-cljs watch app    # Hot reload
npm run watch:css            # Watch Tailwind
npm run build:css            # Build CSS once
```

### Testing
```bash
clj -M:test                  # Run all tests
clj -M:test --focus ns       # Run specific namespace
clj-kondo --lint src         # Lint code
```

### CI Helper
```bash
./scripts/wait-for-ci.sh <pr-number>  # Intelligent CI wait
```

---

## Git Workflow (Gitflow)

**Branches:**
- `main` - Production releases only
- `develop` - Active development (default branch)

**Feature Development:**
```bash
git checkout develop
git pull origin develop
git checkout -b feature/my-feature

# Work, commit, test
git push -u origin feature/my-feature

# Create PR to develop (NOT main!)
gh pr create --base develop
```

**Releases:**
```bash
git checkout -b release/0.2.0
# Update VERSION and CHANGELOG
git commit -m "chore(release): bump version to 0.2.0"

# PR to main triggers automated release
gh pr create --base main
```

---

## Important Constraints

1. **Last.fm Rate Limits:** Max 5 requests/second
2. **OAuth Only:** No password storage
3. **Session Scope:** Real-time only (no historical backfill)
4. **Single Target:** Follow one user at a time (MVP)
5. **Public Profiles:** Target user must have public Last.fm profile

---

## Environment Variables

Required in `.env` file (never commit this file!):

```bash
LASTFM_API_KEY=...           # From Last.fm API account
LASTFM_API_SECRET=...        # From Last.fm API account
OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback
SESSION_SECRET=...           # Min 32 random chars
PORT=8080
HOST=0.0.0.0
ENVIRONMENT=development
POLLING_INTERVAL_MS=20000
```

---

## Code Style

- Follow Clojure Style Guide
- Use `cljfmt` for formatting
- Max line length: 100 characters
- Write docstrings for public functions
- Namespace format: `lasso.<domain>.<component>`

---

## Testing Strategy

- Unit tests for business logic
- Mock external APIs (Last.fm)
- Integration tests for API flows
- Use `with-redefs` for mocking
- Test files mirror source structure

---

## Current Status

See `STATUS.md` for detailed current state.

**Quick Summary:**
- **Sprint:** 3-4 (Backend Development)
- **Version:** v0.1.0 in production
- **Next Task:** See `NEXT.md`

---

## Key Decisions & Patterns

See `MEMORY.md` for full details.

**Highlights:**
- Using atoms for session storage (MVP)
- Client-side rate limiting
- OAuth 2.0 only (no passwords)
- 15-30 second polling interval
- Gitflow branching model

---

## Common Gotchas

See `MEMORY.md` for complete list.

**Top 3:**
1. Always create PRs to `develop`, not `main`
2. Use `./scripts/wait-for-ci.sh` instead of polling
3. Mock time-sensitive functions in tests

---

## External Resources

- **Last.fm API Docs:** https://www.last.fm/api
- **Pedestal Docs:** https://pedestal.io
- **Re-frame Docs:** https://day8.github.io/re-frame/
- **Project Docs:** See `docs/` directory

---

## Tool-Specific Notes

### For Claude Code
See `CLAUDE.md` for Claude Code-specific guidance.

### For Cursor
Project uses Gitflow - always branch from `develop`.

### For GitHub Copilot
See inline comments and docstrings for context.

### For Any Tool
The 3-file system (`STATUS.md`, `NEXT.md`, `MEMORY.md`) provides all context needed to start working immediately.

---

**Last Updated:** 2026-02-12
