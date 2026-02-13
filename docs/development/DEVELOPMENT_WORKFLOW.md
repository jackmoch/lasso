# Development Workflow Guide

**Last Updated:** 2026-02-12

This guide explains the modern, Clojurian approach to starting and managing the Lasso development environment.

## Quick Start

### One-Command Startup (Recommended)

```bash
bb dev
```

That's it! This starts:
- ✅ Backend server (Pedestal + Jetty on port 8080)
- ✅ Frontend watch (shadow-cljs with hot reload)
- ✅ ClojureScript compilation
- ✅ Integrated REPL for both Clojure and ClojureScript

Open http://localhost:8080 and start coding!

## Approaches

### 1. Babashka Tasks (Simplest)

Babashka provides convenient CLI commands for all common tasks:

```bash
# Development
bb dev           # Start everything
bb repl          # Start REPL only
bb cljs-repl     # Start ClojureScript REPL

# Frontend
bb frontend      # Watch frontend only
bb frontend:build # Build frontend for production

# CSS
bb css           # Watch CSS changes
bb css:build     # Build CSS for production

# Testing
bb test          # Run all tests
bb test:watch    # Watch mode
bb test:focus lasso.auth.core-test  # Run specific test

# Building
bb build         # Build everything for production
bb uberjar       # Build backend JAR only
bb clean         # Clean build artifacts
bb clean:full    # Full clean including node_modules

# Linting
bb lint          # Lint code
bb format        # Format code

# Docker
bb docker:build  # Build Docker image
bb docker:run    # Run Docker container

# Git workflow
bb pr            # Create PR to develop
bb pr:main       # Create PR to main
bb ci:wait 123   # Wait for CI on PR #123

# Utilities
bb deps          # Download dependencies
bb outdated      # Check for outdated deps
bb server        # Start production server from JAR
bb help          # Show common commands
bb tasks         # List all tasks
```

### 2. REPL-Driven Workflow (More Control)

For direct REPL control with shadow-cljs integration:

```bash
clj -M:dev
```

**REPL Commands:**

```clojure
;; Start everything
(start)          ; Start backend + frontend
(go)             ; Alias for start

;; Control
(stop)           ; Stop everything
(restart)        ; Restart everything
(reset)          ; Reload namespaces + restart

;; Individual control
(start-backend)  ; Start only backend
(stop-backend)   ; Stop only backend
(start-frontend) ; Start only frontend (shadow-cljs watch)
(stop-frontend)  ; Stop only frontend

;; ClojureScript REPL
(cljs-repl)      ; Connect to browser for interactive CLJS
; Then in CLJS REPL:
cljs.user=> (js/alert "Hello!")
cljs.user=> :cljs/quit  ; Return to Clojure REPL
```

### 3. Manual Process Control

If you need to run each process separately:

```bash
# Terminal 1 - Backend
clj -M:dev
user=> (start-backend)

# Terminal 2 - Frontend
npx shadow-cljs watch app

# Terminal 3 - CSS (optional)
npm run watch:css
```

## Hot Reload

### Frontend (ClojureScript)

- Edit any `.cljs` file
- Save
- Changes appear **instantly** in browser
- Re-frame state preserved across reloads
- No manual refresh needed

### Backend (Clojure)

- **Simple changes**: Reload namespace in REPL
- **Route/handler changes**: `(restart)` in REPL
- **Deep changes**: `(reset)` to reload all namespaces
- No server restart needed for most changes

### CSS (Tailwind)

- Edit Tailwind classes in ClojureScript files
- Save
- CSS rebuilds automatically (when using `bb css` or `npm run watch:css`)

## Implementation Details

### Integrated REPL Architecture

The `dev/user.clj` namespace orchestrates the entire development environment:

**Key features:**
- Requires `shadow.cljs.devtools.api` and `shadow.cljs.devtools.server`
- Starts shadow-cljs server automatically when needed
- Provides unified control over backend and frontend
- Includes helpful status messages and visual feedback

**Files:**
- `dev/user.clj` - REPL utilities with shadow-cljs integration
- `bb.edn` - Babashka task definitions
- `deps.edn` - Updated `:dev` alias with shadow-cljs dependency

