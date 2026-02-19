# Sprint 11 Summary: Persistent User Profiles + Firestore

**Status:** ✅ Complete
**Started:** 2026-02-18
**Completed:** 2026-02-19
**Branch:** `feature/sprint-11-firestore-profiles` → `develop` → `release/0.7.0` → `main`
**Release:** v0.7.0

---

## Overview

Sprint 11 added Cloud Firestore as a persistence layer, enabling two major features: (1) **remember-me login** — users stay authenticated for 90 days without re-running the Last.fm OAuth flow; and (2) **persistent session history** — every scrobble-following session is recorded so users can review past activity on a new `/profile` page.

The sprint also split the CI/CD pipeline into separate PR validation and deployment workflows, eliminating a redundant validate run (~2.5 min saved per deploy).

---

## What Was Built

### Backend

| File | Purpose |
|---|---|
| `src/clj/lasso/firestore/client.clj` | Firestore client with graceful fallback — all calls are no-ops if Firestore unavailable |
| `src/clj/lasso/user/store.clj` | `upsert-user`, `add-session-record`, `finish-session-record`, `get-user-profile` |
| `src/clj/lasso/user/remember.clj` | Remember-me token ops: `save-token!`, `lookup-token`, `delete-token!` (90-day TTL) |
| `src/clj/lasso/user/handlers.clj` | `GET /api/user/profile` — requires auth, returns profile + last 50 sessions |

**Modified files:**
- `auth/handlers.clj` — upsert user + set remember-me cookie after OAuth; stop active session + delete token on logout
- `middleware.clj` — remember-me fallback in `require-auth` (checks `lasso-remember` cookie when no session-id)
- `session/manager.clj` — write Firestore session record on start; update on stop/complete
- `routes.clj` — added `/api/user/profile` route with `require-auth`
- `server.clj` — `firestore/init!` moved into `server/start` (not just `-main`)
- `config.clj` — `collection-prefix` derived from `ENVIRONMENT` env var

### Frontend

| File | Purpose |
|---|---|
| `src/cljs/lasso/user/subs.cljs` | `:user/profile`, `:user/sessions`, `:user/total-scrobbles`, `:user/loading?` |
| `src/cljs/lasso/user/events.cljs` | `:user/fetch-profile`, success/failure handlers (401 → redirect home) |
| `src/cljs/lasso/user/views.cljs` | Profile page — member-since, stats cards, session history table |

**Modified files:**
- `db.cljs` — added `:user` key to default-db
- `routes.cljs` — added `/profile` route
- `views.cljs` — Profile link in header, `:profile` case in `main-panel`
- `core.cljs` — require new user namespaces
- `admin/events.cljs` — `:navigated` handler extended to dispatch `:user/fetch-profile` for `:profile` route
- `events.cljs` — registered `:navigate-home!` effect; dispatched from `:auth/logout-success`

### CI/CD

| File | Change |
|---|---|
| `.github/workflows/pr.yml` | New: validate (lint + tests + docker-validate) on PRs only |
| `.github/workflows/deploy.yml` | Renamed from `ci.yml`: deploy on push/merge, no validate step |

---

## Tests Added

| File | Tests |
|---|---|
| `test/clj/lasso/user/store_test.clj` | 14 tests — upsert, session record lifecycle, profile assembly |
| `test/clj/lasso/user/remember_test.clj` | 8 tests — token save/lookup/delete/expiry |
| `test/clj/lasso/middleware_test.clj` | Updated: remember-me restore path |
| `test/clj/lasso/auth/handlers_test.clj` | Updated: logout session finalisation |
| `test/cljs/lasso/test_utils.cljs` | Added `:navigate-home!` no-op stub in fixture |

**Final test counts: 128 backend tests, 626 assertions, 66 frontend tests — all passing**

---

## Bugs Found During Testing (All Fixed)

These bugs were discovered during manual testing after the initial merge to develop:

### 1. `firestore/init!` not called in dev
**Symptom:** No "Firestore client initialised" log — profile page silently returned empty results.
**Root cause:** `init!` was only in `-main` (production entry point). `bb dev` calls `server/start` directly.
**Fix:** Moved `firestore/init!` into `server/start` so it runs regardless of how the server starts.

### 2. Firestore using wrong GCP project
**Symptom:** `FirestoreOptions/getDefaultInstance` connected to the wrong/no project.
**Root cause:** `getDefaultInstance` reads `GOOGLE_CLOUD_PROJECT` from the real JVM environment. The `.env` file sets the variable in-process but not in the JVM env, so the value was empty.
**Fix:** Used `FirestoreOptions/newBuilder().setProjectId(project-id)` with the explicit project ID from config.

### 3. `.limit()` silent reflection error
**Symptom:** `query-subcollection` returned `nil` instead of session documents.
**Root cause:** Clojure passes a `long` to `.limit()` which expects Java `int`. The reflection error was caught silently by try/catch.
**Fix:** `(.limit (int limit-n))`

### 4. Profile data never fetched on navigation
**Symptom:** Visiting `/profile` showed loading state but no API call was made.
**Root cause:** The `:navigated` event handler only dispatched for `:admin` route. Route controllers (`{:start #(...)}`) in routes.cljs are non-functional because `rfc/apply-controllers` is never called.
**Fix:** Added `(= :profile route-name)` case to `:navigated` handler dispatching `:user/fetch-profile`.

### 5. Sign Out not redirecting from `/profile`
**Root cause:** `:auth/logout-success` updated db state but never navigated.
**Fix:** Registered `:navigate-home!` Re-frame effect + dispatched it from logout success.

### 6. Test failure: `window is not defined`
**Root cause:** `:navigate-home!` effect references `js/window` (browser-only). In Node.js tests, `js/window` doesn't exist. The effect registration in `events.cljs` top-level code overwrote the no-op stub after namespace load.
**Fix:** Re-register the no-op stub inside `with-fresh-db` fixture (runs after all namespaces load, overriding the real implementation for each test).

---

## Key Architectural Decisions

1. **Graceful Firestore fallback:** Every Firestore call is wrapped in `(when (enabled?) ...)`. If Firestore is unavailable, the app works exactly as before Sprint 11 — OAuth login works, sessions work, profile endpoint returns 503 with `PERSISTENCE_UNAVAILABLE` error code.

2. **`:navigated` over route controllers:** The reitit-frontend controller lifecycle requires `rfc/apply-controllers` to be called explicitly. Since this project never calls it, route controllers are inert. All route-triggered data fetches go in the `:navigated` event handler (consistent with the existing admin pattern).

3. **`server/start` owns Firestore init:** Rather than require callers to remember to call `firestore/init!`, moving it into `server/start` ensures it's always called — both in production (`-main` → `start`) and in development (REPL → `start`).

4. **Environment prefix via config:** Collection prefix is derived from the `ENVIRONMENT` env var in `config.clj`, not in `firestore/client.clj`. This keeps the client simple and the logic centralized.

---

## Verification Checklist

- [x] `bb test` — 128 backend tests, 0 failures
- [x] Frontend tests — 66 tests, 0 failures
- [x] First visit (no cookies): OAuth flow works as before
- [x] After OAuth: `lasso-remember` cookie set with 90-day expiry
- [x] `/profile` shows member-since, scrobble count, session history
- [x] Start session → stop session → profile history shows the row
- [x] Sign Out from `/profile` redirects to landing page
- [x] Navigate to `/profile` while logged out → redirected home
- [x] Firestore initialised in dev REPL (`bb dev` logs "Firestore client initialised")
- [x] CI: all checks pass (validate + docker-validate + deploy-staging)
