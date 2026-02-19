# Cloud Monitoring Setup

One-time setup to create log-based metrics and a dashboard for Lasso production monitoring.

## Prerequisites

- `gcloud` CLI authenticated with the Lasso GCP project
- Cloud Logging API enabled
- Cloud Monitoring API enabled

## How It Works

When `ENVIRONMENT=production`, Lasso emits every log line as a JSON object:

```json
{"timestamp":"2026-02-19T12:00:00.000Z","severity":"INFO","logger":"lasso.session.manager","message":"Following session started"}
```

Cloud Logging ingests these automatically as `jsonPayload`. Log-based metrics filter on specific message patterns to produce time-series data.

## Metrics

| Metric | Log Filter | Measures |
|--------|-----------|---------|
| `lasso_sessions_started` | `jsonPayload.message =~ "Following session started"` | Sessions started per hour |
| `lasso_auth_events` | `jsonPayload.message =~ "OAuth callback"` | Logins per hour |
| `lasso_scrobble_events` | `jsonPayload.message =~ "Scrobbled track"` | Scrobbles submitted per hour |
| `lasso_errors` | `severity >= "ERROR"` | Error rate |

## Setup (run once)

```bash
# 1. Create the 4 log-based metrics
./scripts/monitoring/create-metrics.sh YOUR_GCP_PROJECT_ID

# 2. Create the Cloud Monitoring dashboard
gcloud monitoring dashboards create \
  --config-from-file=scripts/monitoring/dashboard.json \
  --project=YOUR_GCP_PROJECT_ID
```

## Verification

After deploying to production, check that JSON logs appear:

```bash
gcloud run services logs read lasso --region us-central1 --limit 10
```

You should see JSON objects with `severity` and `message` fields.

In Cloud Logging Explorer, filter with:
```
resource.type="cloud_run_revision"
jsonPayload.severity="INFO"
```

Metrics appear in **Cloud Monitoring → Metrics Explorer** under:
`Custom Metrics → Logging`

## Notes

- Development logs remain human-readable (plain text, not JSON)
- The `create-metrics.sh` script is idempotent — safe to re-run
- Metrics data retention: 6 weeks (Cloud Monitoring default)
