# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🚀 Quick Start for New Sessions

**Starting a fresh Claude Code session? Read these files first:**

1. **`STATUS.md`** - Current project state, what's done, what's in progress
2. **`NEXT.md`** - Exactly what to work on next with implementation details
3. **`MEMORY.md`** - Gotchas, patterns, and decisions (in `.claude/projects/.../memory/`)
4. **This file** - Full project context and architecture

**Ready to code?** Jump straight to `NEXT.md` and start with task #1.

**Session Handoff Checklist:**
- [ ] Read `STATUS.md` to understand current state
- [ ] Check `NEXT.md` for immediate next task
- [ ] Scan `MEMORY.md` for gotchas and patterns
- [ ] Ensure on `develop` branch: `git checkout develop && git pull`
- [ ] Review this file for project architecture (below)

---

## Project Overview

**Lasso** is a web application that enables Last.fm users to track their listening history during Spotify Jam sessions. When users participate in Spotify Jams as guests (non-owners), their listening activity is not scrobbled to Last.fm. Lasso solves this by allowing users to temporarily follow another Last.fm user and mirror their scrobbles in real-time.

## Technology Stack

### Backend (Clojure)
- **Framework**: Pedestal with Jetty server
- **Build**: tools.deps (deps.edn)
- **Key Libraries**:
  - clj-http for HTTP client
  - Malli for validation
  - Buddy for encryption
  - timbre for logging
  - core.async for concurrent polling

### Frontend (ClojureScript)
- **UI Framework**: Reagent (React wrapper)
- **State Management**: Re-frame
- **Build**: shadow-cljs
- **Styling**: Tailwind CSS
- **Routing**: reitit-frontend
- **HTTP**: cljs-ajax

### Infrastructure
- **Deployment**: Docker containers on Google Cloud Run
- **CI/CD**: GitHub Actions
- **Session Storage**: In-memory atom (MVP)

## Common Commands

### Development

```bash
# Start backend REPL (Terminal 1)
clj -M:dev:repl
# In REPL: (user/start) to start server

# Start frontend with hot reload (Terminal 2)
npx shadow-cljs watch app

# Watch CSS changes (Terminal 3)
npm run watch:css
# or build once:
npm run build:css

# REPL utilities
(user/start)    # Start server
(user/stop)     # Stop server
(user/restart)  # Restart server
(user/reset)    # Stop, reload namespaces, restart
```

### Testing

```bash
# Backend tests
clj -M:test

# Run specific test namespace
clj -M:test --focus lasso.auth.core-test

# Lint code
clj-kondo --lint src

# Frontend tests (when implemented)
npx shadow-cljs compile test
node target/test.js
```

### Building

```bash
# Production frontend build
npx shadow-cljs release app

# Minified CSS
npm run build:css

# Backend uberjar
clj -X:uberjar

# Docker image
docker build -t lasso:latest .

# Clean build artifacts
npm run clean
rm -rf target .cpcache
```

## Architecture

### High-Level Flow

1. **User Authentication**: Last.fm OAuth 2.0 flow
   - User initiates login → Backend generates OAuth URL → Last.fm authorization → Callback with token → Backend exchanges for session key → Server-side session created

2. **Following Session**: User-controlled scrobble mirroring
   - User specifies target Last.fm username → Backend validates target → User starts session → Backend polls target user's recent tracks (15-30s intervals) → New scrobbles identified and submitted to authenticated user's account → Frontend polls for session status updates

3. **Session States**: `not-started` → `active` → `paused` → `stopped`

### Project Structure

```
src/
├── clj/lasso/          # Backend code
│   ├── server.clj      # Entry point, server lifecycle
│   ├── config.clj      # Environment configuration
│   ├── routes.clj      # Pedestal routes
│   ├── middleware.clj  # Custom interceptors
│   ├── auth/           # OAuth implementation
│   │   ├── core.clj    # OAuth flow
│   │   └── session.clj # Session management
│   ├── lastfm/         # Last.fm API integration
│   │   ├── client.clj  # API client
│   │   ├── oauth.clj   # OAuth specific
│   │   └── scrobble.clj # Scrobble operations
│   ├── session/        # Session management
│   │   ├── store.clj   # Session storage
│   │   └── manager.clj # Session lifecycle
│   ├── polling/        # Scrobble polling engine
│   │   ├── engine.clj  # Polling orchestration
│   │   └── scheduler.clj # Scheduling logic
│   └── validation/     # Malli schemas
└── cljs/lasso/         # Frontend code
    ├── core.cljs       # App entry point
    ├── events.cljs     # Re-frame events
    ├── subs.cljs       # Re-frame subscriptions
    ├── views.cljs      # Main views
    ├── routes.cljs     # Client routing
    ├── api.cljs        # Backend API client
    └── components/     # UI components
```

