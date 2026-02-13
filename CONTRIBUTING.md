# Contributing to Lasso

Thank you for your interest in contributing to Lasso! This document outlines our development workflow, branching strategy, and contribution guidelines.

## Development Workflow

### Branching Strategy

We follow **Git Flow** with the following branch types:

#### Main Branches

- **`main`** - Production releases only. Protected branch requiring pull requests and passing CI.
  - Only receives merges from `release/*` and `hotfix/*` branches
  - Every merge triggers automated release creation (tagging, GitHub release)

- **`develop`** - Active development and integration branch. Protected branch requiring pull requests and passing CI.
  - Receives merges from `feature/*` and `bugfix/*` branches
  - This is where day-to-day development happens

#### Supporting Branches

- **`feature/*`** - New features and enhancements
  - Branch from: `develop`
  - Merge into: `develop`
  - Naming: `feature/sprint-X-description` or `feature/short-description`
  - Example: `feature/sprint-3-lastfm-api`, `feature/oauth-flow`

- **`bugfix/*`** - Bug fixes during development
  - Branch from: `develop`
  - Merge into: `develop`
  - Naming: `bugfix/issue-number-description` or `bugfix/short-description`
  - Example: `bugfix/123-login-error`, `bugfix/session-timeout`

- **`hotfix/*`** - Critical production fixes
  - Branch from: `main`
  - Merge into: `main` AND `develop`
  - Naming: `hotfix/version-description`
  - Example: `hotfix/0.1.1-security-patch`

- **`release/*`** - Release preparation (version bumps, changelog updates)
  - Branch from: `develop`
  - Merge into: `main` AND `develop`
  - Naming: `release/version`
  - Example: `release/0.2.0`

### Workflow Steps

#### 1. Create a Feature Branch

```bash
# Make sure you're on develop and up to date
git checkout develop
git pull origin develop

# Create your feature branch
git checkout -b feature/sprint-3-lastfm-api
```

#### 2. Make Your Changes

