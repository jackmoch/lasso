# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-02-12

### Fixed
- **Critical Handler Issues**: Fixed all auth and session handlers to return response maps directly instead of incorrect `{:response ...}` wrapper
- **Handler Signatures**: Updated handler parameters from `[context]` to `[request]` for proper Pedestal compatibility
- **Middleware Session Management**: Fixed require-auth interceptor to attach session to `[:request :session]` instead of `[:session]`
- **Session Extraction**: Updated `get-session-id` helper to extract from request instead of context
- **Request Body Parsing**: Implemented proper InputStream parsing in session handlers with fallbacks for string and map bodies
- **Last.fm API Methods**: Corrected HTTP method selection - GET for unsigned (read) requests, POST with form-params for signed (write) requests
- **OAuth Token Signing**: Fixed `auth.getToken` to use unsigned requests (method doesn't support signatures)
- **Scrobble Response Parsing**: Added support for both integer and string values in `:accepted` and `:ignored` fields to prevent ClassCastException
- **Environment Configuration**: Implemented automatic `.env` file loading on namespace initialization for consistent configuration access

### Added
- Comprehensive integration test suite (`test/clj/lasso/integration/manual_testing_issues_test.clj`) covering all manual testing issues
- 8 new integration tests with 55 assertions covering handler formats, middleware, body parsing, HTTP methods, and OAuth flow
- Complete end-to-end OAuth flow integration test

### Changed
- Test suite expanded from 67 to 75 tests (451 total assertions)
- All tests now passing (0 failures, 0 errors)
- Updated all existing handler tests for new signatures
- Updated middleware tests for request-based session attachment
- Updated OAuth tests to expect unsigned token requests
- Updated rate limiting tests to mock both GET and POST methods

### Infrastructure
- Backend now fully functional with complete OAuth flow working end-to-end
- Session management, scrobble tracking, and polling all operational
- Automated release workflow (`release.yml`) that creates git tags and GitHub releases when VERSION file is updated
- Complete release process documentation in CONTRIBUTING.md
- Semantic versioning guidelines
- Documentation for Claude Interactive workflow (`claude.yml`) for `@claude` mentions
- deps.edn with development, test, REPL, and uberjar build aliases
- shadow-cljs build system with development server
- Tailwind CSS build pipeline
- Docker containerization with non-root user
- CI workflow with clj-kondo linting
- Deployment secrets documentation

### Notes
- This release completes the backend implementation making it fully functional
- OAuth authentication flow works correctly
- Session management (start/pause/resume/stop) works correctly
- Real-time scrobble detection and submission works correctly
- Frontend implementation planned for Sprint 5-6

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

[Unreleased]: https://github.com/jackmoch/lasso/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/jackmoch/lasso/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/jackmoch/lasso/releases/tag/v0.1.0