### Key Architectural Decisions

1. **SPA with RESTful API**: Clear separation between frontend (ClojureScript SPA) and backend (Clojure API server). Backend handles all Last.fm integration and maintains session state.

2. **Polling-Based Updates**: Backend polls Last.fm API every 15-30 seconds during active sessions to respect rate limits (5 req/sec). Frontend polls backend for status updates.

3. **In-Memory Session Store**: Sessions stored in server-side atom for MVP. Future: migrate to Redis for multi-instance deployment.

4. **Stateless API Design**: All endpoints (except auth) require session cookie. Session data isolated per user, cleared on logout.

5. **Security**: OAuth-only (no passwords), HTTP-only secure cookies, server-side token encryption, HTTPS enforced.

## API Design

### Backend Endpoints

**Authentication:**
- `POST /api/auth/init` - Initialize OAuth flow
- `GET /api/auth/callback` - OAuth callback handler
- `POST /api/auth/logout` - Destroy session

**Session Management:**
- `POST /api/session/start` - Start following target user
- `POST /api/session/pause` - Pause active session
- `POST /api/session/resume` - Resume paused session
- `POST /api/session/stop` - Stop and clear session
- `GET /api/session/status` - Get current status + recent activity

### Last.fm API Integration

**Required Methods:**
- `auth.getToken` - Initiate OAuth
- `auth.getSession` - Exchange token for session key
- `user.getRecentTracks` - Fetch target user's scrobbles
- `user.getInfo` - Validate target username
- `track.scrobble` - Submit scrobble to authenticated user

**Rate Limiting:**
- Respect 5 requests/second limit
- Polling interval: 15-30 seconds
- Exponential backoff on errors
- Request queuing to avoid bursts

## Development Workflow

### REPL-Driven Development

The backend uses REPL-driven development:
1. Start REPL with `clj -M:dev:repl`
2. Evaluate `(user/start)` to start server
3. Make code changes
4. Reload with `(user/restart)` or `(user/reset)` for full namespace reload
5. Test functions directly in REPL

### Frontend Hot Reload

shadow-cljs provides instant feedback:
1. Edit files in `src/cljs/`
2. Save - changes appear immediately in browser
3. Re-frame state preserved across reloads
4. Check browser console for compilation errors

### Gitflow Workflow (IMPORTANT)

**Starting New Work:**
```bash
# Always branch from develop for feature work
git checkout develop
git pull origin develop
git checkout -b feature/sprint-5-frontend

# Work, commit, push
git push -u origin feature/sprint-5-frontend

# Create PR targeting develop (NOT main!)
gh pr create --base develop --title "feat(ui): Sprint 5 frontend implementation"
```

**Creating Releases:**
```bash
# Branch from develop when ready to release
git checkout develop
git checkout -b release/0.2.0

# Update VERSION and CHANGELOG, commit
echo "0.2.0" > VERSION
git commit -m "chore(release): bump version to 0.2.0"

# PR to main triggers automated release
gh pr create --base main --title "Release v0.2.0"

# After merge, sync develop with main
git checkout develop && git merge main
```

**See CONTRIBUTING.md for complete gitflow documentation.**

### Making Changes

**Adding New API Endpoint:**
1. Add route in `lasso.routes`
2. Create handler function
3. Update Re-frame events/subscriptions on frontend
4. Add API call in `lasso.api` (frontend)
5. Update Malli schemas in `lasso.validation.schemas`

**Adding New Re-frame Event:**
1. Add event handler in `lasso.events`
2. Add subscription in `lasso.subs` (if needed)
3. Update component to dispatch event
4. Test in browser with re-frame-10x DevTools

**Modifying Session State:**
1. Update schema in `lasso.validation.schemas`
2. Update session store structure in `lasso.session.store`
3. Update session manager logic in `lasso.session.manager`
4. Update frontend state shape in `lasso.events` (initialize-db)

## Testing Strategy

**Unit Tests:**
- Test core business logic functions
- Mock external API calls (Last.fm)
- Use `clojure.test` for assertions
- Use `test.check` for property-based testing where applicable

**Integration Tests:**
- Test Last.fm API integration
- Test session management lifecycle
- Test concurrent session handling

**Manual E2E Tests:**
- Complete authentication flow
- Start → Pause → Resume → Stop session flow
- Error recovery scenarios
- Mobile responsiveness

## Important Constraints

1. **Last.fm API Rate Limits**: Maximum 5 requests/second. Polling must respect this limit.

2. **OAuth Requirements**: Must use OAuth 2.0 (no password storage). Tokens encrypted server-side.

