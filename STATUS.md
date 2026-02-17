# Project Status

**Last Updated:** 2026-02-17
**Current Sprint:** Sprint 9 (Launch Preparation - Not Started)
**Project Phase:** Alpha Development (Pre-Launch)

---

## Quick Status

- **Version:** v0.5.0 (Sprint 8 completed 2026-02-17)
- **Main Branch:** Production-ready v0.5.0
- **Develop Branch:** v0.5.0 (Sprint 8 merged)
- **Active Work:** Ready to begin Sprint 9
- **Blockers:** None
- **Next Milestone:** Sprint 9: Launch

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
- [x] CI requires passing `validate` check
- [x] Documentation updated for gitflow workflow

### Sprint 3-4: Complete Backend Implementation ✅ (v0.2.0)

**Phase 1-3: Backend Foundation**
- [x] Last.fm API client with rate limiting
- [x] OAuth 2.0 implementation
- [x] Session store with encryption
- [x] Scrobble tracking logic
- [x] HTTP utilities
- [x] Validation schemas

**Phase 4-6: Routes & Polling**
- [x] OAuth routes (init, callback, logout)
- [x] Session management routes (start, pause, resume, stop, status)
- [x] Authentication middleware
- [x] Polling engine for real-time scrobble tracking
- [x] Polling scheduler with core.async
- [x] Session lifecycle manager

**Bug Fixes & Testing (v0.2.0)**
- [x] Fixed handler return values (removed incorrect wrapper)
- [x] Fixed middleware session attachment
- [x] Fixed JSON body parsing from InputStreams
- [x] Fixed HTTP method selection (GET vs POST)
- [x] Fixed scrobble response parsing
- [x] Fixed environment configuration loading
- [x] Comprehensive integration tests (75 tests, 451 assertions)
- [x] Manual E2E testing completed successfully

### Sprint 5-6: Frontend Development ✅ (v0.3.0)

**Implementation:**
- [x] Complete Re-frame architecture (db, events, subs)
- [x] Full API client implementation
- [x] Authentication UI (login/logout with Last.fm OAuth)
- [x] Session controls (start/pause/resume/stop)
- [x] Activity feed with real-time polling
- [x] Error handling and loading states
- [x] Tailwind CSS styling and responsive design
- [x] Hot module reload with shadow-cljs
- [x] Development environment with `bb dev` (parallel processes)

**Bug Fixes (E2E Testing):**
- [x] Activity feed now displays scrobbles correctly
- [x] Pause/Resume buttons update without refresh (Reagent Form-2)
- [x] Page refresh preserves polling state and scrobbles
- [x] OAuth web flow callback redirect working
- [x] Timestamp filtering (no 5min lookback, session-start only)
- [x] Re-frame dispatch errors fixed

### Sprint 7: Integration Testing & Polish ✅ (part of v0.5.0)

**Testing Infrastructure:**
- [x] ClojureScript test infrastructure with shadow-cljs
- [x] Frontend unit tests (66 tests, 197 assertions)
  - 32 event handler tests
  - 21 subscription tests
  - 13 component tests
- [x] Backend integration tests (90 tests, 482 assertions)
  - 15 edge case tests (concurrent updates, network errors, data integrity)
- [x] Test coverage reporting with cloverage (79.53% forms, 91.01% lines)
- [x] CI/CD integration with test execution and coverage upload

**Testing Documentation (2,502 lines):**
- [x] Testing README (quick start guide)
- [x] Comprehensive testing guide (contributor documentation)
- [x] E2E testing guide (Playwright setup and patterns)
- [x] Troubleshooting guide (common issues and solutions)
- [x] Coverage guide (improvement strategies)

### Sprint 8: Deployment Preparation ✅ (v0.5.0)

**E2E Testing (25 tests, 0 skipped):**
- [x] Mock Last.fm server implementation (`test/e2e/mocks/lastfm-mock-server.js`)
- [x] Full Playwright E2E test suite (auth, session management, error handling)
- [x] All 25 E2E tests passing (was 7 passing, 15 skipped)
- [x] Tests run reliably in CI with mock server

**Deployment Infrastructure:**
- [x] Google Cloud Run deployment workflows (dev/staging/prod)
- [x] Production security middleware (CORS, CSP, rate limiting, request logging)
- [x] SPA routing via `spa-not-found-interceptor` in server.clj
- [x] Unified CI/CD pipeline (lint → test → build → docker → deploy)
- [x] IAM configuration for unauthenticated Cloud Run access
- [x] Docker multi-stage build with proper frontend asset copying

**Bug Fixes:**
- [x] SPA 404 routing (non-API routes serve index.html)
- [x] Invalid username error display (`INVALID_TARGET_USERNAME` code)
- [x] clj-http error body parsing (`:throw-exceptions false` + `:coerce :always`)
- [x] error_code format consistency (underscore, not hyphen)
- [x] Request logging nil status crash
- [x] CORS in `:leave` phase for all responses
- [x] CSP environment-aware (relaxed in dev, strict in prod)

