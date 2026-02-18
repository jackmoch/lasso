# Lasso Development Pipeline

> **MANDATORY FOR ALL CONTRIBUTORS (HUMAN AND LLM)**
>
> This document defines the **only approved** path for getting code from a developer's machine into production. Every stage has required gates. Skipping gates is not permitted. If a gate cannot be satisfied, stop and resolve the underlying issue before proceeding.

---

## Pipeline Overview

```
  Local Development
        │
        ▼
  Feature Branch (feature/*, bugfix/*)
        │  PR → develop  [Gate 1]
        ▼
  develop branch  ──► Auto-deploy → Dev environment
        │
        ▼
  release/* branch  ──► Auto-deploy → Staging environment
        │  PR → main  [Gate 2]
        ▼
  main branch  ──► Auto-deploy → Production environment
```

Each arrow represents a PR. Each PR must pass its gate before merging.

---

## Stage 1: Local Development

### What Happens Here

- Write code on a feature branch created from `develop`
- Run tests and lint locally before pushing

### Required Before Opening a PR

- [ ] Branch created from `develop` (not `main`, not another feature branch)
- [ ] `bb test` passes with 0 failures
- [ ] `bb lint` passes with 0 errors (warnings acceptable)
- [ ] `CHANGELOG.md` updated under `[Unreleased]`
- [ ] New features have corresponding tests
- [ ] Public functions have docstrings

### Branch Naming

| Work Type | Prefix | Example |
|---|---|---|
| New feature | `feature/` | `feature/sprint-11-firestore` |
| Bug fix | `bugfix/` | `bugfix/session-timeout` |
| Critical prod fix | `hotfix/` | `hotfix/0.7.1-auth-crash` |
| Release prep | `release/` | `release/0.7.0` |

---

## Gate 1: PR to `develop`

**Target branch:** `develop`
**Never target `main` directly** (except release branches and hotfixes)

### Automated Checks (must pass)

- CI lint — clj-kondo, 0 errors
- CI backend tests — 0 failures
- CI frontend tests — 0 failures
- CI build — uberjar builds successfully
- CI Docker build validation — image builds cleanly

### Manual Review

- Code is readable and follows project conventions
- No hardcoded secrets or API keys
- No regressions in existing behavior
- PR description clearly explains what changed and why

### After Merging to `develop`

CI automatically:
1. Builds and pushes a new Docker image tagged `dev-latest` and `{git-sha}`
2. Deploys to `lasso-dev` Cloud Run service
3. Runs smoke tests (health, home page, static assets, Firestore connectivity)

**If smoke tests fail:** the deployment is visible in GitHub Actions. Fix the issue on a new branch and open another PR to `develop`. Do not bypass smoke tests.

---

## Stage 2: Dev Environment Verification

After a PR merges to `develop` and CI deploys to dev:

- [ ] Smoke tests passed in CI (check GitHub Actions)
- [ ] Firestore connectivity shows `"ok"` in `/health` (or failure is understood and accepted as a non-blocking warning on dev)
- [ ] Manual verification of the feature on `https://lasso-dev-ngqcsb2bpa-uc.a.run.app`

Dev is for integration testing. If you discover issues here, fix them with another PR to `develop` — do **not** proceed to a release branch until dev is stable.

---

## Stage 3: Release Branch

When `develop` is stable and ready to release:

```bash
# Always branch from develop
git checkout develop && git pull origin develop
git checkout -b release/X.Y.Z
```

### Required Changes on the Release Branch

1. Update `VERSION` file: `echo "X.Y.Z" > VERSION`
2. Update `CHANGELOG.md`: move `[Unreleased]` items to `[X.Y.Z] - YYYY-MM-DD`
3. Commit: `git commit -m "chore(release): bump version to X.Y.Z"`
4. Push: `git push -u origin release/X.Y.Z`

CI automatically deploys `release/**` branches to the staging environment.

---

## Gate 2: PR to `main`

**Target branch:** `main`
**Only release branches and hotfixes target `main`.**

### Automated Checks (must pass)

Same as Gate 1, plus:
- Staging deployment succeeded (check CI for the `release/**` push)
- All staging smoke tests passed **including Firestore connectivity** (Firestore is required on staging — it blocks the deployment if unavailable)

### Manual Verification on Staging

Before opening the PR to `main`, manually verify on staging:

- [ ] OAuth login flow works end-to-end
- [ ] Remember-me cookie is set and works (close tab, reopen, still logged in)
- [ ] `/profile` page loads with correct data
- [ ] Start → Stop a following session; session appears in profile history
- [ ] Logout clears remember-me cookie (subsequent visit requires OAuth)
- [ ] Admin dashboard is accessible and functional

**Do not merge to `main` until all manual checks pass.**

### After Merging to `main`

The `release.yml` workflow automatically:
1. Creates git tag `vX.Y.Z`
2. Creates GitHub Release with changelog entry
3. Triggers `deploy-prod.yml`