3. **Session Scope**: Only real-time scrobbles during active session. No historical backfilling.

4. **Single Target**: MVP supports following one user at a time (no multi-user following).

5. **Public Profiles Only**: Target user's profile must be public/accessible.

## Environment Variables

Required configuration (see `.env.example`):
```bash
LASTFM_API_KEY=your_api_key
LASTFM_API_SECRET=your_api_secret
OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback
SESSION_SECRET=random_secret_min_32_chars
PORT=8080
HOST=0.0.0.0
ENVIRONMENT=development
POLLING_INTERVAL_MS=20000
```

## Code Style

- Follow Clojure Style Guide
- Use `cljfmt` for formatting
- Max line length: 100 characters
- Prefer pure functions
- Document public functions with docstrings
- Namespace organization: `lasso.<domain>.<component>`

## Deployment

**Docker Build:**
```bash
docker build -t lasso:latest .
```

**Google Cloud Run Deployment:**
```bash
gcloud builds submit --tag gcr.io/PROJECT_ID/lasso
gcloud run deploy lasso --image gcr.io/PROJECT_ID/lasso --platform managed --region us-central1
```

## Current Project Status

**Current Sprint**: Sprint 5-6 (Frontend Development - Not Started)

**Completed:**
- ✅ **Sprint 2**: Development environment and project scaffolding
  - Project Charter, PRD, TDD
  - Development environment setup
  - Project scaffolding (all 25+ files)
  - Build tools configuration (deps.edn, shadow-cljs, Tailwind)
  - CI/CD pipeline (lint, build, test, Docker)
  - Autonomous PR workflow with debugging
  - Automated release workflow
  - Comprehensive documentation

- ✅ **Gitflow Setup** (2024-02-12)
  - `main` branch protected (production releases only)
  - `develop` branch created and protected (active development)
  - CI runs on PRs to both branches
  - Branch protection requires passing `lint-and-build` check
  - Automated release workflow triggered on VERSION changes to `main`

- ✅ **Sprint 3-4**: Complete Backend Implementation (Released v0.2.0 on 2026-02-12)
  - Last.fm API client with rate limiting
  - OAuth 2.0 flow implementation (init, callback, logout)
  - Session store with encryption
  - Scrobble tracking and submission
  - Auth and session management routes
  - Middleware for session authentication
  - Polling engine for real-time scrobble tracking
  - Session lifecycle management (start/pause/resume/stop)
  - Comprehensive test coverage (75 tests, 451 assertions, 0 failures)
  - Integration tests for manual testing issues
  - Fixed 7 critical bugs discovered during E2E testing
  - Complete end-to-end functionality verified

**Version:** v0.2.0 (Released 2026-02-12)

**Branching Model:**
- **`main`**: Production releases only (currently v0.2.0)
- **`develop`**: Active development (synced with main at v0.2.0)
- **Feature branches**: Created from and merged to `develop`
- **Release branches**: Created from `develop`, merged to `main` (triggers automated release)

**Next Phases:**
- Sprint 5-6: Frontend development (UI, session controls, activity feed) - NEXT
- Sprint 7: Integration & testing
- Sprint 8: Deployment
- Sprint 9: Launch

## Common Patterns

### Session Data Structure
```clojure
{:session-id "uuid"
 :username "lastfm-username"
 :session-key "encrypted-session-key"
 :created-at 1706832000000
 :last-activity 1706832300000
 :following-session {:target-username "target"
                     :state :active
                     :started-at 1706832100000
                     :last-poll 1706832280000
                     :scrobble-count 15
                     :scrobble-cache #{"Artist|Track|Timestamp"}}}
```

### Re-frame App State
```clojure
{:auth {:authenticated? false
        :username nil}
 :session {:state :not-started
           :target-username nil
           :scrobble-count 0
           :recent-scrobbles []
           :last-poll nil}
 :ui {:loading? false
      :error nil}}
```

## Autonomous PR Workflow

This project uses an **autonomous PR review and iteration process** where Claude Code independently:
1. Creates PRs after pushing feature branches
2. Monitors CI execution and downloads artifacts
3. Analyzes failures and debugs issues
4. Fixes problems and pushes updates
5. Iterates until all checks pass
6. Marks PRs ready for human review

### How It Works

**After completing feature work:**
1. Push your feature branch: `git push origin feature/name`
2. Claude creates a PR using `gh pr create`
3. CI automatically runs with enhanced debugging capabilities
4. Claude monitors using `gh pr checks` and `gh run view`
5. If failures occur:
   - Claude downloads artifacts: `gh run download`
   - Analyzes logs and lint results
   - Identifies root cause
   - Fixes code and commits
   - Pushes updates (CI re-runs automatically)
