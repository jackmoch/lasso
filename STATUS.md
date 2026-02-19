# Project Status

**Last Updated:** 2026-02-19
**Current Sprint:** None (v0.7.0 released, choosing next sprint)
**Project Phase:** Live (Production Deployed)

---

## Quick Status

- **Version:** v0.7.0 (released, deploying to production)
- **Main Branch:** v0.7.0 (merging now, deploy-prod pending manual trigger)
- **Develop Branch:** v0.7.0 (synced with main)
- **Active Work:** None — awaiting production deploy + next sprint selection
- **Blockers:** None

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
- [x] Last.fm API client with rate limiting
- [x] OAuth 2.0 implementation
- [x] Session store with encryption
- [x] Scrobble tracking logic
- [x] Polling engine for real-time scrobble tracking
- [x] Session lifecycle manager (start/pause/resume/stop)
- [x] 75 tests, 451 assertions

### Sprint 5-6: Frontend Development ✅ (v0.3.0)
- [x] Complete Re-frame architecture (db, events, subs)
- [x] Authentication UI (login/logout with Last.fm OAuth)
- [x] Session controls (start/pause/resume/stop)
- [x] Activity feed with real-time polling
- [x] Tailwind CSS styling and responsive design
- [x] Hot module reload with shadow-cljs

### Sprint 7: Integration Testing & Polish ✅ (part of v0.5.0)
- [x] ClojureScript test infrastructure (66 frontend tests)
- [x] Backend integration tests (90 tests)
- [x] Test coverage reporting with cloverage (79.53% forms, 91.01% lines)

### Sprint 8: Deployment Preparation ✅ (v0.5.0)
- [x] Mock Last.fm server for E2E testing
- [x] 25 Playwright E2E tests passing
- [x] Google Cloud Run deployment workflows (dev/staging/prod)
- [x] Production security middleware (CORS, CSP, rate limiting)
- [x] SPA routing via `spa-not-found-interceptor`

### Sprint 9: Launch ✅ (v0.5.1, 2026-02-18)
- [x] CI/CD pipeline fixes (OAUTH_CALLBACK_URL, staging IAM, image tagging)
- [x] Production deployed at `https://lasso.fm`
- [x] Cloud Monitoring uptime check + alerting active
- [x] OAuth end-to-end smoke test verified

### Sprint 10: Admin Dashboard ✅ (v0.6.0, 2026-02-18)
- [x] Admin console at `/admin` with protected login
- [x] Admin API: login, logout, status, force-stop session
- [x] `require-admin-auth` interceptor
- [x] Admin dashboard UI (Re-frame): metric cards, session table
- [x] Client-side routing via reitit-frontend
- [x] Admin credentials in GCP Secret Manager
- [x] 18 new backend tests (106 total)

### Sprint 11: Persistent User Profiles + Firestore ✅ (v0.7.0, 2026-02-19)
- [x] Cloud Firestore persistence layer with graceful fallback
- [x] Remember-me: 90-day cookie — no re-auth required across browser restarts
- [x] User store: upsert-user, session record create/finish, get-user-profile
- [x] `GET /api/user/profile` endpoint with stats + last 50 sessions
- [x] Profile page at `/profile`: member-since, scrobble/session/people stats, session history
- [x] Firestore data isolation by environment (dev-/staging-/no prefix)
- [x] `/health` reports Firestore status
- [x] CI workflow split: `pr.yml` (validate PRs) + `deploy.yml` (deploy on merge)
- [x] 22 new backend tests (128 total backend, 66 frontend)

**Bug fixes during testing:**
- [x] `firestore/init!` moved into `server/start` (dev REPL was skipping it)
- [x] Explicit `setProjectId` for Firestore client (ADC project detection unreliable)
- [x] `.limit(int n)` cast fix (Clojure long caused silent reflection error)
- [x] Profile fetch wired via `:navigated` handler (route controllers non-functional)
- [x] Sign Out on `/profile` now redirects to landing page
- [x] Route guard: unauthenticated users redirected from `/profile`

---

## What's In Progress

Nothing. v0.7.0 released and deploying.

---

## What's Next

**See:** `NEXT.md` for sprint options

---

## Key Metrics

- **Test Coverage:** 194 tests, 100% passing
  - Backend: 128 tests, 626 assertions
  - Frontend: 66 tests
  - E2E: 25 passing (0 skipped)
- **Code Coverage:** 79.53% forms, 91.01% lines (cloverage)
- **CI Duration:** ~2m 25s (validate), ~5m (validate + staging deploy)
- **Code Quality:** 0 lint errors, 5 warnings (unused requires)
- **Backend Status:** ✅ Fully functional (auth + sessions + profiles + admin)
- **Frontend Status:** ✅ Fully functional (main app + admin dashboard + profile page)
- **Deployment Status:** ✅ Live at lasso.fm, monitoring active

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
main (v0.7.0)
  └─ Sprint 2-4: Backend foundation
  └─ Sprint 5-6: Frontend (v0.3.0)
  └─ Sprint 7: Testing infrastructure
  └─ Sprint 8: E2E + deployment (v0.5.0)
  └─ Sprint 9: Launch + CI fixes (v0.5.1)
  └─ Sprint 10: Admin dashboard (v0.6.0)
  └─ Sprint 11: Firestore profiles + remember-me (v0.7.0)

develop (synced with main)
```

---

## Decisions Made

1. **Gitflow Model:** `main` for releases, `develop` for integration
2. **In-Memory Sessions:** Using atoms for MVP (will migrate to Redis later)
3. **Polling Interval:** 20 seconds (respects Last.fm rate limits)
4. **Security:** OAuth-only (no passwords), encrypted session keys
5. **CI Strategy:** `pr.yml` validates PRs; `deploy.yml` deploys on merge
6. **Handler Pattern:** Pedestal handlers take `[request]`, return response map directly
7. **error_code Format:** Underscore (not hyphen) for JSON key consistency
8. **Environment Secrets:** `OAUTH_CALLBACK_URL` per GitHub Environment (staging/prod differ)
9. **Production Image:** `deploy-prod.yml` uses `:latest` tag
10. **Admin Auth:** Separate session atom + `SameSite=Strict` cookie
11. **Admin Credentials:** GCP Secret Manager, mounted as env vars in Cloud Run
12. **Firestore:** Graceful fallback — app fully functional without Firestore
13. **Firestore Namespacing:** `dev-`/`staging-`/`""` prefix derived from `ENVIRONMENT` env var
14. **Remember-Me:** 90-day `lasso-remember` cookie; token stored in `remember_tokens` collection
15. **`:navigated` handler:** Route-triggered data fetches go here (not route controllers — `rfc/apply-controllers` is never called)

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
# Development
bb dev                       # Start everything (backend + frontend + hot reload)
bb test                      # Run all backend tests
bb build                     # Build production artifacts

# Production
gh workflow run deploy-prod.yml --ref main --field version=v0.7.0
gcloud run services logs read lasso --region us-central1 --limit 50
curl https://lasso.fm/health
```

---

## For New Sessions

👋 **Starting a new Claude Code session?**

1. Read `NEXT.md` for immediate next steps
2. Check `MEMORY.md` for gotchas and patterns
3. Review this file (STATUS.md) for current state
4. See `CLAUDE.md` for full project context
