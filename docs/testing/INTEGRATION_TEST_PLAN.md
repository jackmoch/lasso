# Integration Test Plan - Sprint 7

**Version:** 0.3.0
**Created:** 2026-02-13
**Sprint:** 7 (Integration Testing & Polish)

This document provides a comprehensive checklist for manual integration testing of the Lasso application.

---

## Prerequisites

Before starting integration tests:

- [ ] Application running locally: `bb dev`
- [ ] Access to Last.fm account for testing
- [ ] Access to a second Last.fm account (or friend's username) for following
- [ ] Browser DevTools open for console monitoring
- [ ] Network tab open to monitor API calls

**Test Accounts Needed:**
- Primary: Your Last.fm account (for OAuth login)
- Target: Another Last.fm username to follow (must have recent listening activity)

---

## Phase 1: Core Functionality Testing

### 1.1 Initial Application Load

**Goal:** Verify application starts correctly and displays initial state

- [ ] Navigate to `http://localhost:8080`
- [ ] Verify page loads without errors
- [ ] Check browser console - no errors or warnings
- [ ] Verify "Login with Last.fm" button is visible
- [ ] Verify app is in logged-out state

**Expected State:**
```
✓ No console errors
✓ Clean UI with login prompt
✓ No network errors
```

---

### 1.2 OAuth Authentication Flow

**Goal:** Complete OAuth login with Last.fm

**Steps:**
1. [ ] Click "Login with Last.fm" button
2. [ ] Verify redirect to Last.fm authorization page
3. [ ] Check URL contains `api_key` and `cb` parameters (NO `token` parameter)
4. [ ] Authorize the application on Last.fm
5. [ ] Verify redirect back to `http://localhost:8080/api/auth/callback`
6. [ ] Verify redirect to main app view
7. [ ] Verify username is displayed in UI
8. [ ] Verify "Logout" button is visible
9. [ ] Check browser console - no errors

**Expected Behavior:**
- Clean redirect flow (no error pages)
- Session cookie set (`lasso-session`)
- Authenticated state in Re-frame db
- Username displayed correctly

**API Calls to Verify (Network Tab):**
```
POST /api/auth/callback?token=... → 302 redirect
GET / → 200 (app loads authenticated state)
```

**Potential Issues:**
- If token is included in auth URL = Wrong flow (Desktop vs Web)
- If stuck on "Application authenticated" page = Callback not working
- If console shows "session-id nil" = Session middleware issue

---

### 1.3 Session Start - Valid Username

**Goal:** Start a following session with a valid Last.fm username

**Setup:**
- Choose a Last.fm username that has recent listening activity (check on Last.fm website)

**Steps:**
1. [ ] Enter target username in input field
2. [ ] Click "Start Session" button
3. [ ] Verify button changes to "Starting..."
4. [ ] Verify session state changes to "Active"
5. [ ] Verify UI shows:
   - Target username
   - Session status: "Active"
   - Scrobble count: 0 (initially)
   - "Pause" and "Stop" buttons visible
6. [ ] Check browser console - no errors

**Expected Behavior:**
- Button feedback during API call
- Status updates immediately
- Polling starts automatically (check Network tab for `/api/session/status` calls every 3s)

**API Calls to Verify:**
```
POST /api/session/start {"target-username": "..."} → 200
GET /api/session/status (repeating every 3s)
```

**Potential Issues:**
- If UI doesn't update = Re-frame event not dispatching correctly
- If status polling doesn't start = Check :start-status-polling event
- If "Invalid username" error = Username doesn't exist or profile is private

---

### 1.4 Scrobble Tracking - Real-Time

**Goal:** Verify scrobbles are detected and displayed in real-time

**Setup:**
- Target user must be actively listening to music on Last.fm
- Or wait for target user's scrobbles to appear

**Steps:**
1. [ ] Wait for target user to scrobble tracks
2. [ ] Verify new scrobbles appear in activity feed
3. [ ] Verify scrobble count increments
4. [ ] Verify each scrobble displays:
   - Artist name
   - Track name
   - Timestamp (relative time, e.g., "2 minutes ago")
5. [ ] Check backend logs for scrobble submission
6. [ ] Verify scrobbles appear on your Last.fm profile

**Expected Behavior:**
- Scrobbles appear within 3-20 seconds of target user's scrobble
- Activity feed updates automatically (no refresh needed)
- Count increments correctly
- Backend successfully submits to Last.fm API

**API Calls to Verify:**
```
GET /api/session/status → 200
Response includes recent-scrobbles array
Each with: artist, track, timestamp
```

**Potential Issues:**
- If scrobbles don't appear = Check backend polling logs
- If old scrobbles appear = Timestamp filtering issue
- If duplicate scrobbles = Cache not working correctly

---

### 1.5 Session Pause

**Goal:** Verify session can be paused and polling stops

**Steps:**
1. [ ] Click "Pause" button
2. [ ] Verify button changes to "Pausing..."
3. [ ] Verify session state changes to "Paused"
4. [ ] Verify "Resume" and "Stop" buttons visible
5. [ ] Verify "Pause" button is NOT visible
6. [ ] Check Network tab - status polling stops
7. [ ] Verify scrobble count and activity feed preserved
8. [ ] Wait 30s - verify no new scrobbles are tracked
9. [ ] Check browser console - no errors

**Expected Behavior:**
- Immediate state transition
- Polling stops (no more `/api/session/status` calls)
- All session data preserved
- Backend polling pauses

**API Calls to Verify:**
```
POST /api/session/pause → 200
(No more GET /api/session/status calls)
```

**Potential Issues:**
- If button doesn't update = Reagent Form-2 issue
- If polling continues = Frontend not stopping polling
- If data is lost = State management issue

---

### 1.6 Session Resume

**Goal:** Verify paused session can be resumed

**Steps:**
1. [ ] Click "Resume" button
2. [ ] Verify button changes to "Resuming..."
3. [ ] Verify session state changes to "Active"
4. [ ] Verify "Pause" and "Stop" buttons visible
5. [ ] Verify "Resume" button is NOT visible
6. [ ] Check Network tab - status polling resumes
7. [ ] Verify previous scrobbles still visible
8. [ ] Verify new scrobbles are tracked again
9. [ ] Check browser console - no errors

**Expected Behavior:**
- Clean state transition
- Polling resumes immediately
- All previous data intact
- New scrobbles detected

**API Calls to Verify:**
```
POST /api/session/resume → 200
GET /api/session/status (polling resumes)
```

---

### 1.7 Session Stop

**Goal:** Verify session can be stopped and all data cleared

**Steps:**
1. [ ] Note current scrobble count
2. [ ] Click "Stop" button
3. [ ] Verify button changes to "Stopping..."
4. [ ] Verify session state changes to "Not Started"
5. [ ] Verify UI shows:
   - Input field for target username (empty)
   - "Start Session" button
   - No session data displayed
6. [ ] Verify activity feed is cleared
7. [ ] Verify scrobble count reset to 0
8. [ ] Check Network tab - status polling stops
9. [ ] Check browser console - no errors

**Expected Behavior:**
- Complete state reset
- All session data cleared
- UI returns to initial state
- Polling stops

**API Calls to Verify:**
```
POST /api/session/stop → 200
(No more GET /api/session/status calls)
```

---

### 1.8 Logout Flow

**Goal:** Verify logout clears authentication and session

**Steps:**
1. [ ] Start a new session (if not already active)
2. [ ] Click "Logout" button
3. [ ] Verify redirect or state change to logged-out view
4. [ ] Verify "Login with Last.fm" button visible
5. [ ] Verify session data cleared
6. [ ] Check browser - session cookie removed
7. [ ] Verify status polling stopped
8. [ ] Check browser console - no errors

**Expected Behavior:**
- Clean logout
- All state cleared
- Session cookie removed
- Polling stopped

**API Calls to Verify:**
```
POST /api/auth/logout → 200
```

---

### 1.9 Page Refresh - Session Persistence

**Goal:** Verify session state persists across page refreshes

**Test Scenario A: Active Session**
1. [ ] Start a session
2. [ ] Wait for a few scrobbles
3. [ ] Refresh page (Cmd+R / Ctrl+R)
4. [ ] Verify authenticated state preserved
5. [ ] Verify session state preserved (Active/Paused)
6. [ ] Verify scrobbles still visible
7. [ ] Verify polling resumes automatically
8. [ ] Check browser console - no errors

**Test Scenario B: Paused Session**
1. [ ] Start a session and pause it
2. [ ] Refresh page
3. [ ] Verify session is still paused
4. [ ] Verify polling does NOT start automatically
5. [ ] Verify "Resume" button works after refresh

**Expected Behavior:**
- Full state restoration from session cookie
- Polling state correctly restored
- No data loss

---

## Phase 2: Error Handling & Edge Cases

### 2.1 Invalid Username

**Goal:** Verify graceful handling of invalid usernames

**Steps:**
1. [ ] Enter a non-existent username (e.g., "thisuserdoesnotexist12345")
2. [ ] Click "Start Session"
3. [ ] Verify error message displayed
4. [ ] Verify UI remains in "Not Started" state
5. [ ] Verify no polling starts
6. [ ] Check browser console - no uncaught errors

**Expected Error Message:**
"Unable to start session: Invalid or non-existent username"

---

### 2.2 Target User with No Recent Activity

**Goal:** Verify behavior when target user hasn't scrobbled recently

**Steps:**
1. [ ] Find a Last.fm user with no recent scrobbles (check on Last.fm)
2. [ ] Start session with this username
3. [ ] Verify session starts successfully
4. [ ] Verify scrobble count stays at 0
5. [ ] Verify activity feed shows no scrobbles
6. [ ] Wait 1-2 minutes - verify no errors occur

**Expected Behavior:**
- Session runs normally
- No scrobbles tracked (as expected)
- No errors or crashes

---

### 2.3 Network Connectivity Issues

**Goal:** Verify application handles network failures gracefully

**Test Scenario A: Backend Down**
1. [ ] Start a session
2. [ ] Stop backend server (Ctrl+C in terminal)
3. [ ] Observe frontend behavior
4. [ ] Verify error message displayed after polling fails
5. [ ] Restart backend: `bb dev`
6. [ ] Verify application recovers

**Test Scenario B: Intermittent Network**
1. [ ] Use browser DevTools to throttle network (Slow 3G)
2. [ ] Start session and observe behavior
3. [ ] Verify requests complete (eventually)
4. [ ] Verify no crashes

---

### 2.4 Session Timeout

**Goal:** Verify session expiration handling

**Steps:**
1. [ ] Log in and start a session
2. [ ] Wait for session to expire (if implemented) OR manually clear session cookie
3. [ ] Try to pause/resume/stop session
4. [ ] Verify redirect to login or error message
5. [ ] Verify graceful handling (no crashes)

---

## Phase 3: Cross-Browser Testing

### 3.1 Chrome/Edge (Chromium)

- [ ] Repeat all Phase 1 tests
- [ ] Verify UI renders correctly
- [ ] Verify hot reload works
- [ ] Check browser console for Chromium-specific errors

---

### 3.2 Firefox

- [ ] Repeat all Phase 1 tests
- [ ] Verify OAuth redirect works
- [ ] Verify UI renders correctly
- [ ] Check browser console for Firefox-specific errors

---

### 3.3 Safari (macOS only)

- [ ] Repeat all Phase 1 tests
- [ ] Verify OAuth redirect works
- [ ] Verify UI renders correctly
- [ ] Check browser console for Safari-specific errors

---

## Phase 4: Mobile Responsiveness

### 4.1 Responsive Design Testing

**Using Browser DevTools:**
1. [ ] Open DevTools responsive mode (Cmd+Shift+M / Ctrl+Shift+M)
2. [ ] Test at different viewport sizes:
   - [ ] iPhone SE (375x667)
   - [ ] iPhone 12 Pro (390x844)
   - [ ] Pixel 5 (393x851)
   - [ ] iPad (810x1080)

**For Each Size:**
- [ ] Verify layout adapts correctly
- [ ] Verify all buttons are tappable
- [ ] Verify text is readable (not too small)
- [ ] Verify no horizontal scrolling
- [ ] Verify activity feed scrolls vertically
- [ ] Test all functionality (login, start, pause, resume, stop)

---

### 4.2 Touch Interactions

**On Mobile Device (if available):**
1. [ ] Test on actual mobile device
2. [ ] Verify touch targets are large enough
3. [ ] Verify scrolling is smooth
4. [ ] Verify no hover-only interactions

---

## Phase 5: Performance Testing

### 5.1 Initial Load Time

**Goal:** Verify application loads quickly

**Steps:**
1. [ ] Clear browser cache
2. [ ] Open DevTools Network tab
3. [ ] Navigate to `http://localhost:8080`
4. [ ] Measure load time (DOMContentLoaded, Load events)

**Target:**
- Initial load: < 2 seconds
- Time to interactive: < 3 seconds

**Record Results:**
- DOMContentLoaded: _____ ms
- Load: _____ ms
- Total transferred: _____ KB

---

### 5.2 Polling Overhead

**Goal:** Verify polling doesn't cause performance degradation

**Steps:**
1. [ ] Start session
2. [ ] Let it run for 10-15 minutes
3. [ ] Monitor:
   - [ ] Memory usage (DevTools Memory profiler)
   - [ ] CPU usage
   - [ ] Network requests (should be minimal - only status polling)
4. [ ] Verify application remains responsive
5. [ ] Verify no memory leaks

**Expected:**
- Stable memory usage (no continuous growth)
- Low CPU usage when idle
- Status polling every 3 seconds (frontend) and 20 seconds (backend)

---

### 5.3 Re-frame Event Performance

**Goal:** Verify event handlers execute quickly

**Steps:**
1. [ ] Open Re-frame DevTools (if available)
2. [ ] Trigger various events (start, pause, resume)
3. [ ] Monitor event execution time
4. [ ] Verify no slow event handlers (>100ms)

---

## Test Results Summary

### Test Run 1: [DATE]

**Environment:**
- OS: _____
- Browser: _____
- Backend version: _____
- Frontend version: _____

**Phase 1 Results:**
- [ ] All tests passed
- [ ] Some tests failed (document below)

**Issues Found:**
1. [Issue description]
   - Severity: High/Medium/Low
   - Steps to reproduce: ...
   - Expected: ...
   - Actual: ...

**Phase 2 Results:**
- [ ] All tests passed
- [ ] Some tests failed (document below)

**Phase 3 Results:**
- [ ] Chrome: Pass/Fail
- [ ] Firefox: Pass/Fail
- [ ] Safari: Pass/Fail

**Phase 4 Results:**
- [ ] Mobile responsive: Pass/Fail
- [ ] Issues: ...

**Phase 5 Results:**
- Initial load: _____ ms
- Memory stable: Yes/No
- Performance acceptable: Yes/No

---

## Known Issues & Workarounds

### Issue: [Title]
**Severity:** High/Medium/Low
**Description:** ...
**Workaround:** ...
**Fix Status:** Open/In Progress/Fixed

---

## Next Steps After Testing

Once all tests pass:
1. [ ] Document any bugs found in GitHub Issues
2. [ ] Update `NEXT.md` with Phase 2 tasks (Performance & Polish)
3. [ ] Create sprint summary document
4. [ ] Consider moving to Phase 2 or addressing critical bugs first

---

## Notes & Observations

Use this section for any additional notes during testing:

- ...
- ...
