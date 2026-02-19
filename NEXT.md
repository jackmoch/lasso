# What to Work On Next

**Last Updated:** 2026-02-19 (v0.7.0 released)

---

## Current State

The app is fully live with persistent profiles:

- ✅ Production: `https://lasso.fm` (v0.6.0 — v0.7.0 deploy pending manual trigger)
- ✅ Staging: v0.7.0 deployed (deployed from release branch push)
- ✅ Profile page, remember-me login, session history all verified locally
- ✅ 128 backend + 66 frontend tests, 100% passing

**To deploy v0.7.0 to production:**
```bash
gh workflow run deploy-prod.yml --ref main --field version=v0.7.0
```

---

## Potential Next Sprints

There is no required work. Everything below is optional enhancements chosen by priority.

### Option A: User Experience Polish

Low effort, high user-facing impact:

1. **User guide** — add a `/how-it-works` or simple in-app explainer:
   - What "following" means (mirrors scrobbles, not a social follow)
   - Why scrobbles are ~20s delayed
   - Why only tracks scrobbled after session starts are mirrored
   - How to stop

2. **Better error messages** — when session start fails (invalid username, private profile, API rate limit), show actionable messages instead of generic errors

3. **Target user validation UX** — show a preview of the target user's profile before starting (avatar, display name, recent track) so users know they entered the right username

### Option B: Reliability & Operations

Medium effort, reduces operational risk:

1. **Redis session store** — replace in-memory atom with Redis
   - Sessions survive container restarts
   - Enables scaling to multiple Cloud Run instances
   - Cloud Run scales to zero at night → current sessions are lost on cold start

2. **Log-based metrics** — Cloud Monitoring dashboards for:
   - Login events per hour
   - Active sessions over time
   - Scrobbles submitted per hour
   - Last.fm API error rate

3. **Session persistence on restart** — even without Redis, writing sessions to a file or Cloud Storage on shutdown would survive most restarts

### Option C: Features

Higher effort, new capabilities:

1. **Manual backfill** — after starting a session, show the target user's last N scrobbles (not yet mirrored) and let the user select which to backfill

2. **Session detail view (Admin)** — drill into a single user's session: full recent-scrobble list, Last.fm API error log, polling health

3. **Multiple concurrent targets** — follow more than one user at a time (for when the Jam host changes mid-session)

---

## Reference

**Cloud Run URLs:**
- Production: `https://lasso-ngqcsb2bpa-uc.a.run.app` (also `https://lasso.fm`)
- Staging: `https://lasso-staging-ngqcsb2bpa-uc.a.run.app`

**GCP Project:** `lasso-scrobbler-0667`, region `us-central1`

**Useful commands:**
```bash
# Deploy v0.7.0 to production
gh workflow run deploy-prod.yml --ref main --field version=v0.7.0

# Check prod health
curl https://lasso.fm/health

# View recent logs
gcloud run services logs read lasso --region us-central1 --limit 50

# View admin console
open https://lasso.fm/admin
```
