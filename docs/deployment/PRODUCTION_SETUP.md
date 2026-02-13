# Production Deployment Setup

This guide walks through setting up Lasso for production deployment on Google Cloud Run.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Last.fm API Setup](#lastfm-api-setup)
3. [Google Cloud Setup](#google-cloud-setup)
4. [Environment Configuration](#environment-configuration)
5. [Security Hardening](#security-hardening)
6. [Deployment](#deployment)
7. [Post-Deployment](#post-deployment)
8. [Monitoring](#monitoring)
9. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Accounts

- **Last.fm API Account**: For production API credentials
- **Google Cloud Platform Account**: For Cloud Run hosting
- **Domain** (optional but recommended): For custom domain and HTTPS

### Required Tools

```bash
# Google Cloud SDK
brew install google-cloud-sdk

# Docker (for local testing)
brew install docker

# Babashka (build tool)
brew install babashka
```

## Last.fm API Setup

### 1. Create API Application

1. Go to: https://www.last.fm/api/account/create
2. Fill in application details:
   - **Application Name**: Lasso (Production)
   - **Application Description**: Last.fm scrobble mirroring for Spotify Jam sessions
   - **Callback URL**: `https://your-domain.run.app/api/auth/callback`
3. Save your **API Key** and **API Secret**

### 2. Configure OAuth Callback

**Important:** The callback URL must match exactly:
- Protocol: `https://` (required for production)
- Domain: Your Cloud Run service URL or custom domain
- Path: `/api/auth/callback` (exact match)

Example valid callbacks:
```
https://lasso-abc123.run.app/api/auth/callback
https://app.yourdomain.com/api/auth/callback
```

## Google Cloud Setup

### 1. Create GCP Project

```bash
# Authenticate with Google Cloud
gcloud auth login

# Create new project
gcloud projects create your-project-id --name="Lasso Production"

# Set as active project
gcloud config set project your-project-id

# Enable required APIs
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable containerregistry.googleapis.com
```

### 2. Configure Cloud Run Region

```bash
# Set default region (choose closest to your users)
gcloud config set run/region us-central1

# Available regions:
# - us-central1 (Iowa, USA)
# - us-east1 (South Carolina, USA)
# - europe-west1 (Belgium)
# - asia-northeast1 (Tokyo)
```

### 3. Set Up Secret Manager (Recommended)

Store sensitive configuration in Google Secret Manager:

```bash
# Enable Secret Manager API
gcloud services enable secretmanager.googleapis.com

# Create secrets
echo -n "your-lastfm-api-secret" | \
  gcloud secrets create lastfm-api-secret --data-file=-

echo -n "your-session-secret-32-chars-min" | \
  gcloud secrets create session-secret --data-file=-

# Grant Cloud Run access to secrets
gcloud secrets add-iam-policy-binding lastfm-api-secret \
  --member="serviceAccount:$(gcloud projects describe your-project-id \
  --format='value(projectNumber)')-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding session-secret \
  --member="serviceAccount:$(gcloud projects describe your-project-id \
  --format='value(projectNumber)')-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

## Environment Configuration

### 1. Create Production Environment File

```bash
# Copy template
cp .env.production.template .env.production

# Edit with your production values
vim .env.production
```

### 2. Required Environment Variables

**Critical (must be set):**
```bash
LASTFM_API_KEY=your_production_api_key
LASTFM_API_SECRET=your_production_api_secret
OAUTH_CALLBACK_URL=https://your-domain.run.app/api/auth/callback
SESSION_SECRET=generate_with_openssl_rand_base64_32
```

**Important (should be set):**
```bash
ENVIRONMENT=production
LOG_LEVEL=info
LOG_FORMAT=json
SESSION_COOKIE_SECURE=true
RATE_LIMIT_ENABLED=true
CORS_ALLOWED_ORIGINS=https://your-domain.run.app
```

### 3. Generate Secure Session Secret

```bash
# Generate strong random secret
openssl rand -base64 32

# Copy output and set as SESSION_SECRET
```

## Security Hardening

### 1. Session Security

Ensure these settings in `.env.production`:

```bash
SESSION_COOKIE_SECURE=true        # HTTPS only
SESSION_COOKIE_HTTP_ONLY=true     # No JS access
SESSION_COOKIE_SAME_SITE=strict   # CSRF protection
SESSION_SECRET=<strong-random-32+chars>
```

### 2. CORS Configuration

**Production (strict):**
```bash
CORS_ALLOWED_ORIGINS=https://your-exact-domain.run.app
CORS_ALLOW_CREDENTIALS=true
```

**Development (permissive):**
```bash
CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000
```

### 3. Rate Limiting

Enable rate limiting to prevent abuse:

```bash
RATE_LIMIT_ENABLED=true
RATE_LIMIT_MAX_REQUESTS=100    # Per window
RATE_LIMIT_WINDOW_MS=60000     # 1 minute window
```

### 4. Security Headers

The application should set these headers (configured in code):

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'
```

### 5. Environment Variable Security

**DO:**
- ✅ Use Secret Manager for sensitive values
- ✅ Rotate secrets regularly (every 90 days)
- ✅ Use different secrets per environment
- ✅ Add `.env.production` to `.gitignore`

**DON'T:**
- ❌ Commit `.env.production` to Git
- ❌ Share secrets in Slack/Email
- ❌ Use the same SESSION_SECRET as development
- ❌ Use weak or short secrets

## Deployment

### 1. Build Production Artifacts

```bash
# Build frontend, CSS, and backend
bb build

# Verify build artifacts
ls -lh target/lasso.jar
ls -lh resources/public/js/main.js
ls -lh resources/public/css/tailwind.css
```

### 2. Test Production Build Locally

```bash
# Copy production env to .env
cp .env.production .env

# Run production build locally
java -jar target/lasso.jar

# Test in browser: http://localhost:8080
# Verify OAuth flow, session management, scrobbling
```

### 3. Build Docker Image

```bash
# Build image
docker build -t gcr.io/your-project-id/lasso:latest .

# Test Docker image locally
docker run -p 8080:8080 --env-file .env.production \
  gcr.io/your-project-id/lasso:latest

# Verify: http://localhost:8080
```

### 4. Push to Google Container Registry

```bash
# Configure Docker authentication
gcloud auth configure-docker

# Push image
docker push gcr.io/your-project-id/lasso:latest
```

### 5. Deploy to Cloud Run

**Option 1: Using Secret Manager (Recommended)**

```bash
gcloud run deploy lasso \
  --image gcr.io/your-project-id/lasso:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="ENVIRONMENT=production,LASTFM_API_KEY=your-api-key,OAUTH_CALLBACK_URL=https://lasso-xyz.run.app/api/auth/callback" \
  --set-secrets="LASTFM_API_SECRET=lastfm-api-secret:latest,SESSION_SECRET=session-secret:latest" \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10 \
  --min-instances 0
```

**Option 2: Using Environment Variables**

```bash
gcloud run deploy lasso \
  --image gcr.io/your-project-id/lasso:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="$(cat .env.production | grep -v '^#' | grep -v '^$' | tr '\n' ',' | sed 's/,$//')" \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10 \
  --min-instances 0
```

### 6. Configure Custom Domain (Optional)

```bash
# Map custom domain
gcloud run services update lasso \
  --platform managed \
  --region us-central1 \
  --update-env-vars OAUTH_CALLBACK_URL=https://app.yourdomain.com/api/auth/callback

# Add domain mapping
gcloud run domain-mappings create \
  --service lasso \
  --domain app.yourdomain.com \
  --region us-central1

# Update DNS records as instructed by Cloud Run
```

## Post-Deployment

### 1. Verify Deployment

```bash
# Get service URL
gcloud run services describe lasso \
  --platform managed \
  --region us-central1 \
  --format 'value(status.url)'

# Test health endpoint
curl https://your-service-url.run.app/health

# Test OAuth flow (in browser)
# 1. Click "Login with Last.fm"
# 2. Authorize on Last.fm
# 3. Verify redirect back to app
# 4. Check session is created
```

### 2. Update Last.fm Callback URL

Go to Last.fm API settings and update callback URL to match deployed URL:
```
https://your-actual-deployed-url.run.app/api/auth/callback
```

### 3. Test Full Workflow

1. **Login**: Test OAuth flow
2. **Start Session**: Enter target username, start following
3. **Scrobble**: Verify scrobbles are mirrored
4. **Pause/Resume**: Test session controls
5. **Stop**: Clean session stop
6. **Logout**: Clear session

### 4. Monitor Initial Traffic

```bash
# View logs
gcloud run services logs read lasso \
  --platform managed \
  --region us-central1 \
  --limit 50

# Monitor metrics
gcloud run services describe lasso \
  --platform managed \
  --region us-central1 \
  --format 'value(status.traffic)'
```

## Monitoring

### 1. Google Cloud Logging

View logs in Google Cloud Console:
```
https://console.cloud.google.com/logs/query?project=your-project-id
```

Filter for errors:
```
resource.type="cloud_run_revision"
resource.labels.service_name="lasso"
severity>=ERROR
```

### 2. Cloud Monitoring

Set up alerts for:
- **Error rate** > 5% (15 minutes)
- **Latency** > 2 seconds (p95, 5 minutes)
- **Instance count** > 8 (scaling threshold)

### 3. Custom Metrics

Monitor application-specific metrics:
- Active sessions count
- Scrobbles per minute
- Last.fm API error rate
- OAuth success/failure rate

### 4. Uptime Monitoring

Set up uptime checks:
```bash
gcloud monitoring uptime-check-configs create \
  --display-name="Lasso Health Check" \
  --resource-type=cloud-run-service \
  --http-check-path=/health
```

## Troubleshooting

### OAuth Callback Mismatch

**Symptom:** "Callback URL mismatch" error after Last.fm authorization

**Solution:**
1. Check `OAUTH_CALLBACK_URL` in Cloud Run environment
2. Verify it matches Last.fm API settings exactly
3. Ensure protocol is `https://` not `http://`
4. Redeploy after fixing:
   ```bash
   gcloud run services update lasso \
     --update-env-vars OAUTH_CALLBACK_URL=https://correct-url.run.app/api/auth/callback
   ```

### Session Secret Error

**Symptom:** "Invalid session" errors, sessions not persisting

**Solution:**
1. Ensure `SESSION_SECRET` is set and strong (32+ characters)
2. Verify secret hasn't changed (breaks existing sessions)
3. Check Secret Manager access if using secrets

### CORS Errors

**Symptom:** Browser console shows CORS errors

**Solution:**
1. Check `CORS_ALLOWED_ORIGINS` matches your domain exactly
2. Ensure protocol (https) matches
3. For custom domains, add both Cloud Run URL and custom domain:
   ```bash
   CORS_ALLOWED_ORIGINS=https://lasso-xyz.run.app,https://app.yourdomain.com
   ```

### Last.fm Rate Limiting

**Symptom:** 429 errors from Last.fm API

**Solution:**
1. Check `POLLING_INTERVAL_MS` is set to 20000 (20 seconds minimum)
2. Monitor number of active sessions (each polls separately)
3. Consider implementing back-off strategy

### High Memory Usage

**Symptom:** Cloud Run instances restarting due to OOM

**Solution:**
1. Increase memory allocation:
   ```bash
   gcloud run services update lasso --memory 1Gi
   ```
2. Monitor session count (each session holds state)
3. Implement session cleanup for inactive users

## Security Checklist

Before going live, verify:

- [ ] `SESSION_SECRET` is strong (32+ characters, random)
- [ ] `SESSION_COOKIE_SECURE=true` (HTTPS only)
- [ ] `OAUTH_CALLBACK_URL` uses HTTPS
- [ ] `CORS_ALLOWED_ORIGINS` is restrictive (exact domain)
- [ ] Rate limiting enabled
- [ ] Sensitive values in Secret Manager, not environment variables
- [ ] `.env.production` in `.gitignore`
- [ ] Security headers configured
- [ ] Latest dependencies installed
- [ ] Logs don't contain sensitive data
- [ ] Error messages don't expose internal details

## Rollback Procedure

If deployment has issues:

```bash
# List revisions
gcloud run revisions list \
  --service lasso \
  --platform managed \
  --region us-central1

# Route 100% traffic to previous revision
gcloud run services update-traffic lasso \
  --to-revisions REVISION_NAME=100 \
  --platform managed \
  --region us-central1
```

## Cost Optimization

### Cloud Run Pricing

- **CPU**: $0.00002400/vCPU-second
- **Memory**: $0.00000250/GiB-second
- **Requests**: $0.40/million requests
- **Free tier**: 2 million requests/month

### Optimization Tips

1. **Set min-instances=0**: Only pay when traffic exists
2. **Optimize cold starts**: Keep Docker image small (<500MB)
3. **Use caching**: Reduce Last.fm API calls where possible
4. **Monitor scaling**: Adjust max-instances based on usage

## Next Steps

After successful deployment:

1. **Monitor for 24 hours**: Watch logs and metrics
2. **Gather user feedback**: Test with real users
3. **Set up alerting**: Configure notification channels
4. **Document runbook**: Create incident response guide
5. **Plan for scaling**: Monitor usage patterns

## Resources

- [Google Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Last.fm API Documentation](https://www.last.fm/api)
- [Lasso Development Guide](../development/DEVELOPMENT.md)
- [Security Best Practices](./SECURITY.md)
