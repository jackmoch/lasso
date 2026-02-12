# Lasso Project Memory

Key decisions, patterns, and gotchas to remember across sessions.

## Core Patterns

### Always Use Gitflow
- **Feature/bugfix branches → `develop`** (NOT `main`!)
- **Release branches → `main`** (triggers automated release)
- Create PRs with: `gh pr create --base develop`
- Never push directly to `main` or `develop`

### CI Workflow
- Average run time: ~2min 15s (135 seconds)
- Use `./scripts/wait-for-ci.sh <pr-number>` instead of polling
- CI runs on both `push` and `pull_request` events
- Must pass `lint-and-build` check before merge

### Testing Strategy
- Write tests alongside implementation (not after)
- Mock external APIs (Last.fm) in tests
- Use `with-redefs` for mocking functions
- Test files mirror source structure (`test/clj/lasso/...`)

## Gotchas & Fixes

### 1. Flaky Rate-Limiting Test (FIXED)
**Problem:** Rate-limiting test failed intermittently due to timing dependencies

**File:** `test/clj/lasso/lastfm/client_test.clj`

**Solution:** Mock `wait-for-rate-limit` function and count invocations instead of measuring elapsed time

**Lesson:** Avoid time-dependent assertions in tests; mock time-sensitive functions

### 2. CI Duplicate Runs (FIXED)
**Problem:** CI ran twice on every PR

**Cause:** Both `pull_request` and `pull_request_target` triggers in workflow

**Solution:** Removed `pull_request_target` (only needed for fork PRs with write access)

**Lesson:** Use `pull_request` for standard CI; `pull_request_target` only for fork security

### 3. Test Failures Not Failing CI (FIXED)
**Problem:** CI had `continue-on-error: true` and `|| echo` fallback

**Solution:** Removed both so CI properly fails when tests fail

**Lesson:** Don't suppress test failures in CI; branch protection depends on it

## Architectural Decisions

### In-Memory Session Storage (Temporary)
**Decision:** Use atoms for session storage in MVP

**Rationale:**
- Simple for single-instance deployment
- Fast iteration during development
- Sufficient for alpha testing

**Future:** Migrate to Redis when deploying multi-instance on Cloud Run

**Impact:** Sessions lost on server restart (acceptable for MVP)

### Client-Side Rate Limiting
**Decision:** Implement rate limiting in HTTP client, not relying on Last.fm

**Rationale:**
- Last.fm limit: 5 requests/second
- Proactive limiting prevents 429 errors
- Better UX with predictable delays

**Implementation:** `lasso.lastfm.client/rate-limiter` atom with `wait-for-rate-limit`

### OAuth 2.0 Only (No Password Auth)
**Decision:** Support only OAuth 2.0, no username/password login

**Rationale:**
- More secure (no password storage)
- Last.fm encourages OAuth for new apps
- Better user experience (redirect flow)

**Impact:** Requires callback URL configuration in Last.fm app settings

### Polling Interval: 15-30 Seconds
**Decision:** Poll target user's recent tracks every 15-30 seconds

**Rationale:**
- Respects Last.fm rate limit (5 req/sec)
- Balances real-time updates with API conservation
- Allows room for other API calls (scrobble submission)

**Formula:** `num-active-sessions * (1 poll + 1 scrobble) * 2/sec < 5/sec`

## Code Conventions

### Namespace Organization
```
lasso.domain.component
```

Examples:
- `lasso.lastfm.client` - Last.fm API client
- `lasso.session.store` - Session storage
- `lasso.util.crypto` - Encryption utilities

### Function Naming
- Use descriptive names: `get-recent-tracks`, not `fetch`
- Prefix predicates with `?`: `valid-session?`, `authenticated?`
- Action verbs for mutations: `create-session`, `delete-session`

### Error Handling
- Use `ex-info` for custom exceptions with data
- Return `{:error ...}` maps for expected failures
- Let unexpected errors bubble up for logging

## Environment Configuration

### Required Env Vars
```bash
LASTFM_API_KEY=...           # From Last.fm API account
LASTFM_API_SECRET=...        # From Last.fm API account
OAUTH_CALLBACK_URL=...       # http://localhost:8080/api/auth/callback
SESSION_SECRET=...           # Min 32 chars, random
PORT=8080
ENVIRONMENT=development
```

**Security:** NEVER commit `.env` file (already gitignored)

## Useful Debugging Commands

```bash
# Check session state in REPL
(require '[lasso.session.store :as store])
@store/sessions

# Test OAuth flow manually
(require '[lasso.lastfm.oauth :as oauth])
(oauth/get-token)

# Inspect rate limiter
(require '[lasso.lastfm.client :as client])
@client/rate-limiter

# Run specific test namespace
clj -M:test --focus lasso.lastfm.client-test
```

## Common Errors & Solutions

### Error: "Session key not found"
**Cause:** Session expired or never created

**Fix:** Check session store, ensure OAuth flow completed

### Error: "Invalid API signature"
**Cause:** API secret mismatch or param encoding issue

**Fix:** Verify `LASTFM_API_SECRET`, check signature generation in `lasso.lastfm.client`

### Error: "Rate limit exceeded"
**Cause:** Too many requests to Last.fm API

**Fix:** Ensure `wait-for-rate-limit` is called before each request

## External Dependencies

### Last.fm API
- Docs: https://www.last.fm/api
- Rate limit: 5 requests/second
- OAuth flow: https://www.last.fm/api/authentication

### Libraries
- Pedestal: Web framework (https://pedestal.io)
- Buddy: Encryption (https://funcool.github.io/buddy-core/latest/)
- Malli: Validation (https://github.com/metosin/malli)
- Re-frame: Frontend state (https://day8.github.io/re-frame/)

## Performance Notes

### CI Metrics (Tracked since 2026-02-12)
- Average duration: 2m 15s (135s)
- Lint + build: ~80-90s
- Docker build: ~30-45s
- Variability: ±15s (cache hits/misses)

### Build Artifacts
- Backend JAR: ~40MB (includes deps)
- Frontend JS: ~1.5MB (optimized)
- Docker image: ~150MB (Alpine + JDK)

## Session Handoff Checklist

When starting a new Claude Code session:
- [ ] Read `STATUS.md` - current project state
- [ ] Read `NEXT.md` - immediate next task
- [ ] Check git branch - should be on `develop`
- [ ] Run `git pull origin develop` - ensure up to date
- [ ] Scan this file (MEMORY.md) - recall gotchas

---

**Note:** Update this file when you encounter new gotchas or make important decisions!
