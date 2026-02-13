# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.0] - 2026-02-12

### Added - Frontend Implementation (Sprint 5-6)
- **Complete Re-frame frontend application** with full state management
- **Authentication UI** with Last.fm OAuth flow integration
- **Session control components** (start, pause, resume, stop)
- **Activity feed** displaying real-time scrobble tracking
- **Error handling UI** with dismissable error banners
- **Tailwind CSS styling** for clean, responsive interface
- Frontend API client (`src/cljs/lasso/api.cljs`) for backend communication
- Re-frame event handlers for auth, session management, and polling
- Re-frame subscriptions for reactive UI updates
- Comprehensive component library in `src/cljs/lasso/components/`

### Added - Development Experience
- **Single-command startup**: `bb dev` runs frontend and backend in parallel
- **Hot module reload** with shadow-cljs and colored console feedback
- **Clean development logging** with visual sections and progress indicators
- **Babashka task runner** with comprehensive task library
- Development documentation: `DEVELOPMENT.md`, `HOT_RELOAD_AND_LOGGING.md`
- Hot reload testing guide: `HOT_RELOAD_TEST.md`
- Custom logging configuration with emoji prefixes and clean output
- Logback configuration to suppress verbose Jetty logs

### Fixed - Frontend Bugs (E2E Testing)
- **Activity feed**: Now properly displays recent scrobbles from backend
- **Pause/Resume buttons**: Fixed Reagent Form-2 components for proper reactivity
- **Page refresh**: Polling indicator and scrobbles now persist after refresh
- **State restoration**: `:check-auth-success` now properly restarts polling on active sessions
- **Re-frame dispatch error**: Fixed nil dispatch value in `:check-auth-success`
- **Session data**: Backend now returns actual scrobble data instead of empty arrays

### Fixed - Backend Issues
- **OAuth web flow**: Removed incorrect `auth.getToken` call (desktop-only flow)
- **Scrobble timestamp filtering**: Removed 5-minute lookback buffer per user request
- **Polling engine**: Now stores and returns actual track data for display
- **Session manager**: Returns recent scrobbles in status endpoint

### Changed
- **ClojureScript source path**: Added `src/cljs` to `deps.edn` paths for proper compilation
- **shadow-cljs configuration**: Added `before-reload` and `after-reload` hooks
- **Component architecture**: All components converted to Reagent Form-2 for reactivity
- **Development workflow**: Switched from embedded to standalone shadow-cljs for better dependency resolution

### Infrastructure
- Parallel process execution using `babashka.process`
- Shutdown hooks for clean process termination
- Frontend compiled JavaScript served from backend on `:8080`
- shadow-cljs dev server on `:8280` for hot reload
- Complete development environment with REPL integration

### Testing
- Manual E2E testing completed successfully
- All bugs discovered during testing fixed and verified
- Hot reload tested and working
- OAuth flow tested end-to-end
- Session management (start/pause/resume/stop) verified
- Real-time scrobble tracking confirmed functional

### Notes
- This release completes Sprint 5-6: Frontend Development
- Application is now fully functional end-to-end
- Ready for integration testing and deployment (Sprint 7-8)
- 34 files changed, 2,771 insertions, 256 deletions
- Frontend: 100% complete with Re-frame + Reagent + Tailwind CSS
- Backend: Fully integrated with frontend via REST API

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

[Unreleased]: https://github.com/jackmoch/lasso/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/jackmoch/lasso/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/jackmoch/lasso/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/jackmoch/lasso/releases/tag/v0.1.0
