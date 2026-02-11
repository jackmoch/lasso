# Autonomous PR Workflow for Claude Code

This document describes the automated Pull Request workflow where Claude Code independently reviews, debugs, and iterates on PRs until they're ready for human review.

## Workflow Overview

```
1. Feature Development
   └─> Push feature branch
       └─> Create PR
           └─> CI runs automatically
               └─> Claude reviews CI results
                   ├─> ✅ All pass → Mark PR ready for human review
                   └─> ❌ Failures → Debug, fix, push, repeat
```

## Enhanced CI Capabilities

### Automatic Triggers
- **Pull Requests:** Runs on PRs to `main` OR `develop`
- **Push Events:** Runs on push to `main` and `develop`
- **Re-runs:** Automatically re-runs when new commits are pushed

### What CI Checks

1. **Linting** (clj-kondo)
   - Errors fail the build
   - Warnings are reported but don't fail
   - Results uploaded as artifact
   - Annotated in PR with file/line numbers

2. **Frontend Build** (shadow-cljs)
   - Compiles ClojureScript
   - Uploads JS artifacts
   - Reports build size

3. **Backend Build** (Clojure uberjar)
   - Builds JAR file
   - Uploads JAR artifact
   - Reports JAR size

4. **CSS Build** (Tailwind)
   - Generates production CSS
   - Uploads CSS artifact
   - Reports CSS size

5. **Tests** (Kaocha)
   - Runs all test suites
   - Continues even if no tests exist (Sprint 2)
   - Will enforce tests in future sprints

6. **Docker Build**
   - Multi-stage build verification
   - Uploads Docker image as artifact
   - Reports image size and layers
   - Validates health check

### CI Outputs for Debugging

#### 1. PR Status Comment
Every PR gets an automated comment with:
- ✅/❌ Overall status
- Individual check results
- Link to detailed logs
- Next steps guidance

#### 2. Build Artifacts (7-day retention)
- `target/lasso.jar` - Backend JAR
- `resources/public/js/` - Frontend JavaScript
- `resources/public/css/tailwind.css` - Compiled CSS
- `lint-results.txt` - Linting output
- Docker image (3-day retention)

#### 3. Step Summaries
- Lint results with error counts
- Build artifact sizes
- Docker image layers
- Test results (when tests exist)

## Autonomous PR Review Process

### Phase 1: Create PR

```bash
# After feature development
git push origin feature/my-feature

# Claude creates PR
gh pr create \
  --base develop \
  --title "feat(scope): description" \
  --body "$(cat PR_TEMPLATE_FILLED.md)"
```

### Phase 2: Monitor CI

Claude monitors the PR with:

```bash
# Check PR status
gh pr checks

# View detailed CI logs
gh run view --log

# Download artifacts if needed
gh run download <run-id>
```

### Phase 3: Debug Failures

When CI fails, Claude:

1. **Reads CI logs**
   ```bash
   gh run view <run-id> --log > ci-logs.txt
   # Claude analyzes logs to identify root cause
   ```

2. **Downloads artifacts**
   ```bash
   gh run download <run-id>
   # Claude inspects build outputs, lint results
   ```

3. **Identifies issues**
   - Parse lint-results.txt for errors
   - Check build logs for compilation errors
   - Review test failures
   - Inspect Docker build failures

4. **Fixes code**
   - Make necessary changes to source files
   - Update dependencies if needed
   - Add missing files
   - Fix syntax errors

5. **Commits and pushes**
   ```bash
   git add .
   git commit -m "fix(ci): resolve linting errors in auth module"
   git push
   # CI automatically re-runs
   ```

### Phase 4: Iterate Until Green

Claude repeats Phase 3 until:
- ✅ All CI checks pass
- ✅ No linting errors
- ✅ All builds succeed
- ✅ Tests pass (when they exist)
- ✅ Docker image builds

### Phase 5: Mark Ready for Human Review

When all checks pass, Claude:

1. **Verifies PR is complete**
   - CHANGELOG.md updated
   - All checklist items marked
   - No merge conflicts
   - Up to date with base branch

2. **Adds ready label**
   ```bash
   gh pr edit --add-label "ready-for-review"
   ```

3. **Notifies user**
   - Comment on PR with summary
   - Mention user for review

## Debugging Tools Available to Claude

### GitHub CLI Commands

```bash
# PR management
gh pr list                          # List all PRs
gh pr view <number>                 # View PR details
gh pr checks <number>               # Check CI status
gh pr diff <number>                 # View PR diff

# Workflow runs
gh run list                         # List recent runs
gh run view <run-id>                # View run details
gh run view <run-id> --log          # View full logs
gh run download <run-id>            # Download artifacts
gh run rerun <run-id>               # Manually re-run

# Issues and comments
gh pr comment <number> --body "..." # Add PR comment
gh pr review <number> --comment     # Add review comment
```

### Artifact Analysis

After downloading artifacts, Claude can:

```bash
# Inspect JAR contents
unzip -l target/lasso.jar

# Check JS bundle
ls -lh resources/public/js/main.js
head -n 50 resources/public/js/main.js

# Review lint results
cat lint-results.txt | grep "error:"

# Load and test Docker image
docker load < docker-image.tar
docker run --rm lasso:ci-<sha> java -jar lasso.jar --version
```

