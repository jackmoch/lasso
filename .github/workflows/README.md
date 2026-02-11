# GitHub Actions Workflows

This directory contains automated workflows for the Lasso project.

## Workflows

### 1. CI Pipeline (`ci.yml`)

**Trigger:** Automatically runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

**Purpose:** Continuous integration checks to ensure code quality

**What it does:**
1. **Lint Code** - Runs clj-kondo on all source files
2. **Build Frontend** - Compiles ClojureScript with shadow-cljs
3. **Build CSS** - Generates Tailwind CSS
4. **Run Tests** - Executes test suite (when tests exist)
5. **Build Backend** - Creates uberjar
6. **Build Docker** - Verifies Docker image builds
7. **Upload Artifacts** - Saves build outputs for debugging
8. **Generate Summary** - Creates build report in GitHub UI
9. **PR Status Comment** - Posts results to pull request

**Artifacts (7-day retention):**
- Backend JAR (`target/lasso.jar`)
- Frontend JS bundle (`resources/public/js/`)
- Compiled CSS (`resources/public/css/tailwind.css`)
- Lint results (`lint-results.txt`)
- Docker image (3-day retention)

**Debugging:**
```bash
# Check PR status
gh pr checks <pr-number>

# View workflow logs
gh run view <run-id> --log

# Download artifacts
gh run download <run-id>
```

---

### 2. Claude Code Review (`claude-review.yml`)

**Trigger:** On-demand only:
- When `claude-review` label is added to a PR
- Manual workflow dispatch with PR number

**Purpose:** AI-powered code review for important/complex PRs

**What it does:**
1. Checks out PR code with full history
2. Runs Claude Code Review with detailed logging
3. Posts review findings as PR comment
4. Automatically removes label after completion

**Token Conservation:**
- ✅ Does NOT run on every commit
- ✅ Only runs when explicitly requested
- ✅ Auto-removes label to prevent re-runs
- ✅ Use sparingly for important PRs

**How to request a review:**

**Option 1: Via Label (Recommended)**
```bash
# On GitHub PR page, add the 'claude-review' label
# Review runs automatically and posts results
```

**Option 2: Via CLI**
```bash
# Add label via CLI
gh pr edit <pr-number> --add-label "claude-review"

# Or trigger manually
gh workflow run claude-review.yml -f pr_number=<number>
```

**Review Scope:**
- Code quality and best practices
- Potential bugs and edge cases
- Architecture and design patterns
- Security vulnerabilities
- Performance considerations
- Documentation completeness
- Test coverage gaps

**Output:**
With `show_full_output: true`, you get:
- Detailed analysis of all changes
- Specific file/line feedback
- Actionable suggestions
- Full workflow logs for debugging

---

### 3. Deployment (`deploy.yml`)

**Trigger:** Manual workflow dispatch only

**Purpose:** Deploy to Google Cloud Run (Sprint 8)

**Status:** Skeleton implementation - all steps commented out

**What it will do (Sprint 8):**
1. Authenticate with Google Cloud
2. Build Docker image
3. Push to Google Container Registry
4. Deploy to Cloud Run
5. Run smoke tests

**Required Secrets:**
- `GCP_SA_KEY` - Google Cloud service account key
- `GCP_PROJECT_ID` - Google Cloud project ID

See [`docs/deployment/DEPLOYMENT_SECRETS.md`](../../docs/deployment/DEPLOYMENT_SECRETS.md) for setup instructions.

---

## Workflow Best Practices

### When to Use Each Workflow

**CI Pipeline:** Always runs automatically
- Every commit to main/develop
- Every pull request
- Ensures code quality baseline
- Fast feedback (2-3 minutes)

**Claude Review:** Use selectively
- Complex feature implementations
- Architectural changes
- Security-sensitive code
- Before important releases
- When you want second opinion
- Consumes API tokens - use wisely

**Deployment:** Controlled releases only
- After PR merge and manual testing
- For staging/production deploys
- Sprint 8+ only

### CI Failure Troubleshooting

1. **Check PR status:**
   ```bash
   gh pr checks <pr-number>
   ```

2. **View detailed logs:**
   ```bash
   gh run view <run-id> --log
   ```

3. **Download artifacts for inspection:**
   ```bash
   gh run download <run-id>
   cat lint-results.txt  # Check linting errors
   ls -lh target/lasso.jar  # Verify JAR built
   ```

4. **Common Issues:**
   - **Lint errors:** Check `lint-results.txt` artifact
   - **Build failures:** Review shadow-cljs or clojure logs
   - **Docker failures:** Check Dockerfile and .dockerignore
   - **Missing files:** Ensure all source files are committed

### Adding New Workflows

When creating new workflows:

1. **Create workflow file:**
   ```yaml
   name: My Workflow
   on:
     pull_request:  # or push, workflow_dispatch, etc.
   jobs:
     my-job:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         # ... your steps
   ```

2. **Test locally first:**
   - Use `act` to test workflows locally
   - Or create draft PR to test in CI

3. **Document:**
   - Add section to this README
   - Update CLAUDE.md if relevant
   - Add to CONTRIBUTING.md if needed

4. **Optimize:**
   - Use caching for dependencies
   - Upload artifacts for debugging
   - Add clear step names and descriptions

---

## Workflow Status

You can check workflow status:

**Via GitHub UI:**
- Repository → Actions tab
- View runs, logs, and artifacts

**Via CLI:**
```bash
# List recent runs
gh run list

# View specific run
gh run view <run-id>

# View logs
gh run view <run-id> --log

# Re-run failed jobs
gh run rerun <run-id>
```

**Via PR:**
- PR shows check status automatically
- Click "Details" to view logs
- View automated PR comments

---

## Workflow Permissions

Workflows have these permissions:

**CI Pipeline:**
- `contents: read` - Read repository code
- `pull-requests: write` - Comment on PRs

**Claude Review:**
- `contents: write` - Checkout and potentially commit
- `pull-requests: write` - Post review comments
- `issues: write` - Manage labels

**Deployment:**
- `contents: read` - Read code
- Will need GCP credentials via secrets

---

## Related Documentation

- [Autonomous PR Workflow](../../docs/development/AUTONOMOUS_PR_WORKFLOW.md)
- [Contributing Guidelines](../../CONTRIBUTING.md)
- [Deployment Guide](../../docs/deployment/DEPLOYMENT_SECRETS.md)
- [CLAUDE.md](../../CLAUDE.md) - Autonomous workflow overview

---

**Questions or issues?** Open a GitHub Discussion or Issue.
