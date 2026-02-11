# Contributing to Lasso

Thank you for your interest in contributing to Lasso! This document outlines our development workflow, branching strategy, and contribution guidelines.

## Development Workflow

### Branching Strategy

We follow a **Git Flow** inspired workflow with the following branch types:

#### Main Branches

- **`main`** - Production-ready code. Protected branch requiring pull requests.
- **`develop`** - Integration branch for features. Default branch for development.

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

1. Go to GitHub and create a Pull Request from your branch to `develop`
2. Fill out the PR template with:
   - **Description**: What does this PR do?
   - **Changes**: List of changes made
   - **Testing**: How was this tested?
   - **Screenshots**: If UI changes (optional)
   - **Related Issues**: Links to issues this PR addresses

3. Request review from maintainers
4. Address feedback and update as needed
5. Once approved, squash and merge into `develop`

### Release Process

When ready to release a new version:

#### 1. Create Release Branch

```bash
git checkout develop
git pull origin develop
git checkout -b release/0.2.0
```

#### 2. Prepare Release

- Update `VERSION` file: `0.2.0`
- Update `package.json` version: `"version": "0.2.0"`
- Update `CHANGELOG.md`:
  - Move `[Unreleased]` items to new `[0.2.0] - YYYY-MM-DD` section
  - Add release date
  - Update comparison links at bottom
- Update any other version references

#### 3. Commit Release Changes

```bash
git add VERSION package.json CHANGELOG.md
git commit -m "chore(release): bump version to 0.2.0"
git push origin release/0.2.0
```

#### 4. Create Release PRs

1. **PR to `main`**: `release/0.2.0` → `main`
   - Once merged, tag the release: `git tag v0.2.0`
   - Push tag: `git push origin v0.2.0`
   - Create GitHub Release from tag

2. **PR to `develop`**: Merge `main` back into `develop` to sync

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
