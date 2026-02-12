# GitHub Copilot Instructions - Lasso Project

## Context Files (Read These First)

For complete project context:
- `STATUS.md` - Current project state
- `NEXT.md` - Immediate next task
- `MEMORY.md` - Gotchas and patterns
- `CONTEXT.md` - Full project overview

## Project Type

Full-stack web application (Clojure backend + ClojureScript frontend)

## Tech Stack

**Backend:** Clojure, Pedestal, Jetty
**Frontend:** ClojureScript, Reagent, Re-frame, Tailwind
**Build:** tools.deps, shadow-cljs
**Deployment:** Docker, Google Cloud Run

## Code Conventions

### Naming
- Namespaces: `lasso.domain.component` (e.g., `lasso.lastfm.client`)
- Functions: Descriptive verbs (`get-recent-tracks`, not `fetch`)
- Predicates: Suffix with `?` (`valid-session?`, `authenticated?`)
- Mutations: Action verbs (`create-session`, `delete-session`)

### Style
- Max line length: 100 characters
- Follow Clojure Style Guide
- Write docstrings for public functions
- Prefer pure functions

### Testing
- Test files mirror source structure
- Use `with-redefs` for mocking
- Mock external APIs (Last.fm)
- Avoid time-dependent assertions

## Git Workflow (IMPORTANT!)

```bash
# ✅ CORRECT - Feature PRs go to develop
git checkout develop
git checkout -b feature/my-feature
gh pr create --base develop

# ❌ WRONG - Don't PR to main!
gh pr create --base main  # Only for releases!
```

## Common Patterns

### API Client Calls
```clojure
;; Always use rate-limited client
(require '[lasso.lastfm.client :as client])
(client/request :get "user.getRecentTracks" {:user "username"})
```

### Session Management
```clojure
;; Create session
(require '[lasso.session.store :as store])
(store/create-session username session-key)

;; Get session
(store/get-session session-id)
```

### Error Handling
```clojure
;; Return error maps for expected failures
{:error {:type :invalid-token :message "Token expired"}}

;; Use ex-info for exceptions
(throw (ex-info "Session not found" {:session-id session-id}))
```

## API Design

All routes return JSON:
```clojure
;; Success
{:status 200 :body {:username "user123"}}

;; Error
{:status 400 :body {:error {:type :invalid-request :message "..."}}}
```

## Environment

Never commit `.env` file! Required vars:
- `LASTFM_API_KEY`
- `LASTFM_API_SECRET`
- `SESSION_SECRET` (min 32 chars)

## Current Focus

See `NEXT.md` for the current task.
See `STATUS.md` for what's been completed.

## Constraints

- Last.fm API: 5 requests/second max
- OAuth 2.0 only (no password auth)
- Polling interval: 15-30 seconds
- Single target user (MVP)

---

For architecture details and full context, see `CONTEXT.md`.
