# Project Status

**Last Updated:** 2026-02-12
**Current Sprint:** Sprint 3-4 (Backend Development)
**Project Phase:** Alpha Development (Pre-Launch)

---

## Quick Status

- **Version:** v0.1.0 (released 2024-02-11)
- **Main Branch:** Production-ready, deployed
- **Develop Branch:** 2 commits ahead (gitflow setup, CI improvements)
- **Active Work:** Sprint 3-4 Phase 4-6 (OAuth routes, polling engine)
- **Blockers:** None
- **Next Milestone:** v0.2.0 (Sprint 3-4 completion)

---

## What's Been Completed

### Sprint 2: Scaffolding & Infrastructure ✅
- [x] Project structure and dependencies
- [x] CI/CD pipeline (GitHub Actions)
- [x] Docker build configuration
- [x] Development environment setup
- [x] Automated release workflow
- [x] Project documentation (CLAUDE.md, CONTRIBUTING.md)

### Gitflow Setup (2026-02-12) ✅
- [x] `develop` branch created and protected
- [x] Branch protection rules on `main` and `develop`
- [x] CI requires passing `lint-and-build` check
- [x] Documentation updated for gitflow workflow

### CI Improvements (2026-02-12) ✅
- [x] Fixed duplicate CI runs (removed `pull_request_target`)
- [x] Added duration tracking to CI workflow
- [x] Created `scripts/wait-for-ci.sh` for intelligent waiting
- [x] PR comments now include run duration

### Sprint 3-4 Phase 1-3: Backend Foundation ✅
- [x] Last.fm API client with rate limiting (`src/clj/lasso/lastfm/client.clj`)
- [x] OAuth 2.0 implementation (`src/clj/lasso/lastfm/oauth.clj`)
- [x] Session store with encryption (`src/clj/lasso/session/store.clj`, `src/clj/lasso/util/crypto.clj`)
- [x] Scrobble tracking logic (`src/clj/lasso/lastfm/scrobble.clj`)
- [x] HTTP utilities (`src/clj/lasso/util/http.clj`)
- [x] Validation schemas (`src/clj/lasso/validation/schemas.clj`)
- [x] Comprehensive test coverage (44 tests, 205 assertions)
- [x] Fixed flaky rate-limiting test

**Files Added (Phase 1-3):**
```
src/clj/lasso/
├── auth/session.clj          ✅ Session management
├── lastfm/
│   ├── client.clj             ✅ API client with rate limiting
│   ├── oauth.clj              ✅ OAuth flow
│   └── scrobble.clj           ✅ Scrobble operations
├── session/store.clj          ✅ Session storage
├── util/
│   ├── crypto.clj             ✅ Encryption utilities
│   └── http.clj               ✅ HTTP utilities
└── validation/schemas.clj     ✅ Malli schemas

test/clj/lasso/               ✅ Full test coverage
```

---

## What's In Progress

### Sprint 3-4 Phase 4-6: Routes & Polling ⏳

**Not Yet Started:**
- [ ] OAuth routes (`src/clj/lasso/routes.clj`)
  - `/api/auth/init` - Initialize OAuth flow
  - `/api/auth/callback` - OAuth callback handler
  - `/api/auth/logout` - Destroy session

- [ ] Session management routes
  - `/api/session/start` - Start following target user
  - `/api/session/pause` - Pause active session
  - `/api/session/resume` - Resume paused session
  - `/api/session/stop` - Stop and clear session
  - `/api/session/status` - Get current status

- [ ] Polling engine (`src/clj/lasso/polling/engine.clj`)
  - Poll target user's recent tracks every 15-30s
  - Identify new scrobbles
  - Submit to authenticated user's account
  - Handle rate limiting and errors

- [ ] Polling scheduler (`src/clj/lasso/polling/scheduler.clj`)
  - Manage polling intervals
  - Start/stop/pause session polling
  - Concurrent session handling

- [ ] Session manager (`src/clj/lasso/session/manager.clj`)
  - Session lifecycle management
  - State transitions (not-started → active → paused → stopped)

**See:** `docs/tasks/sprint-3-4-implementation-plan.md` for detailed breakdown

---

## What's Next

**Immediate Next Task:** Sprint 3-4 Phase 4 - API Routes

1. Implement OAuth routes in `src/clj/lasso/routes.clj`
2. Wire up to existing OAuth client from Phase 1-3
3. Add route tests
4. Test full OAuth flow end-to-end

**After That:** Phase 5-6 - Polling Engine

**See:** `NEXT.md` for detailed next steps

---

## Key Metrics

- **Test Coverage:** 44 tests, 205 assertions, 100% passing
- **CI Duration:** ~2min 15s average (tracked since 2026-02-12)
- **Code Quality:** All linting passes, no warnings
- **Docker Build:** Working, ~150MB image

---

## Branch Status

```
main (v0.1.0)
  └─ Sprint 2 scaffolding
  └─ Automated release workflow

develop (ahead by 2 commits)
  └─ Gitflow setup + documentation
  └─ CI duration tracking + wait script
  └─ Sprint 3-4 Phase 1-3 (merged to develop)
```

**Workflow:**
- Feature branches → `develop`
- Release branches → `main` (triggers automated release)

---

## Decisions Made

1. **Gitflow Model:** Using `main` for releases, `develop` for integration
2. **In-Memory Sessions:** Using atoms for MVP (will migrate to Redis later)
3. **Polling Interval:** 15-30 seconds to respect Last.fm rate limits
4. **Rate Limiting:** Client-side with exponential backoff
5. **Security:** OAuth-only (no passwords), encrypted session keys
6. **CI Strategy:** Single workflow, runs on both `main` and `develop` PRs

See `MEMORY.md` for more context on decisions and gotchas.

---

## Environment

- **Platform:** macOS (Darwin 23.6.0)
- **Backend:** Clojure with Pedestal + Jetty
- **Frontend:** ClojureScript with Reagent + Re-frame
- **Build:** tools.deps, shadow-cljs, Tailwind CSS
- **Deployment:** Docker on Google Cloud Run (planned)

---

## Quick Commands

```bash
# Development
clj -M:dev:repl              # Start backend REPL
npx shadow-cljs watch app    # Start frontend hot reload
npm run watch:css            # Watch CSS changes

# Testing
clj -M:test                  # Run all tests
./scripts/wait-for-ci.sh 7   # Wait for PR CI

# Git (Gitflow)
git checkout develop         # Work from develop
git checkout -b feature/X    # Create feature branch
gh pr create --base develop  # PR to develop (NOT main!)
```

---

## For New Sessions

👋 **Starting a new Claude Code session?**

1. Read `NEXT.md` for immediate next steps
2. Check `MEMORY.md` for gotchas and patterns
3. Review this file (STATUS.md) for current state
4. See `CLAUDE.md` for full project context

**Ready to code?** Jump to `NEXT.md` and start with the top task!
