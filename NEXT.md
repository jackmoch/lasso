# What to Work On Next

**Last Updated:** 2026-02-18 (Post v0.5.1 - Production Deployed)

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Configure OAuth (BLOCKER)

**Goal:** Make the login button work on the live production app

**Current Status:**
- ✅ Production live at `https://lasso-ngqcsb2bpa-uc.a.run.app`
- ✅ Staging live at `https://lasso-staging-ngqcsb2bpa-uc.a.run.app`
- ✅ Health, home, and static assets all return 200
- ❌ OAuth login will fail — `OAUTH_CALLBACK_URL` not configured yet

**Steps:**

1. **Register callback URL with Last.fm**
   - Go to https://www.last.fm/api/accounts
   - Edit your API application
   - Set callback URL to: `https://lasso-ngqcsb2bpa-uc.a.run.app/api/auth/callback`

2. **Set GitHub Environment secrets** (Settings → Environments)
   - `production` environment → Add secret:
     ```
     OAUTH_CALLBACK_URL = https://lasso-ngqcsb2bpa-uc.a.run.app/api/auth/callback
     ```
   - `staging` environment → Add secret:
     ```
     OAUTH_CALLBACK_URL = https://lasso-staging-ngqcsb2bpa-uc.a.run.app/api/auth/callback
     ```
   - Environment secrets override repo-level secrets, so each deploy uses the right URL.

3. **Update the running production service** (can do without a full redeploy):
   ```bash
   gcloud run services update lasso \
     --region us-central1 \
     --update-env-vars OAUTH_CALLBACK_URL=https://lasso-ngqcsb2bpa-uc.a.run.app/api/auth/callback
   ```

4. **Test OAuth end-to-end:**
   - Visit https://lasso-ngqcsb2bpa-uc.a.run.app
   - Click "Login with Last.fm"
   - Complete OAuth flow
   - Confirm you land back at the app, logged in
   - Start a session, let it run for a minute, verify scrobbles appear in Last.fm

**Acceptance Criteria:**
- Login → Last.fm OAuth → callback → logged in, no errors
- Start session → scrobbles appear in Last.fm profile

**Priority:** CRITICAL — nothing else matters until this works

---

## Sprint 9 Remaining Tasks

### Phase 2: Validate Production (after OAuth configured)

1. **End-to-end smoke test on production:**
   - [ ] Login with Last.fm OAuth
   - [ ] Start a session following a test account
   - [ ] Confirm scrobbles appear after ~20 seconds
   - [ ] Pause, resume, and stop the session
   - [ ] Logout

2. **Staging validation (optional):**
   - [ ] Same test on staging URL (using staging OAUTH_CALLBACK_URL)

---

### Phase 3: Monitoring & Alerting

**Goal:** Know when the app is down before users report it

1. **Cloud Monitoring uptime check:**
   ```bash
   # Via GCP Console: Monitoring → Uptime checks → Create
   # URL: https://lasso-ngqcsb2bpa-uc.a.run.app/health
   # Check every 1 minute
   ```

2. **Alerting policy:**
   - Create alert when uptime check fails 2+ times
   - Notification channel: email

3. **Log-based metrics** (optional):
   - Track login events, session starts, scrobble counts

**Acceptance Criteria:**
- Uptime check active and alerting configured
- Logs visible in Cloud Console

---

### Phase 4: Documentation & README

**Goal:** Make the app legible to someone discovering it for the first time

1. **Update README.md:**
   - [ ] Add production URL as the "Try it" link
   - [ ] Add a brief description of what Lasso does
   - [ ] Add screenshot or demo GIF
   - [ ] Update badges to reflect live deployment

2. **User guide** (can be a simple page in the app or a docs file):
   - [ ] What is Lasso and why would you use it
   - [ ] How to start a session
   - [ ] What "following" means (mirrors scrobbles, not a social follow)
   - [ ] Why scrobbles may be slightly delayed (~20s polling interval)
   - [ ] How to stop following

**Acceptance Criteria:**
- README is useful for someone finding the repo
- Users can understand how to use the app without asking questions

---

## Success Criteria for v0.6.0 Release

Sprint 9 complete when:
- ✅ App deployed to production Cloud Run
- [ ] Full OAuth flow working with real Last.fm API (needs OAUTH_CALLBACK_URL config)
- [ ] End-to-end scrobble test passing on production
- [ ] Monitoring and uptime alerting operational
- [ ] README updated with live URL
- [ ] Ready for public use

**Next Release:** v0.6.0 (Sprint 9 complete, fully functional launch)

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

- **Custom Domain**
  - Configure a custom domain in Cloud Run (e.g., lasso.app)
  - Update Last.fm app callback URL accordingly

- **Mobile App** (Far future)
  - Native iOS/Android apps
  - Push notifications for new scrobbles

---

## Reference

**Cloud Run URLs:**
- Production: `https://lasso-ngqcsb2bpa-uc.a.run.app`
- Staging: `https://lasso-staging-ngqcsb2bpa-uc.a.run.app`

**GCP Project:** `lasso-scrobbler-0667`, region `us-central1`

**Useful commands:**
```bash
# Check prod health
curl https://lasso-ngqcsb2bpa-uc.a.run.app/health

# Update env var without full redeploy
gcloud run services update lasso \
  --region us-central1 \
  --update-env-vars KEY=VALUE

# View recent logs
gcloud run services logs read lasso --region us-central1 --limit 50

# Trigger production deploy manually
gh workflow run deploy-prod.yml --ref main --field version=vX.Y.Z
```

---

## Questions or Blockers?

If you encounter issues:
1. Check `MEMORY.md` for known gotchas
2. Check `docs/deployment/` for deployment guides
3. Review CI workflow logs: `gh run list --branch main`
4. Check GCP Console → Cloud Run → lasso → Logs
