# What to Work On Next

**Last Updated:** 2026-02-13 (Post v0.4.0 - Sprint 7 Complete)

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Sprint 8: Deployment Preparation

**Goal:** Prepare application for production deployment on Google Cloud Run

**Current Status:**
- ✅ Sprint 7 complete (163 tests, comprehensive documentation)
- ✅ Backend v0.2.0 fully functional
- ✅ Frontend v0.3.0 fully functional
- ✅ Testing infrastructure in place (79.53% coverage)
- ⚠️ 15 E2E tests skipped (require auth mocking)
- 🎯 Ready for deployment preparation

**Branch:** Create `feature/sprint-8-deployment` from `develop`

---

## Sprint 8 Tasks

### Phase 1: E2E Test Completion (Priority)

**Goal:** Complete E2E test suite by implementing auth mocking

**Tasks:**
1. **Auth Mocking Infrastructure**
   - [ ] Create mock OAuth server for E2E tests
   - [ ] Mock Last.fm API endpoints for testing
   - [ ] Enable 15 skipped E2E tests
   - [ ] Verify all E2E flows pass with mocked auth

2. **E2E Coverage Expansion**
   - [ ] Add more error scenario tests
   - [ ] Add mobile viewport E2E tests
   - [ ] Add network failure simulation tests

**Acceptance Criteria:**
- All 22 E2E tests passing (0 skipped)
- Auth flow fully tested without real credentials
- E2E tests run reliably in CI

**Priority:** HIGH - Required before production deployment

---

### Phase 2: Production Environment Configuration

**Goal:** Set up production-ready infrastructure

**Tasks:**
1. **Environment Configuration**
   - [ ] Create production environment variables
   - [ ] Set up Google Cloud Run project
   - [ ] Configure OAuth callback URLs for production
   - [ ] Set up SSL/HTTPS certificates

2. **Build Optimization**
   - [ ] Minimize frontend bundle size
   - [ ] Enable production optimizations in shadow-cljs
   - [ ] Configure CDN for static assets (if needed)
   - [ ] Test production build locally

3. **Security Hardening**
   - [ ] Review session security settings
   - [ ] Configure CORS policies
   - [ ] Set up rate limiting
   - [ ] Security audit of environment variables

**Acceptance Criteria:**
- Production build completes successfully
- All security best practices implemented
- Environment variables properly configured

---

### Phase 3: Deployment & Monitoring

**Goal:** Deploy to Google Cloud Run and set up monitoring

**Tasks:**
1. **Initial Deployment**
   - [ ] Deploy to Google Cloud Run staging environment
   - [ ] Configure health checks
   - [ ] Test with real Last.fm API
   - [ ] Smoke test all functionality

2. **Monitoring & Logging**
   - [ ] Set up Google Cloud Logging
   - [ ] Configure error tracking (Sentry or similar)
   - [ ] Set up uptime monitoring
   - [ ] Create alerting rules

3. **Documentation**
   - [ ] Deployment runbook
   - [ ] Rollback procedures
   - [ ] Incident response guide
   - [ ] User guide for non-technical users

**Acceptance Criteria:**
- Application runs successfully on Cloud Run
- Monitoring and logging operational
- Documentation complete

---

## Deferred Tasks

These features can be tackled post-launch:

- **Manual Backfill Feature**
  - Allow users to manually select recent scrobbles to backfill
  - Show preview of target user's last 10 scrobbles before starting session
  - **Defer to post-launch enhancement**

- **Enhanced Error Messages**
  - Better messaging for invalid/non-existent usernames
  - API-specific error explanations with recovery suggestions

- **Scrobble Deduplication Documentation**
  - Create technical doc explaining how consecutive plays work
  - Add to `docs/technical/`

- **Mobile App** (Far future)
  - Native iOS/Android apps
  - Push notifications for new scrobbles

---

## How to Get Started

### Starting Sprint 8

```bash
# 1. Ensure you're on develop with latest changes
git checkout develop
git pull origin develop

# 2. Create sprint branch
git checkout -b feature/sprint-8-deployment

# 3. Start development environment
bb dev

# 4. Begin Phase 1: E2E Auth Mocking
# Priority: Enable the 15 skipped E2E tests
```

### Phase 1 Quick Start: E2E Auth Mocking

**Priority task:** Implement mock OAuth server for E2E tests

**Approach:**
1. Create mock Last.fm OAuth endpoints (token, session)
2. Mock API endpoints used in tests (getRecentTracks, getInfo)
3. Update Playwright config to use mock server
4. Enable skipped tests and verify they pass
5. Document mock server setup

**Files to create/modify:**
- `test/e2e/mocks/lastfm-mock-server.js` - Mock server implementation
- `test/e2e/setup.js` - Global test setup
- `playwright.config.js` - Add mock server to global setup
- `test/e2e/*.spec.js` - Enable skipped tests

---

## Success Criteria for v0.5.0 Release

Sprint 8 complete when:
- ✅ All 22 E2E tests passing (0 skipped)
- ✅ Production build optimized and tested
- ✅ Deployed to Google Cloud Run staging
- ✅ Monitoring and logging operational
- ✅ Security audit passed
- ✅ Deployment documentation complete
- ✅ Ready for production launch

**Expected Timeline:** 1-2 weeks

**Next Release:** v0.5.0 (Sprint 8 complete, ready for launch)

---

## Questions or Blockers?

If you encounter issues or have questions:
1. Check `MEMORY.md` for known gotchas
2. Check `docs/testing/` for testing guides
3. Check `docs/development/` for dev guides
4. Review test output for clues
5. Ask in PR comments or create issue

---

**Remember:** Sprint 8 focuses on production readiness. Priority is completing E2E tests, then ensuring smooth deployment. Take time to get deployment infrastructure right - it's the foundation for a successful launch! 🚀
