# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Automated release workflow (`release.yml`) that creates git tags and GitHub releases when VERSION file is updated
- Complete release process documentation in CONTRIBUTING.md
- Semantic versioning guidelines

### Changed
- Sprint 2 marked as complete in CLAUDE.md project status
- Enhanced workflow documentation with release workflow guide

### Infrastructure
- deps.edn with development, test, REPL, and uberjar build aliases
- shadow-cljs build system with development server
- Tailwind CSS build pipeline
- Docker containerization with non-root user
- CI workflow with clj-kondo linting
- Deployment secrets documentation

## [0.1.0] - 2024-02-11

### Added
- Sprint 2: Complete project scaffolding
- Development environment setup
- Build system configuration
- Basic "Hello World" application structure
- Project documentation and deployment guides

### Notes
- This is the initial release establishing the project foundation
- No Last.fm integration yet (planned for Sprint 3-4)
- No authentication flow yet (planned for Sprint 3-4)
- No session management yet (planned for Sprint 3-4)

[Unreleased]: https://github.com/jackmoch/lasso/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/jackmoch/lasso/releases/tag/v0.1.0