### Log Analysis

Claude parses logs for:

```bash
# Linting errors
grep "error:" lint-results.txt

# Build failures
grep "ERROR" ci-logs.txt
grep "FAILED" ci-logs.txt

# Dependency issues
grep "Could not find" ci-logs.txt

# Compilation errors
grep "CompilerException" ci-logs.txt
```

## Example Autonomous PR Session

### Scenario: Linting Error

```
1. Feature pushed, PR created
2. CI runs → Linting fails
3. Claude downloads lint-results.txt
4. Finds: "error: Unused namespace clojure.string"
5. Claude removes unused import
6. Commits: "fix(lint): remove unused clojure.string import"
7. Pushes → CI re-runs
8. CI passes ✅
9. Claude marks PR ready for review
10. Notifies user
```

### Scenario: Build Failure

```
1. PR created → CI runs
2. shadow-cljs compilation fails
3. Claude views logs: "Could not find namespace lasso.utils"
4. Claude creates missing lasso.utils namespace
5. Commits: "fix(build): add missing lasso.utils namespace"
6. Pushes → CI re-runs
7. Build succeeds but Docker fails
8. Claude views Docker logs: "COPY failed: no such file"
9. Claude fixes Dockerfile path
10. Commits: "fix(docker): correct resource copy path"
11. Pushes → CI re-runs
12. All checks pass ✅
13. Claude marks ready for review
```

## Success Criteria

A PR is ready for human review when:

- ✅ All CI checks pass
- ✅ No linting errors (warnings OK with justification)
- ✅ All builds succeed (frontend, backend, CSS, Docker)
- ✅ Tests pass (when tests exist)
- ✅ CHANGELOG.md updated
- ✅ Documentation updated
- ✅ No merge conflicts
- ✅ Branch up to date with base
- ✅ PR description complete
- ✅ Checklist items marked

## Limitations & Escalation

Claude escalates to human when:

- ❌ Repeated CI failures after 3 fix attempts
- ❌ Fundamental design issues requiring decisions
- ❌ External dependency issues (npm/clojars outages)
- ❌ Infrastructure issues (GitHub Actions failures)
- ❌ Conflicting requirements or ambiguous specs
- ❌ Security vulnerabilities requiring review
- ❌ Breaking changes requiring approval

## Claude Code Review (On-Demand)

In addition to automated CI checks, you can request an AI code review:

### How to Request a Review

**Option 1: Via Label (Recommended)**
1. Go to your PR on GitHub
2. Add the `claude-review` label
3. Claude Code Review workflow runs automatically
4. Review posted as PR comment
5. Label auto-removed after completion

**Option 2: Manual Trigger**
1. Go to Actions → Claude Code Review
2. Click "Run workflow"
3. Enter PR number
4. Review runs and posts results

### What Claude Reviews

- Code quality and best practices
- Potential bugs and edge cases
- Architecture and design patterns
- Security vulnerabilities
- Performance considerations
- Documentation completeness
- Test coverage gaps

### Token Management

**Important:** Claude Code Review uses API tokens, so:
- ✅ Only runs when explicitly requested (via label or manual trigger)
- ✅ Does NOT run on every commit
- ✅ Automatically removes label after completion to prevent re-runs
- ✅ Use sparingly for important PRs or complex changes

### Review Output

With `show_full_output: true`, you get:
- Detailed analysis of changes
- Specific file/line feedback
- Suggestions for improvements
- Link to full workflow logs

## Future Enhancements

Planned improvements:

- [x] Automated code review with detailed feedback (via claude-review label)
- [ ] Performance regression detection
- [ ] Security scanning (SAST, dependency checks)
- [ ] Visual regression testing (screenshots)
- [ ] Accessibility testing
- [ ] Load testing for backend
- [ ] Automatic dependency updates
- [ ] Changelog generation from commits

## Workflow Diagram

```
┌─────────────────┐
│ Push Feature    │
│ Branch          │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Create PR       │
│ (Claude)        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ CI Runs         │
│ Automatically   │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
  PASS      FAIL
    │         │
    │    ┌────▼──────────┐
    │    │ Claude        │
    │    │ Downloads     │
    │    │ Artifacts     │
    │    └────┬──────────┘
    │         │
    │    ┌────▼──────────┐
    │    │ Claude        │
    │    │ Analyzes      │
    │    │ Logs          │
    │    └────┬──────────┘
    │         │
    │    ┌────▼──────────┐
    │    │ Claude        │
    │    │ Fixes Code    │
    │    └────┬──────────┘
    │         │
    │    ┌────▼──────────┐
    │    │ Commit & Push │
    │    └────┬──────────┘
    │         │
    │         └──────┐
    │                │
    ▼                ▼
┌─────────────────────┐
│ Mark Ready for      │
│ Human Review        │
└─────────────────────┘
         │
         ▼
┌─────────────────────┐
│ Notify User         │
│ PR Ready to Merge   │
└─────────────────────┘
```

---

This workflow enables Claude to independently ensure code quality while keeping humans in the loop for final review and merge decisions.
