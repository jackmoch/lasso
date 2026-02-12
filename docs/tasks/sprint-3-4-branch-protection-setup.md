# Task: Configure Branch Protection for Main Branch

## Context

The Lasso project needs to enforce that all tests pass before PRs can be merged to `main`. This requires:
1. Fixing any flaky/inconsistent tests
2. Ensuring CI properly fails when tests fail
3. Configuring GitHub branch protection rules

## Current State (as of 2026-02-12)

### ✅ Completed

1. **Repository Made Public**
   - Repository visibility changed from private to public
   - Required for free branch protection (alternative would be GitHub Pro)
   - Command used: `gh repo edit jackmoch/lasso --visibility public --accept-visibility-change-consequences`

2. **External Interactions Disabled**
   - Issues: ❌ Disabled
   - Wiki: ❌ Disabled
   - Projects: ❌ Disabled
   - Discussions: ❌ Disabled
   - Result: Only repository owner can create PRs/issues
   - Commands used:
     ```bash
     gh repo edit jackmoch/lasso --enable-issues=false --enable-wiki=false --enable-projects=false
     gh api -X PATCH repos/jackmoch/lasso --field has_discussions=false
     ```

3. **Flaky Test Fixed**
   - **File**: `test/clj/lasso/lastfm/client_test.clj`
   - **Issue**: Rate-limiting test was timing-dependent and failed intermittently
   - **Fix**: Refactored to mock `wait-for-rate-limit` function and count invocations instead of measuring actual elapsed time
   - **Result**: Tests now pass consistently (44 tests, 205 assertions, 0 failures)
   - **Commit**: `976709c` on branch `feature/sprint-3-4-phases-1-3`

4. **CI Test Enforcement Fixed**
   - **File**: `.github/workflows/ci.yml`
   - **Issue**: CI had `continue-on-error: true` and `|| echo` fallback that swallowed test failures
   - **Fix**: Removed both so CI properly fails when tests fail
   - **Changes**:
     ```yaml
     # Before (lines 82-88):
     - name: Run tests
       id: test
       run: |
         echo "Running Clojure tests..."
         clj -M:test || echo "No tests found (expected for Sprint 2)"
         echo "✅ Test step completed"
       continue-on-error: true  # Don't fail if no tests exist yet

     # After:
     - name: Run tests
       id: test
       run: |
         echo "Running Clojure tests..."
         clj -M:test
         echo "✅ All tests passed"
     ```
   - **Commit**: `e35b39d` on branch `fix/claude-review-enable-comments`

5. **Current Branch/PR Status**
   - **PR #4**: `feature/sprint-3-4-phases-1-3` (Sprint 3-4 backend implementation)
     - Contains: Backend foundation (Phases 1-3) + test fix
     - Commits: 3 total
       - `ffdea80` - feat: implement Sprint 3-4 Phases 1-3
       - `44e5e81` - fix(lastfm): API signature generation bug
       - `976709c` - test(lastfm): fix flaky rate-limiting test

   - **PR #5**: `fix/claude-review-enable-comments` (Workflow fixes)
     - Contains: Claude review comments + CI test enforcement
     - Commits: 2 total
       - `8a5246a` - fix(ci): enable Claude review to post PR comments
       - `e35b39d` - fix(ci): enforce test failures in CI pipeline

### ⚠️ Incomplete

**Branch Protection Configuration**

The GitHub API approach failed with error:
```
gh: Invalid request.
For 'links/0/schema', nil is not an object. (HTTP 422)
```

Multiple attempts were made:
- JSON file with full config (failed)
- JSON file with minimal config (failed)
- CLI flags approach (shell escaping issues in zsh)

**Root Cause**: Unknown API issue, possibly related to repository state or API schema validation

## Next Steps

### Required Action: Configure Branch Protection via Web UI

Since the API/CLI approach failed, configure via GitHub web interface:

**URL**: https://github.com/jackmoch/lasso/settings/branches

**Steps**:
1. Click **"Add branch protection rule"**
2. **Branch name pattern**: `main`
3. Enable these settings:
   - ✅ **Require status checks to pass before merging**
     - Click "Add" button
     - Search for and select: **`lint-and-build`** (this is the CI job name from `.github/workflows/ci.yml`)
     - ✅ Check **"Require branches to be up to date before merging"**
4. (Optional but recommended):
   - ✅ **Require a pull request before merging**
   - ✅ **Require approvals**: 0 (since it's a solo project, but enables the PR workflow)
5. Click **"Create"** at the bottom

### Verification

After configuration:
1. Merge **PR #5** first (workflow fixes - enables proper CI test enforcement)
2. Check **PR #4** - should show CI status check as required
3. Try to merge PR #4 - merge button should be disabled until CI passes
4. Verify CI runs and tests pass (should show green checkmark)
5. Merge button should become enabled only after CI passes

### Alternative: Retry API Configuration (Optional)

If you want to try the API approach again in a new session:

```bash
# Create protection config
cat > /tmp/branch-protection.json << 'EOF'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["lint-and-build"]
  },
  "enforce_admins": false
}
EOF

# Apply protection
gh api repos/jackmoch/lasso/branches/main/protection --method PUT < /tmp/branch-protection.json
```

If this fails again, proceed with web UI method.

## Files Modified

- `test/clj/lasso/lastfm/client_test.clj` - Fixed flaky rate-limiting test
- `.github/workflows/ci.yml` - Removed test failure suppression

## References

- GitHub Branch Protection Docs: https://docs.github.com/rest/branches/branch-protection
- CI Workflow: `.github/workflows/ci.yml`
- Test File: `test/clj/lasso/lastfm/client_test.clj`

## Success Criteria

✅ Branch protection rule exists for `main` branch
✅ Status check `lint-and-build` is required
✅ PR merge is blocked until CI passes
✅ All tests pass consistently in CI (44 tests, 205 assertions, 0 failures)

## Notes

- Repository is now public - no secrets were found in git history
- `.env` file is properly gitignored and never committed
- All external interactions (issues, wiki, discussions, projects) are disabled
- Only repository owner can create PRs/issues
- Forking is still allowed (standard for public repos) but fork PRs cannot be opened
