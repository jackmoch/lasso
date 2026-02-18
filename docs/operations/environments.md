# Lasso Environments

This document describes the three deployment environments, how they map to GCP infrastructure, Firestore data isolation, and the manual setup steps required before first deployment.

---

## Overview

| Environment  | Branch         | Cloud Run Service  | Trigger                          | Firestore Prefix |
|--------------|----------------|--------------------|----------------------------------|------------------|
| Development  | `develop`      | `lasso-dev`        | Push to `develop`                | `dev-`           |
| Staging      | `release/**`   | `lasso-staging`    | Push to `release/**` branch      | `staging-`       |
| Production   | `main` (tags)  | `lasso`            | Version tag or `workflow_dispatch` | *(none)*       |

All three services run in the same GCP project (`lasso-scrobbler-0667`) and same region (`us-central1`). Data isolation is achieved via Firestore collection prefixes — not separate databases.

### Service URLs

| Environment | URL |
|-------------|-----|
| Development | `https://lasso-dev-ngqcsb2bpa-uc.a.run.app` |
| Staging     | `https://lasso-staging-ngqcsb2bpa-uc.a.run.app` |
| Production  | `https://lasso-ngqcsb2bpa-uc.a.run.app` |

---

## Firestore Data Isolation

Lasso uses Cloud Firestore for persistent user profiles, session history, and remember-me tokens. All three environments share **one Firestore database** (Firestore Native mode, `us-central1`). Data is isolated via a collection prefix derived from the `ENVIRONMENT` env var:

| Environment | `ENVIRONMENT` value | Collection prefix | Example collection     |
|-------------|---------------------|-------------------|------------------------|
| Development | `development`       | `dev-`            | `dev-users`, `dev-remember_tokens` |
| Staging     | `staging`           | `staging-`        | `staging-users`, `staging-remember_tokens` |
| Production  | `production`        | *(none)*          | `users`, `remember_tokens` |

The prefix is applied automatically in `lasso.firestore.client` — no code changes are needed when deploying to different environments.

### Why Prefixes Instead of Separate Databases?

- Firestore charges per database (one free per project). Prefixes avoid extra cost.
- Simple operations: single gcloud project, single set of IAM permissions.
- Tradeoff: accidental cross-environment data access is prevented by convention, not hard isolation.

If hard isolation becomes necessary (compliance, security), migrate to separate GCP projects.

---

## Firestore Collections

```
{prefix}users (collection)
└── {username} (document)
    ├── username: string
    ├── encrypted_session_key: string
    ├── first_seen: long (epoch ms)
    ├── last_seen: long (epoch ms)
    ├── last_target: string (optional)
    ├── total_scrobbles: integer
    └── sessions/ (subcollection)
        └── {fs-session-id} (document)
            ├── id: string
            ├── target_username: string
            ├── started_at: long (epoch ms)
            ├── ended_at: long | null
            ├── scrobble_count: integer
            └── state: "active" | "completed" | "stopped"

{prefix}remember_tokens (collection)
└── {token} (document)
    ├── username: string
    ├── encrypted_session_key: string
    ├── created_at: long (epoch ms)
    └── expires_at: long (epoch ms, 90 days from created_at)
```

---

## GCP Setup (Manual, One-Time)

These steps must be performed **before the first deployment** to a given environment. They are not automated by CI.

### 1. Create Firestore Database

```bash
# Creates a Firestore Native-mode database in the same region as Cloud Run.
# Only needs to be done once per GCP project.
gcloud firestore databases create \
  --region=us-central1 \
  --project=lasso-scrobbler-0667
```

Verify it exists:
```bash
gcloud firestore databases list --project=lasso-scrobbler-0667
```

### 2. Grant Firestore Access to the Cloud Run Service Account

Each Cloud Run service runs under a service account. That account needs the `roles/datastore.user` role to read/write Firestore.

```bash
# Find the service account (it's the Compute Engine default SA unless a custom SA was set)
gcloud run services describe lasso-dev \
  --region=us-central1 \
  --project=lasso-scrobbler-0667 \
  --format='value(spec.template.spec.serviceAccountName)'

# Grant Firestore access (replace SA_EMAIL with the value above)
gcloud projects add-iam-policy-binding lasso-scrobbler-0667 \
  --member="serviceAccount:SA_EMAIL" \
  --role="roles/datastore.user"
```

Repeat for `lasso-staging` and `lasso` (production) service accounts.

### 3. Local Development Authentication

For local development, Firestore uses Application Default Credentials (ADC). Run this once per developer machine:

```bash
gcloud auth application-default login
```

No service account key file is needed. The app will detect ADC automatically.

If you don't run this, Firestore will be unavailable locally — the app still works (OAuth, scrobbling, session management all function), but persistence features (remember-me, profile page, session history) will be disabled with a warning log.

### 4. Verify `GOOGLE_CLOUD_PROJECT` is Propagated

All deploy jobs in CI set `GOOGLE_CLOUD_PROJECT=lasso-scrobbler-0667` in the Cloud Run environment. Verify with:

```bash
gcloud run services describe lasso-dev \
  --region=us-central1 \
  --format='value(spec.template.spec.containers[0].env)'
```

---

## Environment Variables by Environment

| Variable | Development | Staging | Production |
|---|---|---|---|
| `ENVIRONMENT` | `development` | `staging` | `production` |
| `GOOGLE_CLOUD_PROJECT` | `lasso-scrobbler-0667` | `lasso-scrobbler-0667` | `lasso-scrobbler-0667` |
| `LOG_LEVEL` | `debug` | `info` | `info` |
| `OAUTH_CALLBACK_URL` | GH env secret (dev) | GH env secret (staging) | GH env secret (prod) |
| `POLLING_INTERVAL_MS` | 20000 | 20000 | 20000 |

Secrets (`LASTFM_API_KEY`, `LASTFM_API_SECRET`, `SESSION_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`) are managed in GCP Secret Manager and referenced in `--set-secrets` in CI workflows.

---

## Smoke Tests

After each deployment, CI runs smoke tests. The `/health` endpoint returns:

```json
{ "status": "ok", "firestore": "ok" }
```

- `firestore: "ok"` — Firestore client initialised successfully
- `firestore: "unavailable"` — Firestore unreachable (credentials missing or DB not created)

**Dev:** Firestore unavailability is a warning (deployment proceeds).
**Staging:** Firestore unavailability is a failure (deployment blocked). Staging must be production-equivalent.
**Production:** Firestore unavailability is detected by the health check after traffic shift; auto-rollback triggers if the health endpoint returns non-200.

---

## Monitoring & Logs

```bash
# Tail logs for any service
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=lasso-dev" \
  --limit 50 \
  --project lasso-scrobbler-0667

# Metrics dashboard
# https://console.cloud.google.com/run/detail/us-central1/lasso-dev/metrics?project=lasso-scrobbler-0667
```