### Babashka Tasks

The `bb.edn` file defines over 25 tasks for common operations:

**Benefits:**
- Zero-setup commands (just `bb <task>`)
- Consistent interface across the project
- Self-documenting (`bb tasks` to see all)
- Fast execution with Babashka
- Shell-friendly for CI/CD

### shadow-cljs Integration

shadow-cljs is now available in the Clojure REPL via the `:dev` alias:

```clojure
:dev
{:extra-paths ["dev"]
 :extra-deps {org.clojure/tools.namespace {:mvn/version "1.5.0"}
              thheller/shadow-cljs {:mvn/version "2.27.5"}}}
```

This allows calling shadow-cljs API functions directly from the REPL:
- `(shadow/watch :app)` - Start frontend watch
- `(shadow/stop-worker :app)` - Stop frontend watch
- `(shadow/repl :app)` - Start ClojureScript REPL

## Workflow Comparison

### Before (Manual Multi-Terminal)

```bash
# Terminal 1
clj -M:dev:repl
user=> (start)

# Terminal 2
npx shadow-cljs watch app

# Terminal 3
npm run watch:css
```

**Problems:**
- 3 separate terminals to manage
- Manual coordination needed
- Easy to forget a process
- No unified stop command

### After (Integrated)

**Option 1 - Babashka:**
```bash
bb dev  # Everything starts
# Ctrl+C to stop everything
```

**Option 2 - REPL:**
```bash
clj -M:dev
user=> (start)  # Everything starts
user=> (stop)   # Everything stops
```

**Benefits:**
- ✅ One command to start everything
- ✅ Unified control (start/stop/restart)
- ✅ Clear status messages
- ✅ No manual coordination
- ✅ Easy to stop everything

## Community Standards

This implementation follows modern Clojure community practices:

**Babashka Tasks:**
- Increasingly standard in Clojure projects
- Used by major projects (Logseq, etc.)
- Fast, lightweight, scriptable
- See: [Babashka Book](https://book.babashka.org/)

**shadow-cljs Integration:**
- Recommended by shadow-cljs creator (Thomas Heller)
- Based on [Fullstack Workflow article](https://code.thheller.com/blog/shadow-cljs/2024/10/18/fullstack-cljs-workflow-with-shadow-cljs.html) (Oct 2024)
- Standard pattern for ClojureScript SPAs
- REPL-driven, interactive development

**REPL-First Development:**
- Core Clojure philosophy
- Interactive, exploratory coding
- Immediate feedback loop
- No need to restart for most changes

## Troubleshooting

**REPL won't start:**
```bash
rm -rf .cpcache .shadow-cljs
clj -P  # Re-download deps
bb dev
```

**Frontend not updating:**
- Check browser console for compilation errors
- Try hard refresh (Cmd+Shift+R)
- In REPL: `(stop-frontend)` then `(start-frontend)`

**Backend changes not taking effect:**
```clojure
user=> (reset)  ; Reload all namespaces
```

**Babashka task fails:**
```bash
bb tasks  # Verify task exists
bb <task> --help  # See task documentation
```

**Port 8080 in use:**
```bash
lsof -i :8080
kill <pid>
bb dev
```

## Next Steps

- Try `bb dev` to start the integrated environment
- Explore available tasks with `bb tasks`
- Read `CLAUDE.md` for full project documentation
- See `README.md` for project overview

## References

- [Babashka Book](https://book.babashka.org/) - Babashka tasks and scripting
- [shadow-cljs User Guide](https://shadow-cljs.github.io/docs/UsersGuide.html) - Full shadow-cljs documentation
- [Fullstack shadow-cljs Workflow](https://code.thheller.com/blog/shadow-cljs/2024/10/18/fullstack-cljs-workflow-with-shadow-cljs.html) - Thomas Heller's guide (Oct 2024)
- [Re-frame Docs](https://day8.github.io/re-frame/) - Frontend state management
- [Pedestal Docs](https://pedestal.io/) - Backend web framework