**Files Implemented:**
```
src/clj/lasso/middleware/
└── security.clj                  ✅ CORS, CSP, rate limiting, logging

test/e2e/
├── mocks/
│   └── lastfm-mock-server.js     ✅ Mock Last.fm API server
├── auth.spec.js                  ✅ Auth flow E2E tests (updated)
├── session.spec.js               ✅ Session management E2E tests (updated)
├── error-handling.spec.js        ✅ Error handling E2E tests (updated)
└── helpers.js                    ✅ E2E test utilities (updated)

docs/deployment/                  ✅ Deployment documentation
docs/sprints/sprint-8-summary.md  ✅ Sprint 8 summary
```

---

## What's In Progress

**Nothing currently in progress** - Ready to begin Sprint 9

---

## What's Next

**Immediate Next Sprint:** Sprint 9 - Launch

**Goals:**
- Configure real GCP project with proper credentials
- Deploy to staging environment and smoke test
- Deploy to production
- Set up monitoring and alerting
- Final documentation polish
- User guide for non-technical users

**See:** `NEXT.md` for detailed next steps

---

## Key Metrics

- **Test Coverage:** 181 tests, 100% passing
  - Backend: 90 tests, 482 assertions
  - Frontend: 66 tests, 197 assertions
  - E2E: 25 passing (0 skipped)
- **Code Coverage:** 79.53% forms, 91.01% lines (cloverage)
- **CI Duration:** ~3min 30s average (validate stage)
- **Code Quality:** All linting passes, no warnings
- **Docker Build:** Working, multi-stage build
- **Backend Status:** ✅ Fully functional end-to-end
- **Frontend Status:** ✅ Fully functional end-to-end
- **Application Status:** ✅ Complete full-stack application working
- **Deployment Status:** ✅ Infrastructure ready, GCP credentials needed

---

## Branch Status

```
main (v0.5.0)
  └─ Sprint 2 scaffolding
  └─ Sprint 3-4 complete backend (v0.2.0)
  └─ Sprint 5-6 complete frontend (v0.3.0)
  └─ Sprint 7 testing infrastructure
  └─ Sprint 8 deployment preparation + E2E tests

develop (v0.5.0)
  └─ All of main (v0.5.0)
  └─ Ready for Sprint 9 work
```

**Workflow:**
- Feature branches → `develop`
- Release branches → `main` (triggers automated release)

---

## Decisions Made

1. **Gitflow Model:** Using `main` for releases, `develop` for integration
2. **In-Memory Sessions:** Using atoms for MVP (will migrate to Redis later)
3. **Polling Interval:** 20 seconds (respects Last.fm rate limits)
4. **Rate Limiting:** Client-side with 200ms minimum interval
5. **Security:** OAuth-only (no passwords), encrypted session keys
6. **CI Strategy:** Unified sequential pipeline (validate → docker → deploy)
7. **Handler Pattern:** Pedestal handlers take `[request]` and return response map directly
8. **Body Parsing:** Manual InputStream parsing with fallbacks for string/map
9. **HTTP Methods:** GET for unsigned (reads), POST for signed (writes)
10. **SPA Routing:** Pedestal `::http/not-found-interceptor` for serving index.html
11. **error_code Format:** Underscore (not hyphen) for JSON key consistency

See `MEMORY.md` for more context on decisions and gotchas.

---

## Environment

- **Platform:** macOS (Darwin 23.6.0)
- **Backend:** Clojure with Pedestal + Jetty
- **Frontend:** ClojureScript with Reagent + Re-frame
- **Build:** tools.deps, shadow-cljs, Tailwind CSS
- **Deployment:** Docker on Google Cloud Run (infrastructure ready, needs credentials)

---

## Quick Commands

```bash
# Development (ONE COMMAND!)
bb dev                       # Start everything (backend + frontend + hot reload)
# or: clj -M:dev:repl then (start)

# Other useful tasks
bb test                      # Run all backend tests
bb build                     # Build production artifacts
bb clean                     # Clean build artifacts
bb tasks                     # See all available tasks

# E2E tests
npx playwright test          # Run E2E tests (requires backend running)

# Git (Gitflow)
git checkout develop         # Work from develop
git checkout -b feature/X    # Create feature branch
bb pr                        # Create PR to develop
```

---

## For New Sessions

👋 **Starting a new Claude Code session?**

1. Read `NEXT.md` for immediate next steps
2. Check `MEMORY.md` for gotchas and patterns
3. Review this file (STATUS.md) for current state
4. See `CLAUDE.md` for full project context

**Ready to code?** Jump to `NEXT.md` and start with the top task!
