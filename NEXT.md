# What to Work On Next

**Last Updated:** 2026-02-17 (Post v0.5.0 - Sprint 8 Complete)

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Sprint 9: Launch

**Goal:** Deploy Lasso to production and make it available to users

**Current Status:**
- ✅ Sprint 8 complete (25 E2E tests, deployment infrastructure ready)
- ✅ All 181 tests passing (backend + frontend + E2E)
- ✅ Docker build working, Cloud Run workflows configured
- ✅ Security middleware in place (CORS, CSP, rate limiting)
- ✅ Deployment infrastructure ready
- 🎯 Need real GCP project credentials to complete deployment

**Branch:** Create `feature/sprint-9-launch` from `develop`

---

## Sprint 9 Tasks

### Phase 1: GCP Setup & Staging Deployment (Priority)

**Goal:** Configure real GCP infrastructure and deploy to staging

**Tasks:**
1. **GCP Project Setup**
   - [ ] Create GCP project (or use existing)
   - [ ] Enable Cloud Run, Container Registry APIs
   - [ ] Create service account with appropriate roles
   - [ ] Add secrets to GitHub: `GCP_PROJECT_ID`, `GCP_SA_KEY`, `GCP_REGION`

2. **Secrets Configuration**
   - [ ] Add production secrets to GitHub repository:
     - `LASTFM_API_KEY` - Real Last.fm API key
     - `LASTFM_API_SECRET` - Real Last.fm API secret
     - `LASTFM_CALLBACK_URL` - Production callback URL
     - `SESSION_SECRET` - Random 32+ char secret
   - [ ] Configure Cloud Run environment variables

3. **Staging Deployment**
   - [ ] Trigger deploy-staging workflow
   - [ ] Verify container starts successfully
   - [ ] Test OAuth flow end-to-end with real Last.fm
   - [ ] Smoke test all functionality

**Acceptance Criteria:**
- App running on Cloud Run staging URL
- Full OAuth flow working with real Last.fm credentials
- Session start/pause/resume/stop all working
- Scrobbles appearing correctly in Last.fm profile

**Priority:** HIGH - Core sprint goal

---

### Phase 2: Production Deployment

**Goal:** Deploy to production and configure domain

**Tasks:**
1. **Production Configuration**
   - [ ] Update OAuth callback URL to production domain
   - [ ] Configure custom domain (if desired)
   - [ ] Set up HTTPS (Cloud Run provides this automatically)
   - [ ] Update CORS settings for production domain

2. **Production Deployment**
   - [ ] Trigger deploy-production workflow
   - [ ] Verify production deployment
   - [ ] Test complete user flow on production

3. **DNS & Domain** (if applicable)
   - [ ] Configure custom domain in Cloud Run
   - [ ] Update Last.fm API application settings with production URL

**Acceptance Criteria:**
- App accessible at production URL
- All functionality working in production

---

### Phase 3: Monitoring & Alerting

**Goal:** Set up observability for production

**Tasks:**
1. **Cloud Logging**
   - [ ] Verify application logs appear in Cloud Logging
   - [ ] Create log-based metrics for key events (logins, sessions started)
   - [ ] Set up log retention policy

2. **Uptime Monitoring**
   - [ ] Configure Cloud Monitoring uptime checks
   - [ ] Create alerting policy for downtime
   - [ ] Set up notification channel (email, Slack, etc.)

3. **Error Tracking** (optional)
   - [ ] Evaluate Sentry or Cloud Error Reporting
   - [ ] Configure error alerts

**Acceptance Criteria:**
- Logs visible in Cloud Logging
- Uptime monitoring active with alerting
- On-call runbook created

---

### Phase 4: Documentation & Launch

**Goal:** Prepare for public launch

**Tasks:**
1. **User Documentation**
   - [ ] Create user guide explaining how to use Lasso
   - [ ] FAQ page (why scrobbles are delayed, what "following" means)
   - [ ] Troubleshooting guide for common user issues

2. **README Update**
   - [ ] Update README with public-facing description
   - [ ] Add screenshots of the application
   - [ ] Add link to live application

3. **Launch Announcement**
   - [ ] Write launch announcement (blog post, social media, etc.)

**Acceptance Criteria:**
- User guide complete
- README polished for public viewers
- Ready to share with users

---

## GitHub Secrets Needed for Deployment

To complete Sprint 9, the following GitHub repository secrets must be configured:

**GCP Credentials:**
```
GCP_PROJECT_ID       - Your GCP project ID
GCP_SA_KEY           - Service account key JSON (base64 encoded)
GCP_REGION           - Cloud Run region (e.g., us-central1)
```

**Application Secrets:**
```
LASTFM_API_KEY       - Last.fm API key from https://www.last.fm/api/account/create
LASTFM_API_SECRET    - Last.fm API secret
LASTFM_CALLBACK_URL  - https://your-cloud-run-url/api/auth/callback
SESSION_SECRET       - Random 32+ character string
```

**Setting up in GitHub:**
```bash
# Using gh CLI
gh secret set GCP_PROJECT_ID --body "your-project-id"
gh secret set LASTFM_API_KEY --body "your-api-key"
# etc.
```

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

- **Redis Session Store**
  - Migrate from in-memory atom to Redis
  - Enables multi-instance deployment and session persistence across restarts

- **Mobile App** (Far future)
  - Native iOS/Android apps
  - Push notifications for new scrobbles

---

## How to Get Started

### Starting Sprint 9

```bash
# 1. Ensure you're on develop with latest changes
git checkout develop
git pull origin develop

# 2. Create sprint branch
git checkout -b feature/sprint-9-launch

# 3. Configure GitHub secrets (see above)

# 4. Test staging deployment
# (after secrets are configured, push to trigger CI)
```

---

## Success Criteria for v0.6.0 Release

Sprint 9 complete when:
- ✅ App deployed to production Cloud Run
- ✅ Full OAuth flow working with real Last.fm API
- ✅ Monitoring and logging operational
- ✅ User documentation complete
- ✅ Ready for public use

**Expected Timeline:** 1-2 weeks (mostly GCP setup and testing)

**Next Release:** v0.6.0 (Sprint 9 complete, production launched)

---

## Questions or Blockers?

If you encounter issues or have questions:
1. Check `MEMORY.md` for known gotchas
2. Check `docs/deployment/` for deployment guides
3. Check `docs/testing/` for testing guides
4. Review CI workflow logs for deployment failures
5. Check GCP Console for Cloud Run logs

---

**Remember:** Sprint 9 is the finish line! The application is code-complete and tested. The main work is infrastructure configuration and validation. Take time to verify everything works correctly in staging before promoting to production. 🚀
