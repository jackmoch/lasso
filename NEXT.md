# What to Work On Next

**Last Updated:** 2026-02-13 (Post v0.3.0 Release)

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Sprint 7: Integration Testing & Polish

**Goal:** Test the integrated full-stack application, verify all functionality, and polish the user experience

**Current Status:**
- ✅ Backend v0.2.0 fully functional (OAuth, sessions, scrobble tracking, polling)
- ✅ Frontend v0.3.0 fully functional (Re-frame, components, hot reload)
- ✅ Full-stack application operational end-to-end
- ✅ Development experience optimized (`bb dev`, hot reload, logging)
- 🎯 Ready for comprehensive integration testing

**Branch:** Create `feature/sprint-7-integration` from `develop`

---

## Sprint 7 Tasks

### Phase 1: Integration Testing (Week 1)

**Goal:** Verify all functionality works correctly in integrated environment

**Tasks:**
1. **End-to-End Flow Testing**
   - [ ] Complete OAuth flow with real Last.fm accounts
   - [ ] Session lifecycle (start → pause → resume → stop)
   - [ ] Real-time scrobble tracking with live music listening
   - [ ] Multiple session states and transitions
   - [ ] Error scenarios and recovery

2. **Cross-Browser Testing**
   - [ ] Chrome/Edge (primary)
   - [ ] Firefox
   - [ ] Safari (if on macOS)
   - [ ] Verify hot reload works in all browsers

3. **Mobile Responsiveness**
   - [ ] Test on mobile devices or DevTools responsive mode
   - [ ] Verify UI adapts correctly to small screens
   - [ ] Test touch interactions

**Acceptance Criteria:**
- All flows work smoothly without errors
- UI is responsive and usable on mobile
- No console errors or warnings
- All features work as expected

---

### Phase 2: Performance & Polish (Week 1-2)

**Goal:** Optimize performance and improve user experience

**Tasks:**
1. **Performance Profiling**
   - [ ] Frontend load time analysis
   - [ ] Re-frame event handler performance
   - [ ] Polling efficiency (backend & frontend)
   - [ ] Memory usage over extended sessions

2. **User Experience Improvements**
   - [ ] Add loading states for all async operations
   - [ ] Improve error messages (task #6 from Sprint 5-6)
   - [ ] Add success feedback for user actions
   - [ ] Consider adding sound/notification for new scrobbles

3. **Documentation Improvements**
   - [ ] User guide for non-technical users
   - [ ] Screenshots/GIFs for README
   - [ ] FAQ section
   - [ ] Troubleshooting guide

**Acceptance Criteria:**
- Page loads quickly (<2s initial load)
- All user actions have clear feedback
- Documentation is clear and comprehensive

---

### Phase 3: Bug Fixes & Edge Cases (Week 2)

**Goal:** Handle edge cases and fix any discovered issues

**Tasks:**
1. **Edge Case Handling**
   - [ ] What happens if Last.fm API is down?
   - [ ] What if target user has no recent scrobbles?
   - [ ] What if target user makes profile private?
   - [ ] Session timeout handling
   - [ ] Network connectivity issues

2. **Error Recovery**
   - [ ] Auto-retry failed API calls
   - [ ] Graceful degradation when offline
   - [ ] Session recovery after browser restart

3. **Data Validation**
   - [ ] Input validation for usernames
   - [ ] Rate limit handling improvements
   - [ ] Duplicate scrobble prevention

**Acceptance Criteria:**
- Application handles all error scenarios gracefully
- No crashes or unrecoverable states
- Clear error messages guide user recovery

---

## Deferred Tasks from Sprint 5-6

These are nice-to-have features that can be tackled in Sprint 7 or later:

- **Task #3:** Document scrobble deduplication logic
  - Create technical doc explaining how consecutive plays work
  - Add to `docs/technical/`

- **Task #5:** Manual backfill feature (Future enhancement)
  - Allow users to manually select recent scrobbles to backfill
  - Show preview of target user's last 10 scrobbles before starting session
  - **Defer to Sprint 8 or post-launch**

- **Task #6:** Improve error messages
  - Better messaging for invalid/non-existent usernames
  - API-specific error explanations
  - **Can do in Sprint 7 Phase 2**

---

## Post-Sprint 7: Deployment (Sprint 8)

**Goal:** Deploy to production environment

**Tasks (Preview):**
1. **Deployment Infrastructure**
   - [ ] Google Cloud Run configuration
   - [ ] Environment variable setup
   - [ ] SSL/HTTPS configuration
   - [ ] Domain setup

2. **Production Readiness**
   - [ ] Production build optimization
   - [ ] Monitoring and logging setup
   - [ ] Backup strategy
   - [ ] Security audit

3. **Launch Preparation**
   - [ ] Beta testing with real users
   - [ ] Launch announcement
   - [ ] User onboarding flow

---

## How to Get Started

### Starting Sprint 7

```bash
# 1. Ensure you're on develop with latest changes
git checkout develop
git pull origin develop

# 2. Create sprint branch
git checkout -b feature/sprint-7-integration

# 3. Start development environment
bb dev

# 4. Begin Phase 1: Integration Testing
# - Use your own Last.fm account
# - Follow a friend's Last.fm username
# - Verify scrobbles are tracked correctly
```

### Testing Checklist

Create a testing checklist as you go:
- [ ] OAuth login flow
- [ ] Start session with valid username
- [ ] Scrobbles appear in activity feed
- [ ] Pause session - polling stops
- [ ] Resume session - polling restarts
- [ ] Stop session - clears all data
- [ ] Logout - clears session
- [ ] Page refresh preserves state
- [ ] Error handling for invalid username
- [ ] Error handling for API failures

---

## Success Criteria for v0.4.0 Release

Sprint 7 complete when:
- ✅ All integration tests passing
- ✅ Application stable under normal use
- ✅ Performance meets targets
- ✅ Documentation complete
- ✅ No critical bugs or issues
- ✅ Ready for deployment

**Expected Timeline:** 1-2 weeks (depending on findings during testing)

**Next Release:** v0.4.0 (Sprint 7 complete)

---

## Questions or Blockers?

If you encounter issues or have questions:
1. Check `MEMORY.md` for known gotchas
2. Check `docs/development/` for guides
3. Review test output for clues
4. Ask in PR comments or create issue

---

**Remember:** Sprint 7 is about quality and polish. Take time to test thoroughly and improve the user experience. The foundation is solid - now make it shine! ✨
