# What to Work On Next

**Last Updated:** 2026-02-12 (Evening Session)

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Sprint 5-6: E2E Testing & Verification

**Goal:** Test the complete application end-to-end and verify all functionality works

**Current Status:**
- ✅ Backend v0.2.0 fully functional (OAuth, sessions, scrobble tracking, polling)
- ✅ Frontend 100% implemented (Re-frame, components, styling)
- ✅ OAuth web flow bug fixed (callback redirect working)
- ✅ Timestamp filtering bug fixed (5min lookback, no old scrobbles)
- ✅ All tests passing (75 backend + 7 polling = 82 tests total)
- 🎯 Ready for manual E2E testing

**Branch:** `feature/sprint-5-6-frontend-wip`

**Frontend Files (All Implemented ✅):**
- `src/cljs/lasso/core.cljs` - App initialization
- `src/cljs/lasso/db.cljs` - App state schema
- `src/cljs/lasso/events.cljs` - Re-frame events (auth, session, polling, UI)
- `src/cljs/lasso/subs.cljs` - Re-frame subscriptions
- `src/cljs/lasso/api.cljs` - Backend API client
- `src/cljs/lasso/views.cljs` - Main layout with navbar
- `src/cljs/lasso/components/auth.cljs` - Login/logout UI
- `src/cljs/lasso/components/session_controls.cljs` - Session controls
- `src/cljs/lasso/components/activity_feed.cljs` - Real-time scrobble feed
- `src/cljs/lasso/components/error.cljs` - Error display

**What to Test:**

1. **Authentication Flow**
   - ✅ Login button redirects to Last.fm
   - ✅ OAuth callback redirects back to app
   - ✅ User info displays after login
   - ✅ Logout clears session
   - ✅ Session persists on page reload

2. **Session Controls**
   - ✅ Can enter target Last.fm username
   - ✅ Start button creates active session
   - ✅ Pause button pauses polling
   - ✅ Resume button resumes polling
   - ✅ Stop button (with confirmation) clears session

3. **Scrobble Tracking**
   - ✅ Only scrobbles tracks AFTER session starts
   - ✅ 5-minute lookback buffer works
   - ✅ No old scrobbles backfilled
   - ✅ Real-time updates every 5 seconds
   - ✅ Scrobble count increments correctly

4. **UI/UX**
   - ✅ Responsive design
   - ✅ Loading states during operations
   - ✅ Error messages display properly
   - ✅ Dismissable error banner

**Dependencies Already Available:**
- ✅ Backend API fully functional at `http://localhost:8080/api/*`
- ✅ OAuth flow: `/api/auth/init`, `/api/auth/callback`, `/api/auth/logout`
- ✅ Session management: `/api/session/start|pause|resume|stop|status`
- ✅ shadow-cljs build configuration
- ✅ Tailwind CSS pipeline
- ✅ Reagent and Re-frame dependencies

**Testing:**
- Manual E2E testing with real Last.fm accounts
- Test all session state transitions
- Test error handling (invalid username, network errors)
- Mobile responsiveness testing
- Cross-browser compatibility

**Testing Steps:**

1. **Start the application:**
   ```bash
   # Terminal 1: Backend
   clj -M:dev:repl
   # In REPL: (user/start)

   # Terminal 2: Frontend
   npx shadow-cljs watch app

   # Open: http://localhost:8080
   ```

2. **Test OAuth Flow:**
   - Click "Login with Last.fm"
   - Authorize on Last.fm
   - Verify redirect back to app works
   - Verify user info displays

3. **Test Session Flow:**
   - Enter a target Last.fm username (someone actively listening)
   - Click "Start Following"
   - Verify session starts
   - Wait for target to scrobble a track
   - Verify only NEW scrobbles appear (not old ones)
   - Test pause/resume
   - Test stop with confirmation

4. **Verify Bug Fixes:**
   - ✅ OAuth callback redirects properly (not stuck on Last.fm)
   - ✅ Only tracks after session start are scrobbled
   - ✅ 5-minute lookback buffer works for recent tracks

**Acceptance Criteria:**
- [ ] Complete OAuth flow works in browser
- [ ] Can start/pause/resume/stop sessions
- [ ] Real-time scrobble feed displays updates
- [ ] Only new scrobbles tracked (no old backfill)
- [ ] All error states handled gracefully
- [ ] Frontend connects successfully to backend
- [ ] App usable for basic scrobble tracking workflow

**Estimated Time:** 1-2 hours manual testing

**Reference:**
- Backend API: All routes implemented and tested
- Re-frame tutorial: https://day8.github.io/re-frame/
- Reagent docs: https://reagent-project.github.io/
- Existing skeleton: `src/cljs/lasso/core.cljs` and `views.cljs`

---

## After That (Queued Tasks)

### 2️⃣ Sprint 7: Integration & Testing

**After frontend is functional:**
- End-to-end testing with real Last.fm accounts
- Error handling improvements
- Performance optimization
- User experience polish
- Bug fixes discovered during testing

---

### 3️⃣ Sprint 8: Deployment Preparation

**Files:** Docker, CI/CD, deployment configs

Tasks:
- Production build optimization
- Docker image finalization
- Google Cloud Run configuration
- Environment variable management
- Monitoring and logging setup

---

### 4️⃣ Sprint 9: Launch

**Final steps before public release:**
- Production deployment to GCP
- Domain setup and SSL
- User documentation
- Announcement and marketing

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
