# Project Status

**Last Updated:** 2026-02-18
**Current Sprint:** Sprint 9 (Launch - In Progress)
**Project Phase:** Live (Production Deployed)

---

## Quick Status

- **Version:** v0.5.1 (2026-02-18)
- **Main Branch:** v0.5.1 (production deployed)
- **Develop Branch:** v0.5.1 + CI/CD fixes (synced with main)
- **Active Work:** Sprint 9 - OAuth configuration + monitoring
- **Blockers:** `OAUTH_CALLBACK_URL` not yet configured → OAuth login won't work until set
- **Next Milestone:** OAuth working end-to-end on production

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

### Sprint 9: Launch - Phase 1 ✅ (v0.5.1, 2026-02-18)

**CI/CD Pipeline Fixes:**
- [x] Pass `OAUTH_CALLBACK_URL` env var to Cloud Run in all deploy stages
- [x] Staging IAM propagation: 15s wait + 5-attempt health check retry
- [x] Post-deployment comment: `continue-on-error` (no PR context on push)
- [x] `publish-image` now runs on `main` branch pushes (tagged `:latest`)
- [x] `deploy-prod.yml`: handle first-time service creation (`--no-traffic` not valid on new services)
- [x] `deploy-prod.yml`: use `:latest` image tag (removes SHA coupling between commits)

**Deployments:**
- [x] **Staging deployed:** `https://lasso-staging-ngqcsb2bpa-uc.a.run.app`
- [x] **Production deployed:** `https://lasso-ngqcsb2bpa-uc.a.run.app`
- [x] Health endpoint: 200 ✅
- [x] Home page: 200 ✅
- [x] Static assets (CSS/JS): 200 ✅

**GitHub Environments configured:**
- [x] `development`, `staging`, `production` environments created
- [x] `roles/run.admin` granted to GitHub Actions service account

---

## What's In Progress

**Sprint 9 - Phase 2: OAuth Configuration**

The app is live but OAuth login won't work until this is done:

1. Register callback URL with Last.fm (at last.fm/api/accounts):
   - Production: `https://lasso-ngqcsb2bpa-uc.a.run.app/api/auth/callback`
2. Set `OAUTH_CALLBACK_URL` as environment-level secrets in GitHub:
   - Settings → Environments → `production` → Add secret
   - Settings → Environments → `staging` → Add secret
3. Redeploy production to pick up the env var

---

## What's Next

**See:** `NEXT.md` for full Sprint 9 task list

**Remaining Sprint 9 goals:**
- Configure OAuth (above) and test end-to-end
- Set up Cloud Monitoring uptime checks
- Update README for public viewers
- User guide for non-technical users

---

## Key Metrics

- **Test Coverage:** 181 tests, 100% passing
  - Backend: 90 tests, 482 assertions
  - Frontend: 66 tests, 197 assertions
  - E2E: 25 passing (0 skipped)
- **Code Coverage:** 79.53% forms, 91.01% lines (cloverage)
- **CI Duration:** ~3min 30s (validate), ~6min (validate + staging deploy)
- **Code Quality:** All linting passes, no warnings
- **Backend Status:** ✅ Fully functional
- **Frontend Status:** ✅ Fully functional
- **Deployment Status:** ✅ Live on production (OAuth config pending)

---

## Cloud Run Services

| Environment | Service | URL | Status |
|---|---|---|---|
| Production | `lasso` | https://lasso-ngqcsb2bpa-uc.a.run.app | ✅ Live |
| Staging | `lasso-staging` | https://lasso-staging-ngqcsb2bpa-uc.a.run.app | ✅ Live |
| Dev | `lasso-dev` | (not yet deployed) | ⏳ Pending |

---

## Branch Status

```
main (v0.5.1)
  └─ Sprint 2 scaffolding
  └─ Sprint 3-4 complete backend (v0.2.0)
  └─ Sprint 5-6 complete frontend (v0.3.0)
  └─ Sprint 7 testing infrastructure
  └─ Sprint 8 deployment preparation + E2E tests (v0.5.0)
  └─ Sprint 9 CI/CD fixes + production deploy (v0.5.1)

develop (v0.5.1)
  └─ All of main (synced)
  └─ Active Sprint 9 work
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
12. **Environment Secrets:** `OAUTH_CALLBACK_URL` set per GitHub Environment (not repo-level) so staging/prod have different values
13. **Production Image:** `deploy-prod.yml` uses `:latest` tag (CI builds it on every main push)

See `MEMORY.md` for more context on decisions and gotchas.

---

## Environment

- **Platform:** macOS (Darwin 23.6.0)
- **Backend:** Clojure with Pedestal + Jetty
- **Frontend:** ClojureScript with Reagent + Re-frame
- **Build:** tools.deps, shadow-cljs, Tailwind CSS
- **Deployment:** Docker on Google Cloud Run (production live!)
- **GCP Project:** `lasso-scrobbler-0667`, region `us-central1`

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

# Production
gcloud run services describe lasso --region us-central1  # Check prod service
gcloud run services logs read lasso --region us-central1  # View logs
```

---

## For New Sessions

👋 **Starting a new Claude Code session?**

1. Read `NEXT.md` for immediate next steps
2. Check `MEMORY.md` for gotchas and patterns
3. Review this file (STATUS.md) for current state
4. See `CLAUDE.md` for full project context

**Ready to code?** Jump to `NEXT.md` and start with the top task!
