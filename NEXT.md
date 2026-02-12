# What to Work On Next

**Last Updated:** 2026-02-12

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Sprint 3-4 Phase 4: Implement OAuth API Routes

**Goal:** Create Pedestal routes for Last.fm OAuth authentication flow

**Files to Create/Modify:**
- `src/clj/lasso/routes.clj` - Main routes definition
- `test/clj/lasso/routes_test.clj` - Route tests

**What to Implement:**

1. **Initialize OAuth Flow** (`POST /api/auth/init`)
   ```clojure
   ;; Request: {}
   ;; Response: {:auth-url "https://last.fm/api/auth?token=..."}
   ;; - Call lasso.lastfm.oauth/get-token
   ;; - Store request token in session
   ;; - Return authorization URL for redirect
   ```

2. **OAuth Callback** (`GET /api/auth/callback`)
   ```clojure
   ;; Request: ?token=abc123
   ;; Response: {:status "success", :username "user123"}
   ;; - Extract token from query params
   ;; - Call lasso.lastfm.oauth/get-session with token
   ;; - Store session key in lasso.session.store
   ;; - Set session cookie
   ;; - Return success with username
   ```

3. **Logout** (`POST /api/auth/logout`)
   ```clojure
   ;; Request: {} (authenticated)
   ;; Response: {:status "logged-out"}
   ;; - Get session-id from request
   ;; - Call lasso.session.store/delete-session
   ;; - Clear session cookie
   ;; - Return success
   ```

**Dependencies Already Available:**
- ✅ `lasso.lastfm.oauth/get-token` - Gets OAuth request token
- ✅ `lasso.lastfm.oauth/get-session` - Exchanges token for session key
- ✅ `lasso.session.store/create-session` - Stores user session
- ✅ `lasso.session.store/delete-session` - Removes session
- ✅ `lasso.validation.schemas` - Request/response schemas

**Testing:**
- Unit tests for each route handler
- Integration test for full OAuth flow
- Test error cases (invalid token, expired session, etc.)

**Acceptance Criteria:**
- [ ] All three routes implemented and tested
- [ ] Full OAuth flow works end-to-end
- [ ] Session cookies set/cleared correctly
- [ ] All tests pass
- [ ] Code linted with no warnings

**Estimated Time:** 2-3 hours

**Reference:**
- Pedestal routing: https://pedestal.io/reference/routing-quick-reference
- OAuth flow diagram: `docs/technical-design.md` (if exists)
- Existing client code: `src/clj/lasso/lastfm/oauth.clj`

---

## After That (Queued Tasks)

### 2️⃣ Sprint 3-4 Phase 5: Session Management Routes

**Files:** `src/clj/lasso/routes.clj` (extend)

Routes to implement:
- `POST /api/session/start` - Start following target user
- `POST /api/session/pause` - Pause active session
- `POST /api/session/resume` - Resume paused session
- `POST /api/session/stop` - Stop and clear session
- `GET /api/session/status` - Get current status + recent scrobbles

**Dependencies:**
- Phase 4 (OAuth routes) must be complete
- Will use `lasso.session.manager` (to be created in Phase 6)

---

### 3️⃣ Sprint 3-4 Phase 6: Polling Engine

**Files to Create:**
- `src/clj/lasso/polling/engine.clj`
- `src/clj/lasso/polling/scheduler.clj`
- `src/clj/lasso/session/manager.clj`

**Functionality:**
- Poll target user's Last.fm every 15-30 seconds
- Identify new scrobbles
- Submit to authenticated user
- Handle errors and rate limiting
- Manage session states

**Dependencies:**
- Phase 4 & 5 (routes) must be complete
- Uses `lasso.lastfm.client` and `lasso.lastfm.scrobble`

---

### 4️⃣ Sprint 5-6: Frontend Development

**After Sprint 3-4 backend is complete:**
- UI components (Reagent)
- Re-frame state management
- Session controls (start/pause/stop)
- Activity feed for scrobbles
- Responsive design with Tailwind

---

## Backlog (Future)

- [ ] Deploy to Google Cloud Run
- [ ] Migrate sessions from atoms to Redis
- [ ] Add monitoring and logging
- [ ] Performance optimization
- [ ] Mobile app consideration

---

## When You Finish a Task

1. **Update this file** - Move completed task to STATUS.md
2. **Commit your changes** - Follow conventional commits
3. **Create PR to `develop`** - Use `./scripts/wait-for-ci.sh <pr-number>`
4. **Update STATUS.md** - Mark task as complete
5. **Commit NEXT.md + STATUS.md** together

---

## Need Help?

- **Architecture questions:** See `CLAUDE.md` § Architecture
- **Development setup:** See `CLAUDE.md` § Common Commands
- **Git workflow:** See `CONTRIBUTING.md` § Branching Strategy
- **Past decisions:** See `MEMORY.md`
- **Current state:** See `STATUS.md`

---

## Decision Points

**If you encounter any of these, consult the user before proceeding:**

- Changing API contract (routes, request/response format)
- Adding new dependencies
- Modifying database schema or session structure
- Security-related changes (auth flow, encryption)
- Performance tradeoffs (caching strategy, polling intervals)

For routine implementation following the existing patterns, proceed autonomously and create a PR for review.