6. Once all checks pass, Claude notifies you for final review

### Enhanced CI Pipeline

**Capabilities for Autonomous Debugging:**
- ✅ Runs on PRs to `main` and `develop` branches
- ✅ Uploads build artifacts (JAR, JS, CSS, lint results)
- ✅ Generates detailed build summaries in GitHub UI
- ✅ Posts automated PR status comments
- ✅ Uploads Docker images for inspection
- ✅ Enhanced lint output with file/line annotations
- ✅ Build artifact size reporting
- ✅ Docker image layer inspection

**Available Artifacts (7-day retention):**
- `target/lasso.jar` - Backend JAR
- `resources/public/js/` - Frontend JavaScript bundle
- `resources/public/css/tailwind.css` - Compiled CSS
- `lint-results.txt` - Linting output with errors/warnings
- Docker image (3-day retention)

**GitHub CLI Commands for PR Management:**
```bash
gh pr list                     # List all PRs
gh pr view 123                 # View PR details
gh pr checks 123               # Check CI status
gh run view <run-id> --log     # View CI logs
gh run download <run-id>       # Download artifacts

# Intelligent CI waiting (recommended for autonomous workflow)
./scripts/wait-for-ci.sh 123   # Wait based on historical averages
```

**CI Duration Tracking:**
- CI workflow tracks and reports run duration
- Historical average displayed in GitHub Actions summary
- Typical run time: ~2min 15s (135 seconds)
- PR comments include duration for each run

### Expected Workflow

**Normal Case (No Issues):**
```
Push → Create PR → CI runs → All pass → Notify user → Merge
```

**Debug Case (CI Failures):**
```
Push → Create PR → CI runs → Failures detected
   ↓
Download artifacts (lint-results.txt, build logs)
   ↓
Analyze root cause (linting errors, compilation failures, etc.)
   ↓
Fix code + commit + push
   ↓
CI re-runs automatically → Check status
   ↓
Repeat until all checks pass → Notify user → Merge
```

### Escalation Criteria

Claude escalates to human review when:
- Repeated failures after 3 fix attempts
- Fundamental design decisions needed
- External dependency issues (npm/clojars outages)
- Infrastructure problems (GitHub Actions failures)
- Security vulnerabilities requiring judgment
- Breaking changes requiring approval

### Claude Code Review (On-Demand)

For detailed AI code review on important PRs:

1. **Add `claude-review` label** to your PR on GitHub
2. Claude Code Review workflow runs automatically
3. Review posted as PR comment with detailed feedback
4. Label auto-removed to prevent re-runs

**Note:** Only use for important/complex PRs to conserve API tokens. Does NOT run on every commit.

### Automated Releases

Releases are **fully automated** - no manual tagging required!

**How it works:**
1. Update `VERSION` file with new version (e.g., `0.2.0`)
2. Update `CHANGELOG.md` with changes for that version
3. Commit with message: `chore(release): bump version to X.Y.Z`
4. Create PR and merge to `main`
5. GitHub Actions automatically:
   - Creates git tag `vX.Y.Z`
   - Extracts changelog entry
   - Creates GitHub release
   - Posts summary to workflow run

**Semantic Versioning:**
- `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)
- Increment MAJOR for breaking changes
- Increment MINOR for new features (backwards compatible)
- Increment PATCH for bug fixes (backwards compatible)

**Workflow:** `.github/workflows/release.yml`
- Triggers on VERSION file changes to main
- Checks if tag already exists (prevents duplicates)
- Safe to re-run (idempotent)

See `CONTRIBUTING.md` for detailed release process.

### Documentation

For complete autonomous workflow documentation, see:
- `docs/development/AUTONOMOUS_PR_WORKFLOW.md` - Detailed process guide
- `.github/workflows/ci.yml` - CI pipeline configuration
- `.github/workflows/claude-review.yml` - On-demand code review
- `.github/workflows/release.yml` - Automated release and tagging
- `CONTRIBUTING.md` - Git workflow, branching strategy, and release process

## Troubleshooting

**REPL won't start:** Clear `.cpcache` and run `clojure -P` to re-download dependencies

**shadow-cljs build fails:** Clear cache with `rm -rf .shadow-cljs` and `npx shadow-cljs clean`

**Hot reload not working:** Hard refresh browser (Cmd+Shift+R), check shadow-cljs output for compilation errors

**Port 8080 in use:** Find process with `lsof -i :8080` and kill it

**Tailwind classes not applied:** Rebuild CSS with `npm run build:css`, check `tailwind.config.js` content paths

**CI failing on PR:** Run `gh pr checks <pr-number>` to see status, `gh run view <run-id> --log` for detailed logs, `gh run download <run-id>` to inspect artifacts
