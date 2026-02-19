#!/usr/bin/env bash
# One-time setup: create Cloud Logging log-based metrics for Lasso.
# Run this once per GCP project. Safe to re-run (idempotent via delete+create).
#
# Usage:
#   ./scripts/monitoring/create-metrics.sh [PROJECT_ID]
#
# If PROJECT_ID is not provided, uses the current gcloud project.

set -euo pipefail

PROJECT="${1:-$(gcloud config get-value project)}"

if [[ -z "$PROJECT" ]]; then
  echo "ERROR: No GCP project set. Run: gcloud config set project YOUR_PROJECT" >&2
  exit 1
fi

echo "Creating log-based metrics in project: $PROJECT"
echo ""

create_metric() {
  local name="$1"
  local description="$2"
  local filter="$3"

  echo "Creating metric: $name"
  # Delete first to make idempotent; ignore error if doesn't exist
  gcloud logging metrics delete "$name" --project="$PROJECT" --quiet 2>/dev/null || true
  gcloud logging metrics create "$name" \
    --description="$description" \
    --log-filter="$filter" \
    --project="$PROJECT"
  echo "  ✓ $name created"
}

create_metric \
  "lasso_sessions_started" \
  "Number of following sessions started per hour" \
  'resource.type="cloud_run_revision" resource.labels.service_name="lasso" jsonPayload.message=~"Following session started"'

create_metric \
  "lasso_auth_events" \
  "Number of OAuth authentication events (logins) per hour" \
  'resource.type="cloud_run_revision" resource.labels.service_name="lasso" jsonPayload.message=~"OAuth callback"'

create_metric \
  "lasso_scrobble_events" \
  "Number of tracks scrobbled per hour" \
  'resource.type="cloud_run_revision" resource.labels.service_name="lasso" jsonPayload.message=~"Scrobbled track"'

create_metric \
  "lasso_errors" \
  "Error rate across all Lasso components" \
  'resource.type="cloud_run_revision" resource.labels.service_name="lasso" severity>="ERROR"'

echo ""
echo "All metrics created. View at:"
echo "  https://console.cloud.google.com/logs/metrics?project=$PROJECT"
echo ""
echo "To create the dashboard, run:"
echo "  gcloud monitoring dashboards create --config-from-file=scripts/monitoring/dashboard.json --project=$PROJECT"
