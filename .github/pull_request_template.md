## Description

<!-- Provide a clear and concise description of what this PR does -->

## Type of Change

<!-- Mark the relevant option with an "x" -->

- [ ] 🎨 Feature (new functionality)
- [ ] 🐛 Bug fix (fixes an issue)
- [ ] 📚 Documentation (updates to docs, comments, README)
- [ ] 🔧 Refactor (code restructuring without changing behavior)
- [ ] ✅ Test (adding or updating tests)
- [ ] 🔨 Build/Config (build scripts, dependencies, configuration)
- [ ] 🚀 Performance (performance improvements)
- [ ] 🔒 Security (security-related changes)
- [ ] 🎯 **Release (version bump - see Release Checklist below)**

## Changes Made

<!-- List the specific changes made in this PR -->

-
-
-

## Related Issues

<!-- Link related issues, e.g., "Closes #123" or "Relates to #456" -->

-

## Testing

<!-- Describe how these changes were tested -->

### Manual Testing
- [ ] Tested locally with REPL
- [ ] Tested frontend in browser
- [ ] Tested hot reload functionality
- [ ] Verified build succeeds

### Automated Testing
- [ ] All existing tests pass
- [ ] New tests added for new functionality
- [ ] Linter passes with no warnings

### Test Steps

<!-- Provide steps to test this PR -->

1.
2.
3.

## Screenshots

<!-- If applicable, add screenshots to demonstrate UI changes -->

## Checklist

<!-- Ensure all items are complete before requesting review -->

- [ ] Code follows project style guidelines
- [ ] Self-review of code completed
- [ ] Comments added for complex logic
- [ ] Documentation updated (README, CLAUDE.md, docstrings)
- [ ] CHANGELOG.md updated under `[Unreleased]` section
- [ ] No new warnings from linter
- [ ] All tests pass locally
- [ ] Dependent changes merged and published
- [ ] Branch is up to date with base branch
- [ ] Commit messages follow Conventional Commits format

## Deployment Notes

<!-- Any special deployment considerations or environment variable changes -->

- [ ] No deployment changes required
- [ ] Environment variables added/changed (document below)
- [ ] Database migrations required
- [ ] Dependencies added/updated

## Breaking Changes

<!-- List any breaking changes and migration steps -->

- [ ] No breaking changes
- [ ] Breaking changes (describe below)

## 🎯 Release Checklist (FOR RELEASE PRs ONLY)

<!-- If this is a release PR (release/* → main), complete this section -->
<!-- Delete this section if NOT a release PR -->

### Pre-Release Verification
- [ ] All features/fixes for this release merged to develop
- [ ] All tests passing on develop
- [ ] VERSION file updated
- [ ] CHANGELOG.md updated with release notes and date
- [ ] Changelog comparison links updated

### Post-Release Documentation Updates (CRITICAL)
**After this PR merges to main, you MUST update these files on develop:**

- [ ] **STATUS.md**
  - [ ] Version updated at top
  - [ ] Current sprint updated
  - [ ] Completed work moved to "What's Been Completed"
  - [ ] "What's In Progress" cleared
  - [ ] Test metrics and key metrics updated

- [ ] **NEXT.md**
  - [ ] Immediate next task updated to upcoming sprint
  - [ ] Completed tasks moved/removed

- [ ] **CLAUDE.md**
  - [ ] "Current Sprint" section updated
  - [ ] Version updated in Quick Start
  - [ ] Completed work added to "Completed" section

- [ ] **docs/sprints/sprint-X-summary.md**
  - [ ] New sprint summary created documenting completed work

- [ ] **MEMORY.md** (in .claude/projects/.../memory/)
  - [ ] New learnings and gotchas added

### Post-Release Git Workflow
- [ ] Merge main back to develop after release
- [ ] Commit documentation updates to develop
- [ ] Verify automated release created on GitHub
- [ ] Verify git tag created

**See CONTRIBUTING.md "Post-Release Documentation Updates" for detailed checklist.**

## Additional Context

<!-- Add any other context about the PR here -->