- Write clean, well-documented code
- Follow the [Clojure Style Guide](https://guide.clojure.style/)
- Add tests for new functionality
- Update documentation as needed
- Keep commits focused and atomic

#### 3. Commit Your Changes

We follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:

```bash
# Format: <type>(<scope>): <description>
#
# Types: feat, fix, docs, style, refactor, test, chore
# Scope: api, auth, ui, build, config (optional)

# Examples:
git commit -m "feat(api): add Last.fm OAuth 2.0 authentication flow"
git commit -m "fix(session): resolve session timeout handling"
git commit -m "docs(readme): update setup instructions"
git commit -m "test(auth): add OAuth flow integration tests"
```

**Commit Message Guidelines:**
- Use present tense ("add feature" not "added feature")
- Use imperative mood ("move cursor to..." not "moves cursor to...")
- First line <= 72 characters
- Reference issues and PRs where relevant: "fixes #123"

#### 4. Update CHANGELOG.md

Before creating a PR, update `CHANGELOG.md` under the `[Unreleased]` section:

```markdown
## [Unreleased]

### Added
- Last.fm OAuth 2.0 authentication flow (#45)
- Session management with encrypted cookies (#46)

### Changed
- Improved error handling in API client (#47)

### Fixed
- Session timeout issue during long polling (#48)
```

#### 5. Push Your Branch

```bash
git push origin feature/sprint-3-lastfm-api
```

#### 6. Create a Pull Request

**IMPORTANT: Feature and bugfix PRs target `develop`, NOT `main`!**

```bash
# Create PR to develop
gh pr create --base develop --title "feat(api): add Last.fm OAuth flow"
```

Or via GitHub UI:
1. Go to GitHub and create a Pull Request from your branch to `develop`
2. Fill out the PR template with:
   - **Description**: What does this PR do?
   - **Changes**: List of changes made
   - **Testing**: How was this tested?
   - **Screenshots**: If UI changes (optional)
   - **Related Issues**: Links to issues this PR addresses

3. CI will automatically run (must pass before merge)
4. Address feedback and update as needed
5. Once CI passes and approved, squash and merge into `develop`

### Release Process (Gitflow)

When ready to release a new version (e.g., at the end of a sprint):

#### 1. Create Release Branch from Develop

```bash
git checkout develop
git pull origin develop
git checkout -b release/0.2.0
```

#### 2. Prepare Release

- Update `VERSION` file: `0.2.0`
- Update `CHANGELOG.md`:
  - Move `[Unreleased]` items to new `[0.2.0] - YYYY-MM-DD` section
  - Add release date
  - Update comparison links at bottom

```bash
echo "0.2.0" > VERSION

# Edit CHANGELOG.md to move unreleased items to [0.2.0] - 2024-02-12
```

#### 3. Commit and Push Release Branch

```bash
git add VERSION CHANGELOG.md
git commit -m "chore(release): bump version to 0.2.0"
git push origin release/0.2.0
```

#### 4. Create PR to Main (Triggers Automated Release)

```bash
gh pr create --base main --title "Release v0.2.0"
```

Once this PR merges to `main`:
- ✅ GitHub Actions automatically creates tag `v0.2.0`
- ✅ GitHub Actions creates GitHub Release with changelog
- ✅ Deployment pipelines triggered (if configured)

#### 5. Merge Main Back to Develop

After the release PR merges:

```bash
git checkout develop
git pull origin develop
git merge origin/main
git push origin develop
```

This keeps `develop` in sync with the version bump.

## Semantic Versioning

We use [Semantic Versioning](https://semver.org/) (SemVer): `MAJOR.MINOR.PATCH`

- **MAJOR** (1.0.0): Breaking changes, incompatible API changes
- **MINOR** (0.1.0): New features, backwards-compatible
- **PATCH** (0.0.1): Bug fixes, backwards-compatible

### Pre-1.0.0 Versioning

During initial development (0.x.x):
- **MINOR** version may include breaking changes
- **PATCH** version for backwards-compatible changes
- Version 1.0.0 will be the first stable public release

### Version Increments

| Change Type | Example | Version Change |
|-------------|---------|----------------|
| New feature | Add OAuth flow | 0.1.0 → 0.2.0 |
| Bug fix | Fix session timeout | 0.2.0 → 0.2.1 |
| Breaking change | Change API endpoints | 0.2.1 → 0.3.0 (pre-1.0) or 0.2.1 → 1.0.0 (post-1.0) |
| Security patch | Fix XSS vulnerability | 0.2.1 → 0.2.2 |

## Code Standards

### Clojure/ClojureScript

- Follow [Clojure Style Guide](https://guide.clojure.style/)
- Use `cljfmt` for code formatting
- Max line length: 100 characters
- Write docstrings for public functions
- Use meaningful variable names
- Prefer pure functions over stateful code

### Linting

```bash
# Run linter before committing
clj-kondo --lint src

# Auto-fix formatting (if using cljfmt)
clj -M:cljfmt fix
```

### Testing

```bash
# Run all tests
clj -M:test

# Run specific test namespace
clj -M:test --focus lasso.auth.core-test

# Tests should be added for:
# - New features
# - Bug fixes
# - Edge cases
```

## Pull Request Checklist

Before submitting a PR, ensure:

- [ ] Code follows project style guidelines
- [ ] All tests pass (`clj -M:test`)
- [ ] Linter passes with no warnings (`clj-kondo --lint src`)
- [ ] New features have tests
- [ ] Documentation updated (README, docstrings)
- [ ] CHANGELOG.md updated under `[Unreleased]`
- [ ] Commit messages follow Conventional Commits
- [ ] Branch is up to date with target branch
- [ ] No merge conflicts

## Automated Release System

Releases are **fully automated** via GitHub Actions when VERSION changes merge to `main`. No manual tagging required!

### How It Works

When a PR with VERSION file changes merges to `main`:
1. `release.yml` workflow detects VERSION file change
2. Creates git tag `vX.Y.Z` automatically
3. Extracts changelog entry for that version
4. Creates GitHub release with notes
5. Posts summary to workflow run

**View releases:** https://github.com/jackmoch/lasso/releases

### Creating a Release (Gitflow Approach)

**IMPORTANT: Releases come from `release/*` branches, NOT feature branches!**

Follow the "Release Process (Gitflow)" section above. In summary:

1. Create `release/X.Y.Z` branch from `develop`
2. Update `VERSION` and `CHANGELOG.md` in that branch
3. PR `release/X.Y.Z` → `main` (automated release triggers on merge)
4. Merge `main` back to `develop` to sync version

### Semantic Versioning

We follow [SemVer](https://semver.org/):

- **MAJOR** (1.0.0): Breaking changes
- **MINOR** (0.1.0): New features, backwards compatible
- **PATCH** (0.0.1): Bug fixes, backwards compatible

**Examples:**
- `0.1.0` → `0.2.0`: Added new features
- `0.2.0` → `0.2.1`: Fixed bugs
- `0.9.0` → `1.0.0`: First stable release with breaking changes

### Changelog Format

Follow [Keep a Changelog](https://keepachangelog.com/):

```markdown
## [Unreleased]

### Added
- New features go here during development

### Changed
- Changes to existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Vulnerability fixes

## [0.2.0] - 2024-02-12

### Added
- Feature X implementation

[Unreleased]: https://github.com/jackmoch/lasso/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/jackmoch/lasso/compare/v0.1.0...v0.2.0
```

### Release Checklist

#### Pre-Release (Before Creating Release Branch)

- [ ] All features/fixes for this release are merged to `develop`
- [ ] All tests passing on `develop`
- [ ] All CI checks passing
- [ ] Sprint work completed and tested

#### Release Branch Preparation

- [ ] Create release branch: `git checkout -b release/X.Y.Z`
- [ ] Update VERSION file with new version number
- [ ] Update CHANGELOG.md:
  - [ ] Move `[Unreleased]` items to new `[X.Y.Z] - YYYY-MM-DD` section
  - [ ] Add release date
  - [ ] Update comparison links at bottom
- [ ] Commit changes: `git commit -m "chore(release): bump version to X.Y.Z"`

#### Post-Release Documentation Updates (CRITICAL)

**After the release PR merges to `main`, update ALL project documentation:**

- [ ] **STATUS.md** - Update current state
  - [ ] Update version at top
  - [ ] Update current sprint
  - [ ] Move completed work to "What's Been Completed" section
  - [ ] Clear "What's In Progress" section
  - [ ] Update test metrics and key metrics
  - [ ] Update branch status diagram

- [ ] **NEXT.md** - Update immediate next task
  - [ ] Change immediate next task to upcoming sprint/phase
  - [ ] Move completed tasks to backlog or remove
  - [ ] Update task descriptions and acceptance criteria

- [ ] **CLAUDE.md** - Update project overview
  - [ ] Update "Current Sprint" section
  - [ ] Update "Version" in Quick Start
  - [ ] Add completed work to "Completed" section
  - [ ] Update "Current Status" with new branch state

- [ ] **docs/sprints/sprint-X-summary.md** - Create sprint summary
  - [ ] Create new file for completed sprint
  - [ ] Document what was accomplished
  - [ ] List all files created/modified
  - [ ] Include test metrics and verification results
  - [ ] Document key decisions and lessons learned
  - [ ] Add git workflow details

- [ ] **MEMORY.md** - Update learnings (in `.claude/projects/.../memory/`)
  - [ ] Add any new gotchas discovered
  - [ ] Document patterns that worked well
  - [ ] Update common errors and fixes
  - [ ] Keep concise (under 200 lines)

#### Post-Release Sync

- [ ] Merge `main` back to `develop`:
  ```bash
  git checkout develop
  git pull origin develop
  git merge origin/main
  git push origin develop
  ```
- [ ] Commit documentation updates:
  ```bash
  git add STATUS.md NEXT.md CLAUDE.md docs/sprints/sprint-X-summary.md
  git commit -m "docs: update all project documentation after vX.Y.Z release"
  git push origin develop
  ```
- [ ] Verify automated release created on GitHub
- [ ] Verify git tag created: `git tag -l`

**💡 Tip:** Use `scripts/prepare-release.sh` to help automate version bumps and documentation reminders.

## Getting Help

- **Questions**: Open a GitHub Discussion
- **Bugs**: Open a GitHub Issue with reproduction steps
- **Features**: Open a GitHub Issue with feature proposal
- **Security**: Email maintainer directly (see SECURITY.md)

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the project
- Show empathy towards other contributors

## License

By contributing to Lasso, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Lasso! 🎵
