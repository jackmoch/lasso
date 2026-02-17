# CI/CD Deployment Workflows

This document describes the automated deployment workflows for the Lasso application across different environments.

## Overview

Lasso uses GitHub Actions for automated deployments to Google Cloud Run across three environments:

| Environment | Service Name | Trigger | Branch/Tag |
|-------------|-------------|---------|------------|
| **Development** | `lasso-dev` | Automatic on push | `develop` |
| **Staging** | `lasso-staging` | Automatic on push | `release/**` |
| **Production** | `lasso` | Automatic on tag | `v*.*.*` |

## Workflows

### 1. Development Deployment (`deploy-dev.yml`)

**Trigger:** Automatic on every push to `develop` branch

**Purpose:** Rapid iteration and testing of new features in a dev environment

**Steps:**
1. Build Docker image tagged with commit SHA and `dev-latest`
2. Push to Google Container Registry
3. Deploy to `lasso-dev` service on Cloud Run
4. Run smoke tests:
   - Health endpoint check
   - Home page check
   - Static assets (CSS/JS) check

**Configuration:**
- Memory: 512Mi
- CPU: 1
- Max instances: 3
- Min instances: 0
- Log level: `debug`
- Environment: `development`

**Manual trigger:**
```bash
gh workflow run deploy-dev.yml
```

---

### 2. Staging Deployment (`deploy-staging.yml`)

**Trigger:** Automatic on push to `release/**` branches

**Purpose:** Pre-production testing and validation before production release

**Steps:**
1. Extract version from release branch name (e.g., `release/1.0.0`)
2. Build Docker image tagged with commit SHA, version, and `staging-latest`
3. Push to Google Container Registry
4. Deploy to `lasso-staging` service on Cloud Run
5. Run comprehensive tests:
   - Health endpoint check
   - Home page check
   - Static assets with content validation
   - Security headers verification
   - Performance checks (response time)
6. Post deployment comment to commit with deployment details

**Configuration:**
- Memory: 512Mi
- CPU: 1
- Max instances: 5
- Min instances: 0
- Log level: `info`
- Environment: `staging`

**Manual trigger:**
```bash
gh workflow run deploy-staging.yml
```

**Post-deployment:**
- Perform manual testing on staging
- Verify OAuth flow works end-to-end
- Test session management features
- If all tests pass, merge release branch to `main`

---

### 3. Production Deployment (`deploy-prod.yml`)

**Trigger:** Automatic on pushing version tags (e.g., `v1.0.0`)

**Purpose:** Deploy validated releases to production

**Steps:**
1. Extract version from tag
2. Verify version matches `VERSION` file
3. Check if Docker image already exists in GCR
4. Build new image if needed, or tag existing image
5. Get current revision (for potential rollback)
6. Deploy to `lasso` service **without routing traffic** initially
7. Run health checks on new revision (using revision-specific URL)
8. Route 100% traffic to new revision
9. Run post-deployment smoke tests on production URL
10. Update GitHub release with deployment information

**Configuration:**
- Memory: 512Mi
- CPU: 1
- Max instances: 20
- Min instances: 1 (always-on for production)
- Log level: `info`
- Environment: `production`

**Manual trigger:**
```bash
gh workflow run deploy-prod.yml --field version=v1.0.0
```

**Rollback:**
If deployment fails, automatic rollback attempts to restore previous revision:
```bash
gcloud run services update-traffic lasso \
  --region us-central1 \
  --to-revisions PREVIOUS_REVISION=100
```

**Manual rollback:**
See deployment summary for rollback command with specific revision name.

---

## GitHub Environments

The workflows use GitHub Environments for approval gates and secrets management:

### Development Environment
- No approval required
- Automatic deployment on every push to `develop`

### Staging Environment
- Optional approval gate (can be configured in GitHub Settings)
- Deploys on release branches

### Production Environment
- **Approval required** (configure in GitHub Settings → Environments → production)
- Protected environment
- Deploy only on version tags

**To configure approval gates:**
1. Go to repository Settings → Environments
2. Select environment (e.g., `production`)
3. Enable "Required reviewers"
4. Add team members who can approve deployments

---

## Required GitHub Secrets

All workflows require the following repository secrets:

| Secret Name | Description | Example |
|------------|-------------|---------|
| `GCP_SA_KEY` | GCP Service Account JSON key | `{"type": "service_account"...}` |

**Additional secrets in GCP Secret Manager:**
- `lastfm-api-key` - Last.fm API key
- `lastfm-api-secret` - Last.fm API secret
- `session-secret` - Application session encryption key

**To set up GCP_SA_KEY:**
1. Create service account in GCP with Cloud Run Admin and Storage Admin roles
2. Download JSON key
3. Add to GitHub: Settings → Secrets and variables → Actions → New repository secret
4. Name: `GCP_SA_KEY`, Value: Paste entire JSON content

---

## Smoke Tests

Each deployment runs automated smoke tests to verify functionality:

### Basic Checks (All Environments)
- **Health Endpoint:** `GET /health` returns 200
- **Home Page:** `GET /` returns 200
- **CSS Asset:** `GET /css/tailwind.css` returns 200
- **JS Asset:** `GET /js/main.js` returns 200

### Content Validation (Staging/Production)
- CSS contains Tailwind classes (`tw-` prefix)
- JS contains ClojureScript output (`shadow$provide`)

### Security Checks (Staging)
- `X-Frame-Options` header present
- `Content-Security-Policy` header present

