# What to Work On Next

**Last Updated:** 2026-02-12

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Sprint 5-6: Frontend Development

**Goal:** Build ClojureScript frontend with Reagent and Re-frame to connect to the fully functional backend

**Current Status:**
- ✅ Backend v0.2.0 fully functional (OAuth, sessions, scrobble tracking, polling)
- ✅ 75 tests passing, 451 assertions
- 🎯 Ready to build the UI

**Files to Create/Modify:**

**Core Re-frame Setup:**
- `src/cljs/lasso/events.cljs` - Event handlers for state mutations
- `src/cljs/lasso/subs.cljs` - Subscriptions for component data
- `src/cljs/lasso/db.cljs` - App state schema and initialization
- `src/cljs/lasso/api.cljs` - Backend API client

**UI Components:**
- `src/cljs/lasso/views.cljs` - Main app layout
- `src/cljs/lasso/components/auth.cljs` - Login/logout UI
- `src/cljs/lasso/components/session_controls.cljs` - Start/pause/resume/stop buttons
- `src/cljs/lasso/components/activity_feed.cljs` - Real-time scrobble display
- `src/cljs/lasso/components/status.cljs` - Session status display

**What to Implement:**

1. **Authentication Flow**
   - Login button that calls `POST /api/auth/init`
   - Redirect to Last.fm OAuth
   - Handle callback and show authenticated state
   - Logout functionality
   - Session persistence check on app load

2. **Session Controls**
   - Form to enter target Last.fm username
   - Start button (`POST /api/session/start`)
   - Pause/Resume buttons (conditional rendering)
   - Stop button with confirmation
   - Visual state indicators (not-started, active, paused, stopped)

3. **Activity Feed**
   - Poll `GET /api/session/status` every 5 seconds when active
   - Display recent scrobbles in real-time
   - Show scrobble count
   - Display target username
   - Show last poll time

4. **State Management**
   - Re-frame events for all API calls
   - Subscriptions for auth state, session state, scrobbles
   - Loading states and error handling
   - Optimistic UI updates

5. **Styling**
   - Responsive layout with Tailwind CSS
   - Dark/light theme support
   - Mobile-friendly design
   - Loading spinners and transitions
   - Error message display

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

**Acceptance Criteria:**
- [ ] Complete OAuth flow working in browser
- [ ] Can start/pause/resume/stop sessions
- [ ] Real-time scrobble feed displays updates
- [ ] Responsive design works on mobile
- [ ] All error states handled gracefully
- [ ] Frontend connects successfully to backend
- [ ] App usable for basic scrobble tracking workflow

**Estimated Time:** 1-2 days

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
