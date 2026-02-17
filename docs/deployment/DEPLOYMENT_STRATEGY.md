# Deployment Strategy

> **Unified approach for Git workflow, CI/CD, versioning, artifact management, and deployments**

This document defines how Lasso manages code, builds artifacts, versions releases, and deploys to environments. It unifies our Gitflow branching strategy with CI/CD pipelines, artifact tagging, and deployment processes.

---

## Overview

### Design Principles

1. **Traceability**: Every deployed artifact can be traced back to exact source code (git commit, branch, version)
2. **Immutability**: Build once, deploy many times (same artifact moves through environments)
3. **Automation**: Minimize manual steps, maximize CI/CD automation
4. **Safety**: Clear separation between development, staging, and production
5. **Semantic Versioning**: Human-readable versions that communicate intent

### Key Components

- **Gitflow**: Branch-based development workflow
- **Semantic Versioning**: VERSION file controls releases
- **GitHub Actions**: Automated builds, tests, and deployments
- **Google Container Registry (GCR)**: Centralized artifact storage
- **Google Cloud Run**: Deployment target for all environments

---

## Branch → Environment Mapping

| Branch Pattern | Environment | Auto-Deploy | Artifact Tag Pattern | Domain |
|---------------|-------------|-------------|---------------------|---------|
| `develop` | Development | ✅ Yes | `dev`, `dev-{sha}` | `lasso-dev-*.run.app` |
| `release/*` | Staging | ✅ Yes | `v{version}-rc.{n}`, `staging` | `lasso-staging-*.run.app` |
| `main` | Production | ⚠️ Manual | `v{version}`, `latest` | `lasso.app` (custom domain) |
| `feature/*` | (PR preview) | ❌ No | `pr-{number}` | N/A |

### Environment Descriptions

**Development (develop branch)**
- **Purpose**: Integration testing, daily development work
- **Stability**: Unstable, frequent changes
- **Data**: Test data, mock Last.fm API
- **Access**: Internal team only
- **Deployment**: Automatic on merge to `develop`