### Performance Checks (Staging)
- Response time < 2 seconds (warning if exceeded)

### Production Checks
- Response time < 3 seconds
- Traffic routing verification
- Revision health before traffic switch

---

## Deployment Process

### Standard Feature Development Flow

1. **Develop Feature**
   ```bash
   git checkout develop
   git checkout -b feature/my-feature
   # ... work on feature ...
   git commit -am "feat: add my feature"
   git push origin feature/my-feature
   ```

2. **Create PR to develop**
   ```bash
   gh pr create --base develop --title "feat: add my feature"
   ```

3. **Merge PR** → Automatic deployment to `lasso-dev` 🚀

4. **Test on Dev**
   - Verify feature works on dev environment
   - URL: https://lasso-dev-[PROJECT_ID].us-central1.run.app

### Release Flow

5. **Create Release Branch**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b release/1.0.0
   ```

6. **Prepare Release**
   ```bash
   echo "1.0.0" > VERSION
   # Update CHANGELOG.md with release notes
   git commit -am "chore(release): bump version to 1.0.0"
   git push origin release/1.0.0
   ```

7. **Automatic Staging Deployment** 🚀
   - Push to `release/1.0.0` triggers `deploy-staging.yml`
   - Service deployed to `lasso-staging`
   - Smoke tests run automatically

8. **Test on Staging**
   - Perform comprehensive manual testing
   - Test OAuth flow end-to-end
   - Verify session management
   - Check error handling

9. **Merge to Main**
   ```bash
   gh pr create --base main --title "Release v1.0.0"
   # Get approval and merge
   ```

10. **Tag Release**
    ```bash
    git checkout main
    git pull origin main
    git tag v1.0.0
    git push origin v1.0.0
    ```

11. **Automatic Production Deployment** 🚀
    - Tag triggers `deploy-prod.yml`
    - Deployment waits for approval (if configured)
    - New revision created without traffic
    - Health checks run on new revision
    - Traffic switches to new revision
    - Post-deployment tests verify production

12. **Sync develop**
    ```bash
    git checkout develop
    git merge main
    git push origin develop
    ```

---

## Monitoring Deployments

### View Workflow Status
```bash
# List recent workflow runs
gh run list --workflow=deploy-dev.yml

# Watch specific workflow run
gh run watch RUN_ID
```

### View Deployment Logs
```bash
# GitHub Actions logs
gh run view RUN_ID --log

# Cloud Run logs
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=lasso-dev" \
  --limit 50 \
  --project lasso-scrobbler-0667
```

### Service URLs
- **Dev:** https://lasso-dev-242166835352.us-central1.run.app
- **Staging:** https://lasso-staging-242166835352.us-central1.run.app
- **Production:** https://lasso-242166835352.us-central1.run.app

*(Replace project ID with your actual GCP project ID)*

---

## Troubleshooting

### Deployment Fails

**Check workflow logs:**
```bash
gh run view --log
```

**Common issues:**
- **Authentication failure:** Verify `GCP_SA_KEY` secret is valid
- **Insufficient permissions:** Service account needs Cloud Run Admin role
- **Secrets not found:** Verify secrets exist in Secret Manager
- **Smoke tests fail:** Check Cloud Run logs for application errors

### Rollback Production

If production deployment succeeds but issues are discovered later:

```bash
# List revisions
gcloud run revisions list \
  --service lasso \
  --region us-central1

# Rollback to previous revision
gcloud run services update-traffic lasso \
  --region us-central1 \
  --to-revisions PREVIOUS_REVISION_NAME=100
```

### Manual Deployment

If automated workflows fail, deploy manually:

```bash
# Build and push image
docker build -t gcr.io/lasso-scrobbler-0667/lasso:manual .
docker push gcr.io/lasso-scrobbler-0667/lasso:manual

# Deploy to Cloud Run
gcloud run deploy lasso-dev \
  --image gcr.io/lasso-scrobbler-0667/lasso:manual \
  --region us-central1 \
  --project lasso-scrobbler-0667
```

---

## Best Practices

1. **Always test on dev first** - Every change should go through dev before staging/prod
2. **Use release branches** - Create release branches for staging deployments
3. **Semantic versioning** - Follow semver (MAJOR.MINOR.PATCH) for version tags
4. **Meaningful commits** - Use conventional commits for clear deployment history
5. **Monitor after deploy** - Check logs and metrics after each deployment
6. **Keep secrets updated** - Rotate secrets regularly in Secret Manager
7. **Test rollback** - Periodically verify rollback process works
8. **Document changes** - Update CHANGELOG.md for every release

---

## Security

### Service Account Permissions
The GitHub Actions service account requires:
- `roles/run.admin` - Deploy to Cloud Run
- `roles/iam.serviceAccountUser` - Use Cloud Run service account
- `roles/storage.admin` - Push to Container Registry
- `roles/secretmanager.secretAccessor` - Access secrets

### Secret Management
- Secrets stored in GCP Secret Manager, not in code
- GitHub Actions references secrets via `--set-secrets` flag
- Never commit credentials to repository
- Rotate secrets regularly

### Network Security
- All environments use HTTPS only
- CORS configured for web application
- Rate limiting enabled
- Security headers enforced

---

## Related Documentation

- [Deployment Strategy](./DEPLOYMENT_STRATEGY.md) - Overall deployment approach
- [Google Cloud Setup](./GOOGLE_CLOUD_SETUP.md) - Initial GCP configuration
- [CLAUDE.md](../../CLAUDE.md) - Project overview and development workflow
