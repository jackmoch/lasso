# Sprint 9 Summary: Launch

**Status:** 🔄 In Progress
**Started:** 2026-02-18
**Branch:** hotfixes directly to `main` (CI/CD fixes) + `develop` (docs)
**Release:** v0.5.1

---

## Overview

Sprint 9 focuses on deploying Lasso to production and making it available to real users. Phase 1 (CI/CD fixes + deployment) is complete. Phase 2 (OAuth configuration) is the current blocker.

---

## Objectives

1. ✅ Fix CI/CD pipeline issues discovered during first real deployment
2. ✅ Deploy to staging successfully
3. ✅ Deploy to production successfully
4. ⏳ Configure OAuth callback URL so login works
5. ⏳ Validate end-to-end flow with real Last.fm credentials
6. ⏳ Set up monitoring and uptime alerting
7. ⏳ Update README and user documentation

---

## Accomplishments

### Phase 1: CI/CD Fixes and Production Deployment ✅

#### Environment/Secret Architecture Clarified

Discovered and explained the relationship between GitHub Environments, environment-level secrets, and repository-level secrets:

- Three GitHub Environments (`development`, `staging`, `production`) were already created but had no environment-specific secrets.
- `OAUTH_CALLBACK_URL` must differ per environment (each Cloud Run service has a unique URL).
- Solution: set `OAUTH_CALLBACK_URL` as an **environment-level secret** per GitHub Environment, not a repo-level secret. This way `environment: staging` jobs automatically get the staging URL and `environment: production` jobs get the production URL.

#### Fix 1: OAUTH_CALLBACK_URL Not Passed to Cloud Run

**Problem:** None of the deploy workflows passed `OAUTH_CALLBACK_URL` to Cloud Run as an environment variable.

**Fix:** Added `OAUTH_CALLBACK_URL=${{ secrets.OAUTH_CALLBACK_URL }}` to `--set-env-vars` in:
- `ci.yml` (dev deploy step)
- `ci.yml` (staging deploy step)
- `deploy-prod.yml` (production deploy step)

#### Fix 2: Staging Deploy 403 — IAM Permission Denied

**Problem:** Staging smoke tests returned 403 on health check immediately after deploy.

**Root cause (attempt 1):** Assumed IAM propagation timing. Added 15s sleep + 5-attempt retry loop with 10s between attempts.

**Root cause (actual):** `PERMISSION_DENIED: Permission 'run.services.setIamPolicy' denied`. The GitHub Actions service account had `roles/editor` which explicitly excludes all `*.setIamPolicy` permissions. The `|| true` in the IAM binding step was silently hiding the error — the service was deploying but never becoming publicly accessible.

**Fix:**
1. Manually set IAM binding from local: `gcloud run services add-iam-policy-binding lasso-staging --member="allUsers" --role="roles/run.invoker"`
2. Granted `roles/run.admin` to the GitHub Actions service account so future CI runs can set IAM policies on Cloud Run services.

#### Fix 3: Post-Deployment Comment Failure

**Problem:** The staging deploy step posting a commit comment failed with `HttpError: Resource not accessible by integration`, causing the entire deploy job to fail despite all smoke tests passing.

**Fix:** Added `continue-on-error: true` to the "Post deployment comment" step. The comment is informational; a failure there should not abort a successful deployment.

#### Fix 4: publish-image Only Ran on release/** and develop

**Problem:** `publish-image` CI job had a condition excluding `main` branch. When code was merged to main (and tagged), no Docker image was built for the main-branch commit SHA. `deploy-prod.yml` expected an image tagged with `github.sha` and always failed.

**Fix:** Updated `publish-image` condition to include `main`:
```yaml
if: |
  (github.ref == 'refs/heads/develop' || github.ref == 'refs/heads/main' || startsWith(github.ref, 'refs/heads/release/')) &&
  (github.event_name == 'push' || github.event_name == 'workflow_dispatch')
```
Added `main` branch env-tag: images from main are tagged `:latest` (vs `dev-latest` for develop, `staging-latest` for release).

#### Fix 5: Tags from GITHUB_TOKEN Don't Trigger Workflows

**Problem:** The automated release workflow creates the git tag using `GITHUB_TOKEN`. GitHub intentionally prevents tags created by `GITHUB_TOKEN` from triggering other workflows (security feature to prevent infinite loops). So `deploy-prod.yml` (which triggers on tag pushes) never ran automatically.

**Fix:** Trigger production deploys manually via `workflow_dispatch`:
```bash
gh workflow run deploy-prod.yml --ref main --field version=vX.Y.Z
```

#### Fix 6: deploy-prod.yml Used github.sha (SHA Coupling)

**Problem:** `deploy-prod.yml` looked for `gcr.io/.../lasso:${{ github.sha }}`. When triggered via `workflow_dispatch` with `--ref v0.5.1`, `github.sha` was the commit the tag pointed to (`820da6c`). But the image was built at a later commit (`ddf65ea`). SHA mismatch → deploy failed.

**Fix:** Changed `deploy-prod.yml` to use `:latest` image tag throughout:
- "Verify image exists" checks for `lasso:latest`
- "Tag image with version" tags `:latest` as `lasso:vX.Y.Z`
- Deploy command uses `--image .../lasso:latest`
- Summary lines reference version tag instead of SHA

**Why this is safe:** CI always builds `:latest` from the HEAD of `main`. Production deploys are only triggered after CI passes on `main`. So `:latest` always represents the verified, tested code.

#### Fix 7: --no-traffic Not Supported on New Services