**Staging (release/* branches)**
- **Purpose**: Pre-production testing, release validation
- **Stability**: Stable, release candidate testing
- **Data**: Production-like test data, real Last.fm API
- **Access**: Internal team + beta testers
- **Deployment**: Automatic on push to `release/*`

**Production (main branch)**
- **Purpose**: Live user-facing application
- **Stability**: Highly stable, tagged releases only
- **Data**: Real user data, real Last.fm API
- **Access**: Public
- **Deployment**: Manual trigger after validation

---

## Versioning Strategy

### Semantic Versioning (SemVer)

We use **Semantic Versioning 2.0.0**: `MAJOR.MINOR.PATCH`

- **MAJOR** (1.0.0): Breaking changes, major new features
- **MINOR** (0.1.0): New features, backwards compatible
- **PATCH** (0.0.1): Bug fixes, backwards compatible

**Pre-release suffixes:**
- `-rc.{n}`: Release candidate (e.g., `0.4.0-rc.1`)
- `-beta.{n}`: Beta testing (e.g., `0.4.0-beta.1`)
- `-alpha.{n}`: Alpha/internal testing (e.g., `0.4.0-alpha.1`)

### VERSION File

- Single source of truth: `/VERSION`
- Plain text file containing only the version number (no `v` prefix)
- Example: `0.4.0`

### Version Lifecycle

```
develop         release/0.4.0              main
  │                  │                       │
  │                  │                       │
  ├─ Work on        ├─ Create release       ├─ Merge release
  │  features        │  branch               │  to main
  │                  │                       │
  │                  ├─ Bump VERSION to     ├─ VERSION: 0.4.0
  │  VERSION: 0.3.0  │  0.4.0-rc.1          │  Tag: v0.4.0
  │                  │                       │
  │                  ├─ Test & fix          ├─ Deploy to
  │                  │  0.4.0-rc.2          │  production
  │                  │                       │
  │                  ├─ Final: 0.4.0        └─ Merge back
  │                  │                          to develop
  │                  │
```

---

## Artifact Tagging Strategy

### Docker Images (Google Container Registry)

All Docker images stored at: `gcr.io/lasso-scrobbler-0667/lasso`

#### Tag Patterns

| Tag Pattern | Source | Example | Purpose |
|------------|--------|---------|---------|
| `latest` | main | `latest` | Points to most recent production release |
| `v{version}` | main | `v0.4.0` | Immutable production release |
| `v{version}-rc.{n}` | release/* | `v0.4.0-rc.1` | Release candidate for staging |
| `dev` | develop | `dev` | Latest development build (mutable) |
| `dev-{sha}` | develop | `dev-a1b2c3d` | Specific development build (immutable) |
| `pr-{number}` | feature/* PR | `pr-123` | PR preview build |

#### Multi-tagging Strategy

When building artifacts, we apply **multiple tags** for flexibility:

**Production build (main branch):**
```bash
docker tag image gcr.io/lasso-scrobbler-0667/lasso:v0.4.0
docker tag image gcr.io/lasso-scrobbler-0667/lasso:latest
```

**Staging build (release/0.4.0 branch):**
```bash
docker tag image gcr.io/lasso-scrobbler-0667/lasso:v0.4.0-rc.1
docker tag image gcr.io/lasso-scrobbler-0667/lasso:staging
```

**Development build (develop branch):**
```bash
docker tag image gcr.io/lasso-scrobbler-0667/lasso:dev
docker tag image gcr.io/lasso-scrobbler-0667/lasso:dev-a1b2c3d
```

### JAR Artifacts (GitHub Artifacts)

Backend JAR files stored as GitHub Actions artifacts:

- **Name pattern**: `lasso-{version}.jar` or `lasso-{version}-{sha}.jar`
- **Storage**: GitHub Actions artifacts (7-day retention)
- **Usage**: Bundled in Docker image, not deployed directly

---

## CI/CD Workflows

### 1. Continuous Integration (ci.yml)

**Triggers:**
- Push to `main` or `develop`
- Pull requests to `main` or `develop`

**Jobs:**
1. **lint-and-build**
   - Lint Clojure code
   - Run backend tests (Clojure)
   - Run frontend tests (ClojureScript)
   - Generate test coverage
   - Build frontend (shadow-cljs)
   - Build CSS (Tailwind)
   - Build backend (uberjar)
   - Upload artifacts (JAR, JS, CSS, coverage)

2. **docker-build**
   - Build Docker image
   - Tag as `lasso:ci-{sha}`
   - Upload as artifact (3-day retention)
   - **NOT pushed to GCR** (only for PR validation)

3. **pr-status-comment**
   - Post PR comment with build results

**Artifacts Produced:**
- ✅ Build validation only
- ❌ No registry publishing (yet)

### 2. Continuous Deployment (deploy-*.yml) - NEW

We'll create three new deployment workflows:

#### 2a. Deploy to Development (deploy-dev.yml)

**Trigger:** Push to `develop` (after CI passes)

**Steps:**
1. Download artifacts from CI workflow
2. Build Docker image
3. Tag image:
   - `gcr.io/lasso-scrobbler-0667/lasso:dev`
   - `gcr.io/lasso-scrobbler-0667/lasso:dev-{sha}`
4. Push to GCR
5. Deploy to Cloud Run (lasso-dev)
6. Run smoke tests

**Environment:** Development
- Service: `lasso-dev`
- URL: Auto-generated Cloud Run URL
- Config: Mock Last.fm API, test data

#### 2b. Deploy to Staging (deploy-staging.yml)

**Trigger:** Push to `release/*` branches (after CI passes)

**Steps:**
1. Extract version from VERSION file (e.g., `0.4.0-rc.1`)
2. Download artifacts from CI workflow
3. Build Docker image
4. Tag image:
   - `gcr.io/lasso-scrobbler-0667/lasso:v{version}` (e.g., `v0.4.0-rc.1`)
   - `gcr.io/lasso-scrobbler-0667/lasso:staging`
5. Push to GCR
6. Deploy to Cloud Run (lasso-staging)
7. Run integration tests against real Last.fm API
8. Post test results to release PR

**Environment:** Staging
- Service: `lasso-staging`
- URL: Auto-generated Cloud Run URL
- Config: Real Last.fm API, test credentials

#### 2c. Deploy to Production (deploy-prod.yml)

**Trigger:** Manual workflow_dispatch (after release merged to main)

**Steps:**
1. Extract version from VERSION file (e.g., `0.4.0`)
2. Verify git tag exists (`v{version}`)
3. Download staging artifacts OR rebuild from main
4. Build Docker image
5. Tag image:
   - `gcr.io/lasso-scrobbler-0667/lasso:v{version}` (immutable)
   - `gcr.io/lasso-scrobbler-0667/lasso:latest` (mutable pointer)
6. Push to GCR
7. Deploy to Cloud Run (lasso-prod) - **requires manual approval**
8. Run smoke tests
9. Post deployment notification (Slack/email)

**Environment:** Production
- Service: `lasso-prod`
- URL: `lasso.app` (custom domain)
- Config: Real Last.fm API, production credentials

### 3. Automated Release (release.yml) - EXISTING

**Trigger:** VERSION file change on `main`

**Steps:**
1. Read VERSION file
2. Check if tag `v{version}` exists
3. Extract changelog for version
4. Create git tag `v{version}`
5. Create GitHub Release

**No changes needed** - already works well!

---

## Complete Deployment Flow

### Feature Development → Production

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FEATURE DEVELOPMENT                                           │
├─────────────────────────────────────────────────────────────────┤
│ Branch: feature/sprint-8-deployment                             │
│ Actions:                                                         │
│   - Write code, commit changes                                  │
│   - Push to GitHub                                              │
│   - Create PR to develop                                        │
│ CI Actions:                                                      │
│   - Run ci.yml (lint, test, build)                             │
│   - Docker image built but NOT pushed                           │
│   - PR comment with results                                     │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. DEVELOPMENT ENVIRONMENT                                       │
├─────────────────────────────────────────────────────────────────┤
│ Branch: develop                                                  │
│ Trigger: Merge PR to develop                                    │
│ Actions:                                                         │
│   - ci.yml runs (validate)                                      │
│   - deploy-dev.yml runs (deploy)                                │
│ Artifacts:                                                       │
│   - gcr.io/.../lasso:dev                                        │
│   - gcr.io/.../lasso:dev-{sha}                                  │
│ Deployed to: lasso-dev (Cloud Run)                              │
│ Testing: Integration testing, daily QA                          │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. RELEASE PREPARATION                                           │
├─────────────────────────────────────────────────────────────────┤
│ Branch: release/0.4.0 (created from develop)                   │
│ Actions:                                                         │
│   - Update VERSION: 0.4.0-rc.1                                  │
│   - Update CHANGELOG.md                                         │
│   - Commit & push                                               │
│   - Create PR to main                                           │
│ CI Actions:                                                      │
│   - ci.yml runs (validate)                                      │
│   - deploy-staging.yml runs (deploy)                            │
│ Artifacts:                                                       │
│   - gcr.io/.../lasso:v0.4.0-rc.1                               │
│   - gcr.io/.../lasso:staging                                    │
│ Deployed to: lasso-staging (Cloud Run)                          │
│ Testing: Pre-production validation, real Last.fm API           │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. RELEASE FINALIZATION                                          │
├─────────────────────────────────────────────────────────────────┤
│ Branch: release/0.4.0                                            │
│ Actions:                                                         │
│   - Fix bugs found in staging (commit to release branch)        │
│   - Re-deploy to staging (0.4.0-rc.2, rc.3, etc.)              │
│   - Final: Update VERSION to 0.4.0 (remove -rc suffix)         │
│   - Merge PR to main                                            │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. PRODUCTION RELEASE                                            │
├─────────────────────────────────────────────────────────────────┤
│ Branch: main                                                     │
│ Trigger: Merge release/0.4.0 to main                            │
│ Automated Actions:                                               │
│   - ci.yml runs (final validation)                              │
│   - release.yml runs:                                           │
│     * Creates git tag v0.4.0                                    │
│     * Creates GitHub Release                                    │
│ Manual Actions:                                                  │
│   - Run deploy-prod.yml workflow (manual trigger)               │
│   - Approve production deployment                               │
│ Artifacts:                                                       │
│   - gcr.io/.../lasso:v0.4.0 (immutable)                        │
│   - gcr.io/.../lasso:latest (updated)                          │
│ Deployed to: lasso-prod (Cloud Run)                             │
│ Domain: lasso.app                                                │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. POST-RELEASE                                                  │
├─────────────────────────────────────────────────────────────────┤
│ Actions:                                                         │
│   - Merge main back to develop                                  │
│   - Update documentation (STATUS.md, NEXT.md, etc.)            │
│   - Close release PR                                            │
│   - Monitor production logs and metrics                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation Checklist

### Phase 1: Workflow Creation (Now)

- [ ] Create `deploy-dev.yml` workflow
- [ ] Create `deploy-staging.yml` workflow
- [ ] Create `deploy-prod.yml` workflow
- [ ] Update `ci.yml` to NOT push Docker images (validation only)
- [ ] Configure GCP service accounts for Cloud Run deployment
- [ ] Set up GitHub secrets for GCP authentication

### Phase 2: Environment Setup

- [ ] Create Cloud Run services:
  - [ ] `lasso-dev` (development)
  - [ ] `lasso-staging` (staging)
  - [ ] `lasso-prod` (production)
- [ ] Configure Secret Manager for each environment
- [ ] Set up environment-specific configuration

### Phase 3: Testing & Validation

- [ ] Test deploy-dev workflow with develop branch
- [ ] Create test release branch (release/0.4.0-rc.1)
- [ ] Test deploy-staging workflow
- [ ] Validate artifact tagging strategy
- [ ] Test production deployment workflow (dry-run)

### Phase 4: Documentation

- [ ] Update CLAUDE.md with new deployment process
- [ ] Update CONTRIBUTING.md with release process
- [ ] Create runbook for manual production deployments
- [ ] Document rollback procedures

---

## FAQ

### Q: Why not auto-deploy to production?

**A:** Production deployments require human judgment:
- Final validation of staging environment
- Coordination with stakeholders
- Monitoring for issues before rollout
- Ability to schedule deployments (avoid peak hours)

Manual trigger provides safety while still being fast.

### Q: What if I need to rollback production?

**A:** Since artifacts are immutable and tagged:
```bash
# Rollback to previous version
gcloud run deploy lasso-prod \
  --image gcr.io/lasso-scrobbler-0667/lasso:v0.3.0 \
  --region us-central1
```

### Q: How do I deploy a hotfix?

**A:** Create hotfix branch from main:
```bash
git checkout main
git checkout -b hotfix/0.4.1-critical-fix

# Make fix, update VERSION to 0.4.1
git commit -am "fix: critical security issue"

# Merge to main (triggers release.yml)
# Then manually trigger deploy-prod.yml

# Merge back to develop
git checkout develop
git merge hotfix/0.4.1-critical-fix
```

### Q: Can I preview a PR before merging?

**A:** Not automatically yet, but we can add:
- PR preview deployments using Cloud Run with unique URLs
- Tag pattern: `pr-{number}`
- Auto-cleanup after PR closes

### Q: How do I test locally with production config?

**A:**
```bash
# Build production image locally
docker build -t lasso:local .

# Run with production env (use .env.production)
docker run -p 8080:8080 --env-file .env.production lasso:local
```

---

## Version History

- **v1.0** (2026-02-14): Initial deployment strategy
- Sprint 8, Phase 3 (Deployment & Monitoring)

---

**Next Steps:** Implement Phase 1 (Workflow Creation) to establish automated deployment pipelines.
