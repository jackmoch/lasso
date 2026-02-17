#!/usr/bin/env bash
# Production Deployment Script for Lasso
# Deploys to Google Cloud Run with proper configuration
#
# Usage:
#   ./scripts/deploy-production.sh [options]
#
# Options:
#   --project PROJECT_ID    GCP project ID (required)
#   --region REGION        Cloud Run region (default: us-central1)
#   --domain DOMAIN        Custom domain for OAuth callback
#   --build-only           Build image but don't deploy
#   --skip-build           Deploy existing image without building
#   --help                 Show this help message

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
REGION="us-central1"
BUILD_IMAGE=true
DEPLOY_SERVICE=true
PROJECT_ID=""
DOMAIN=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --project)
      PROJECT_ID="$2"
      shift 2
      ;;
    --region)
      REGION="$2"
      shift 2
      ;;
    --domain)
      DOMAIN="$2"
      shift 2
      ;;
    --build-only)
      DEPLOY_SERVICE=false
      shift
      ;;
    --skip-build)
      BUILD_IMAGE=false
      shift
      ;;
    --help)
      grep '^#' "$0" | tail -n +3 | sed 's/^# //'
      exit 0
      ;;
    *)
      echo -e "${RED}Error: Unknown option $1${NC}"
      exit 1
      ;;
  esac
done

# Validate required arguments
if [ -z "$PROJECT_ID" ]; then
  echo -e "${RED}Error: --project is required${NC}"
  echo "Usage: $0 --project PROJECT_ID [options]"
  exit 1
fi

# Set project
gcloud config set project "$PROJECT_ID"

# Determine OAuth callback URL
if [ -n "$DOMAIN" ]; then
  OAUTH_CALLBACK="https://${DOMAIN}/api/auth/callback"
else
  # Use Cloud Run service URL
  SERVICE_URL=$(gcloud run services describe lasso \
    --region "$REGION" \
    --format 'value(status.url)' 2>/dev/null || echo "")

  if [ -n "$SERVICE_URL" ]; then
    OAUTH_CALLBACK="${SERVICE_URL}/api/auth/callback"
  else
    OAUTH_CALLBACK="https://lasso-${PROJECT_ID}.run.app/api/auth/callback"
    echo -e "${YELLOW}Warning: Service not deployed yet, using predicted URL${NC}"
  fi
fi

echo -e "${GREEN}=== Lasso Production Deployment ===${NC}"
echo "Project:  $PROJECT_ID"
echo "Region:   $REGION"
echo "Callback: $OAUTH_CALLBACK"
echo ""

# Build image
if [ "$BUILD_IMAGE" = true ]; then
  echo -e "${GREEN}Step 1/3: Building production artifacts...${NC}"

  # Run babashka build
  if command -v bb &> /dev/null; then
    bb build
  else
    echo -e "${YELLOW}Warning: babashka not found, skipping local build${NC}"
    echo "Make sure artifacts are built before deploying"
  fi

  echo -e "${GREEN}Step 2/3: Building Docker image...${NC}"

  # Build and push via Cloud Build
  gcloud builds submit \
    --tag "gcr.io/${PROJECT_ID}/lasso:latest" \
    --timeout 10m

  echo -e "${GREEN}✓ Docker image built and pushed${NC}"
else
  echo -e "${YELLOW}Skipping build step${NC}"
fi

# Deploy to Cloud Run
if [ "$DEPLOY_SERVICE" = true ]; then
  echo -e "${GREEN}Step 3/3: Deploying to Cloud Run...${NC}"

  # Check if secrets exist
  SECRET_FLAGS=""
  if gcloud secrets describe lastfm-api-secret &>/dev/null && \
     gcloud secrets describe session-secret &>/dev/null; then
    echo "✓ Using Secret Manager for sensitive values"
    SECRET_FLAGS="--set-secrets=LASTFM_API_SECRET=lastfm-api-secret:latest,SESSION_SECRET=session-secret:latest"
  else
    echo -e "${YELLOW}Warning: Secrets not found in Secret Manager${NC}"
    echo "Some environment variables must be set manually after deployment"
  fi

  # Deploy service
  gcloud run deploy lasso \
    --image "gcr.io/${PROJECT_ID}/lasso:latest" \
    --platform managed \
    --region "$REGION" \
    --allow-unauthenticated \
    --memory 512Mi \
    --cpu 1 \
    --max-instances 10 \
    --min-instances 0 \
    --set-env-vars="ENVIRONMENT=production,LASTFM_API_BASE_URL=https://ws.audioscrobbler.com,LASTFM_AUTH_URL=https://www.last.fm,OAUTH_CALLBACK_URL=${OAUTH_CALLBACK},LOG_LEVEL=info,LOG_FORMAT=json,SESSION_COOKIE_SECURE=true,RATE_LIMIT_ENABLED=true,POLLING_INTERVAL_MS=20000" \
    $SECRET_FLAGS

  echo -e "${GREEN}✓ Service deployed${NC}"

  # Get service URL
  SERVICE_URL=$(gcloud run services describe lasso \
    --region "$REGION" \
    --format 'value(status.url)')

  echo ""
  echo -e "${GREEN}=== Deployment Complete ===${NC}"
  echo "Service URL:    $SERVICE_URL"
  echo "OAuth Callback: ${OAUTH_CALLBACK}"
  echo ""
  echo "Next steps:"
  echo "1. Update Last.fm API settings with callback URL:"
  echo "   ${OAUTH_CALLBACK}"
  echo "2. Test OAuth flow: ${SERVICE_URL}"
  echo "3. Monitor logs: gcloud run services logs read lasso --region ${REGION}"

  # Health check
  echo ""
  echo "Performing health check..."
  sleep 5

  if curl -f -s "${SERVICE_URL}/health" > /dev/null; then
    echo -e "${GREEN}✓ Health check passed${NC}"
  else
    echo -e "${RED}✗ Health check failed${NC}"
    echo "Check logs for errors"
    exit 1
  fi

else
  echo -e "${YELLOW}Skipping deployment step${NC}"
fi

echo ""
echo -e "${GREEN}All done! 🎉${NC}"