**Problem:** `deploy-prod.yml` always passed `--no-traffic` to `gcloud run deploy`. This flag is only valid when updating an existing service (allows health checking the new revision before routing traffic). On the first ever deploy, Cloud Run errors: `--no-traffic not supported when creating a new service`.

**Fix:** Added a "Check if service exists" step that sets an output variable. The deploy step conditionally includes `--no-traffic` and the "Route traffic" step is skipped for new services:
```yaml
- name: Check if service exists
  id: service-exists
  run: |
    if gcloud run services describe $SERVICE_NAME --region $REGION &>/dev/null; then
      echo "exists=true" >> $GITHUB_OUTPUT
    else
      echo "exists=false" >> $GITHUB_OUTPUT
    fi

- name: Route traffic to new revision
  if: steps.service-exists.outputs.exists == 'true'
  ...
```

#### Successful Deployments

**Staging:** 4 attempts before all issues resolved.
- Run 22120925056: FAILED (403, IAM permission denied silently)
- Run 22121106134: FAILED (403, retry loop didn't help — IAM not actually set)
- Run 22121395706: FAILED (post-comment HttpError)
- Run 22121572254: ✅ SUCCESS

**Production:** 4 attempts before all issues resolved.
- Run 22122385651: FAILED (SHA mismatch, image not found)
- Run 22122529242: FAILED (SHA mismatch after fix attempt)
- Run 22122599757: FAILED (`--no-traffic` on new service)
- Run 22122665792: ✅ SUCCESS (from `main` ref with fixed workflow)

**Final URLs:**
- Production: `https://lasso-ngqcsb2bpa-uc.a.run.app`
- Staging: `https://lasso-staging-ngqcsb2bpa-uc.a.run.app`

---

## Files Modified

```
.github/workflows/ci.yml
  - publish-image: add main branch, add :latest env-tag
  - deploy-dev: add OAUTH_CALLBACK_URL to --set-env-vars
  - deploy-staging: add OAUTH_CALLBACK_URL to --set-env-vars
  - staging smoke tests: 15s wait + 5-attempt retry loop
  - post-deployment comment: continue-on-error: true

.github/workflows/deploy-prod.yml
  - Add OAUTH_CALLBACK_URL to --set-env-vars
  - Replace "Check if image exists" with "Verify image exists" using :latest
  - Remove "Tag image with version and latest" (was tagging SHA); replace with "Tag image with version" (tags :latest)
  - Add "Check if service exists" step
  - Deploy: conditionally add --no-traffic based on service-exists
  - Deploy: use :latest image instead of github.sha
  - Health checks: use service URL directly for new services
  - Route traffic: skip if new service
  - Summary: reference version tag instead of SHA

CHANGELOG.md
  - Added [0.5.1] entry with all CI/CD fixes

VERSION
  - Bumped to 0.5.1
```

---

## Technical Learnings

### 1. GITHUB_TOKEN Cannot Trigger Other Workflows

Tags (or any git refs) pushed by a workflow using the default `GITHUB_TOKEN` will NOT trigger other workflows. This is intentional. To trigger downstream workflows from a tag, either:
- Use a Personal Access Token (PAT) with `repo` scope
- Use `workflow_dispatch` manually after the tag is created
- Chain workflows using `workflow_run` event

### 2. roles/editor Does Not Include setIamPolicy

GCP's `roles/editor` is a legacy role. It explicitly excludes all `*.setIamPolicy` permissions. If a CI service account needs to set IAM policies on Cloud Run services (to make them publicly accessible), it needs `roles/run.admin` or a custom role with `run.services.setIamPolicy`.

The `|| true` pattern in shell scripts will hide permission errors. Always check that IAM commands succeed before relying on their effects.

### 3. --no-traffic Flag Behavior

`gcloud run deploy --no-traffic` deploys a new revision without routing any traffic to it. This only works on existing services. On a brand-new service, there's no existing traffic to preserve, and the flag is rejected. Detect service existence before using it.

### 4. GitHub Environments vs Repository Secrets

- **Repository secrets:** Single value, used by all jobs regardless of environment
- **Environment secrets:** Scoped to a specific GitHub Environment; override repo secrets when a job declares `environment: <name>`

For values that differ per deployment target (like `OAUTH_CALLBACK_URL`), use environment-level secrets. The three GitHub Environments (`development`, `staging`, `production`) are already created and the workflows already declare the correct `environment:` for each deploy job.

### 5. SHA-Based Image Tagging Fragility

Tying a production deploy to `github.sha` creates fragility when the workflow runs in different contexts (tag vs branch) or when commits are made after image publication. For a simple project, using `:latest` (built from every main push) is more reliable. For multi-team projects with strict audit requirements, a version-tag-based approach is better.

---

## Current State (End of Phase 1)

| Item | Status |
|---|---|
| Production deployed | ✅ |
| Staging deployed | ✅ |
| Health checks passing | ✅ |
| Static assets serving | ✅ |
| OAUTH_CALLBACK_URL configured | ❌ (next task) |
| OAuth login working | ❌ (blocked on above) |
| Monitoring set up | ❌ |
| README updated | ❌ |

---

## Next Steps (Phase 2)

1. Register production callback URL with Last.fm API app
2. Set `OAUTH_CALLBACK_URL` as environment-level secrets in GitHub
3. Update running Cloud Run service env var
4. Test full OAuth → session → scrobble flow on production
5. Set up Cloud Monitoring uptime checks
6. Update README.md with live URL and description
