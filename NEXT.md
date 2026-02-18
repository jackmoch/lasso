# What to Work On Next

**Last Updated:** 2026-02-18 (Sprint 9 + Sprint 10 complete on develop)

This file tells you exactly what to work on next. When you finish a task, update this file and commit it.

---

## Immediate Next Task

### 🎯 Cut v0.6.0 Release

**Goal:** Ship the admin dashboard + monitoring + Sprint 9 completion to production

**Current Status:**
- ✅ Admin dashboard (Sprint 10) merged to develop
- ✅ Monitoring & ops (Sprint 9 Phase 3) merged to develop
- ✅ OAuth configured and working (production + staging)
- ✅ Admin secrets deployed (GCP Secret Manager → Cloud Run)
- ✅ Health check logs suppressed, per-route rate limiting added
- ✅ All 197 tests passing (106 backend, 66 frontend, 25 E2E)
- 📦 Ready to release as v0.6.0

**Steps:**

1. **Ensure develop is up to date:**
   ```bash
   git checkout develop
   git pull origin develop
   ```

2. **Create release branch:**
   ```bash
   git checkout -b release/0.6.0
   ```

3. **Bump VERSION file:**
   ```bash
   echo "0.6.0" > VERSION
   ```

4. **Update CHANGELOG.md:**
   - Change `## [Unreleased]` → `## [0.6.0] - 2026-02-18`
   - Add link at bottom: `[0.6.0]: https://github.com/jackmoch/lasso/compare/v0.5.1...v0.6.0`
   - Update `[Unreleased]` link: `[Unreleased]: https://github.com/jackmoch/lasso/compare/v0.6.0...HEAD`

5. **Commit and push:**
   ```bash
   git add VERSION CHANGELOG.md
   git commit -m "chore(release): bump version to 0.6.0"
   git push -u origin release/0.6.0
   ```

6. **Create PR to main:**
   ```bash
   gh pr create --base main --title "Release v0.6.0" --body "Sprint 9 (Launch) + Sprint 10 (Admin Dashboard) release"
   ```

7. **After PR merges:** GitHub Actions automatically creates the tag + release

8. **Sync develop:**
   ```bash
   git checkout develop
   git merge origin/main
   git push origin develop
   ```

**Acceptance Criteria:**
- `v0.6.0` tag exists on main
- GitHub release created with CHANGELOG notes
- Production auto-deploys via `deploy-prod.yml`

---

## After v0.6.0 Ships

### End-to-End Smoke Test

Verify the full user flow on production:

1. Visit `https://lasso.fm`
2. Click "Login with Last.fm" → complete OAuth flow → confirm redirect back, logged in
3. Enter a target username → Start session
4. Wait ~20 seconds → confirm scrobbles appear in activity feed
5. Pause, resume, stop the session
6. Test admin console: visit `/admin/login` → login with admin credentials → view sessions → confirm they appear

### Admin Console Access

Admin credentials are in GCP Secret Manager:
- **Username:** `admin`
- **Password:** stored in `admin-password` secret in GCP Secret Manager (project: `lasso-scrobbler-0667`)
- **URL:** `https://lasso.fm/admin/login`

---

## Deferred Tasks

These features can be tackled post-launch:

- **Manual Backfill Feature**
  - Allow users to manually select recent scrobbles to backfill
  - Show preview of target user's last 10 scrobbles before starting session

- **Enhanced Error Messages**
  - Better messaging for invalid/non-existent usernames
  - API-specific error explanations with recovery suggestions

- **Redis Session Store**
  - Migrate from in-memory atom to Redis
  - Enables multi-instance deployment and session persistence across restarts

- **Session Detail View** (Admin)
  - Drill into a single session's full recent-scrobbles list
  - Track Last.fm API error rates per session

- **Log-Based Metrics**
  - Track login events, session starts, scrobble counts in Cloud Monitoring

- **User Guide**
  - Simple in-app or docs page explaining Lasso for non-technical users
  - What "following" means, why scrobbles are delayed, how to stop

---

## Reference

**Cloud Run URLs:**
- Production: `https://lasso-ngqcsb2bpa-uc.a.run.app` (also `https://lasso.fm`)
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
gh workflow run deploy-prod.yml --ref main --field version=v0.6.0
```

---

## Questions or Blockers?

If you encounter issues:
1. Check `MEMORY.md` for known gotchas
2. Check `docs/deployment/` for deployment guides
3. Review CI workflow logs: `gh run list --branch main`
4. Check GCP Console → Cloud Run → lasso → Logs