CI `deploy-prod.yml` then:
1. Verifies the `:latest` image exists in GCR
2. Deploys with `--no-traffic` (zero-downtime)
3. Waits 20s for the new revision to warm up
4. Shifts 100% traffic to the new revision
5. Runs post-deployment smoke tests
6. Auto-rolls back if the health check fails

### Post-Release Documentation Updates (MANDATORY)

After the release PR merges, **immediately** update all project documentation. This is not optional — it is part of the release process.

```bash
git checkout develop
git pull origin develop
git merge origin/main
git push origin develop
```

Then update on `develop`:

| File | What to Update |
|---|---|
| `STATUS.md` | Version, current sprint, completed work, test metrics |
| `NEXT.md` | Next sprint/task |
| `CLAUDE.md` | Current sprint, version, completed sections |
| `docs/sprints/sprint-X-summary.md` | Create new sprint summary file |
| `MEMORY.md` | New gotchas and patterns |

Commit: `git commit -am "docs: update all project documentation after vX.Y.Z release"`

---

## Hotfix Process

For critical production bugs that cannot wait for a full sprint:

```bash
git checkout main && git pull origin main
git checkout -b hotfix/X.Y.Z-description

# Make minimal fix, test locally
# Update VERSION and CHANGELOG

git commit -m "fix: <description> (hotfix X.Y.Z)"
git push -u origin hotfix/X.Y.Z-description

# PR to main (not develop)
gh pr create --base main --title "hotfix: X.Y.Z description"
```

After merging to `main`, **also** merge back to `develop`:

```bash
git checkout develop
git merge origin/main
git push origin develop
```

---

## Firestore-Specific Pipeline Notes

Firestore is a dependency introduced in Sprint 11. The following must be true for each environment before Firestore features work:

| Prerequisite | Dev | Staging | Production |
|---|---|---|---|
| Firestore database created | Required | Required | Required |
| Service account has `roles/datastore.user` | Required | Required | Required |
| `GOOGLE_CLOUD_PROJECT` env var set | Auto (CI) | Auto (CI) | Auto (CI) |
| `ENVIRONMENT` env var set | Auto (CI) | Auto (CI) | Auto (CI) |

**First-time setup** for each environment: see `docs/operations/environments.md`.

If Firestore is unavailable (missing IAM or DB not created):
- **Dev:** App starts, logs a warning. Smoke test warns but does not fail.
- **Staging:** App starts, but the Firestore smoke test **fails the deployment**.
- **Production:** App starts. If the health endpoint returns a non-200 response, auto-rollback fires.

---

## Approval Matrix

| Action | Who Can Approve |
|---|---|
| Merge feature PR to `develop` | Any maintainer |
| Merge release PR to `main` | Lead maintainer (after manual staging verification) |
| Emergency hotfix to `main` | Lead maintainer |
| Manual production deploy via `workflow_dispatch` | Lead maintainer |

---

## Quick Reference: Common Commands

```bash
# Start work on a new feature
git checkout develop && git pull origin develop
git checkout -b feature/my-feature

# Run all checks locally before pushing
bb test && bb lint

# Create PR to develop
gh pr create --base develop --title "feat: my feature"

# Start a release
git checkout develop && git pull origin develop
git checkout -b release/X.Y.Z
echo "X.Y.Z" > VERSION
# edit CHANGELOG.md
git commit -m "chore(release): bump version to X.Y.Z"
git push -u origin release/X.Y.Z

# Monitor CI
gh pr checks <pr-number>
gh run view <run-id> --log

# Check environment health
curl https://lasso-dev-ngqcsb2bpa-uc.a.run.app/health
curl https://lasso-staging-ngqcsb2bpa-uc.a.run.app/health
curl https://lasso-ngqcsb2bpa-uc.a.run.app/health
```

---

## For LLM Agents (Claude Code)

This section is addressed directly to Claude Code and other automated agents working in this repository.

**You MUST follow this pipeline.** It is not optional.

1. **Never push directly to `develop` or `main`** — always use feature branches and PRs.
2. **Never merge your own PRs** — create the PR and wait for human approval unless explicitly authorized to self-merge.
3. **All Gate 1 automated checks must pass** before considering a PR ready.
4. **Gate 2 requires manual staging verification** — you cannot verify OAuth flows, remember-me cookies, or UI interactions autonomously. Flag this to the human operator.
5. **Post-release documentation updates are mandatory** — after a release PR merges, update STATUS.md, NEXT.md, CLAUDE.md, sprint summary, and MEMORY.md before considering the release complete.
6. **Do not skip or bypass hooks** — if a pre-commit hook or CI check fails, fix the underlying issue. Do not use `--no-verify` or similar bypass flags.
7. **When in doubt, stop and ask** — an incorrect merge to `main` can disrupt production. Escalate rather than guess.

See `CLAUDE.md` for the full autonomous workflow and post-release documentation checklist.
