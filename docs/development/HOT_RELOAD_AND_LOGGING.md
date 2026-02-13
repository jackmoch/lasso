# Hot Reload and Development Logging

This document explains the hot reload setup and logging improvements for Lasso development.

## Hot Reload Configuration

### How It Works

**shadow-cljs** provides automatic hot reloading for ClojureScript code changes. When you save a file in `src/cljs/`, shadow-cljs:

1. Detects the change
2. Recompiles only the changed namespace
3. Calls the `:before-load` hook (`lasso.core/before-reload`)
4. Loads the new code into the browser
5. Calls the `:after-load` hook (`lasso.core/after-reload`)
6. Re-mounts the UI without losing state

### Configuration Files

**`shadow-cljs.edn`:**
```clojure
:devtools {:before-load lasso.core/before-reload
           :after-load lasso.core/after-reload
           :http-port 8280
           :http-root "resources/public"}
```

**`src/cljs/lasso/core.cljs`:**
```clojure
(defn ^:dev/before-load before-reload
  "Called before hot reload. Clean up state."
  []
  (js/console.log "🔄 Hot reload: cleaning up..."))

(defn ^:dev/after-load after-reload
  "Called after hot reload. Re-mount UI."
  []
  (js/console.log "🔄 Hot reload: re-mounting...")
  (mount-root)
  (js/console.log "✅ Hot reload complete"))
```

### What Gets Hot Reloaded

✅ **Automatic (no browser refresh needed):**
- UI component changes (views, components)
- Event handler logic (events.cljs)
- Subscription logic (subs.cljs)
- Style changes (when using Tailwind classes)

❌ **Requires manual refresh:**
- Database schema changes (db.cljs)
- App initialization logic (init function)
- Route configuration changes
- CSS file changes (run `bb css` to rebuild)

### Troubleshooting Hot Reload

**If changes aren't appearing:**

1. **Check browser console** for hot reload logs:
   ```
   🔄 Hot reload: cleaning up...
   🔄 Hot reload: re-mounting...
   ✅ Hot reload complete
   ```

2. **Check shadow-cljs output** for compilation errors:
   ```
   [:app] Build completed. (180 files, 179 compiled, 0 warnings)
   ```

3. **Clear shadow-cljs cache:**
   ```bash
   bb clean:frontend
   ```

4. **Hard refresh browser:**
   - Chrome/Edge: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows)
   - Firefox: Cmd+Shift+Delete → Clear Cache
   - Or open DevTools → Right-click refresh → "Empty Cache and Hard Reload"

**If compilation is slow:**
- shadow-cljs caches compiled code in `.shadow-cljs/`
- First compile takes ~20s, subsequent compiles are much faster (<2s)
- Cache corruption can cause issues - use `bb clean:frontend` to fix

## Development Logging

### Overview

Lasso uses **clean, organized logging** in development to make it easy to read terminal output.

### Logging Configuration

**`dev/logging.clj`:**
- Configures timbre with clean formatting
- Suppresses verbose Jetty/Pedestal logs
- Provides banner/section helpers for organized output

**`dev/logback.xml`:**
- Configures logback (used by Jetty) to be less verbose
- Only shows WARN level for Jetty
- Only shows INFO level for Pedestal

### Log Levels

| Level | Icon | When to Use |
|-------|------|-------------|
| INFO  | ℹ️   | Important events (server started, session created) |
| WARN  | ⚠️   | Warning conditions (API rate limit, missing config) |
| ERROR | ❌  | Error conditions (API failure, validation error) |
| DEBUG | 🐛  | Detailed debugging (disabled in dev by default) |

### Development Output

When you run `bb dev`, you'll see:

```
┌──────────────────────────────────────────────────────────┐
│                     🚀 Lasso Development                   │
└──────────────────────────────────────────────────────────┘

▸ Frontend

  Starting shadow-cljs watch...
  ✓ Frontend hot reload enabled
  → Watching src/cljs/ for changes

▸ Backend

  ✓ Server started
  → http://localhost:8080

▸ Ready

  Open http://localhost:8080 in your browser
  Watch console for hot reload notifications

  REPL Commands:
    (stop)      - Stop all services
    (restart)   - Restart all services
    (reset)     - Reload namespaces + restart
    (cljs-repl) - Connect to browser REPL
```

### Backend Logging

Backend logs use timbre and appear with emoji prefixes:

```
ℹ️  Server started on port 8080
ℹ️  Session created for user: johndoe
⚠️  Last.fm API rate limit approaching
❌ Failed to fetch recent tracks: API timeout
```

### Frontend Logging

Frontend logs use `js/console.log` and appear in browser console:

```
🚀 Lasso initializing...
✅ Lasso ready
🔄 Hot reload: cleaning up...
🔄 Hot reload: re-mounting...
✅ Hot reload complete
```

### Silenced Logs

To keep output clean, these are suppressed in development:

- Jetty startup messages (verbose)
- Pedestal route compilation details
- shadow-cljs dependency resolution
- SLF4J warnings (expected in dev)

### Adding New Logs

**Backend (Clojure):**
```clojure
(require '[taoensso.timbre :as log])

(log/info "User logged in:" username)
(log/warn "API rate limit:" remaining "requests left")
(log/error "Database error:" (.getMessage ex))
```

**Frontend (ClojureScript):**
```clojure
(js/console.log "ℹ️ User action:" action)
(js/console.warn "⚠️ Validation failed:" errors)
(js/console.error "❌ API call failed:" response)
```

## REPL Development

### Starting REPL

```bash
bb dev  # Starts everything with REPL
```

### REPL Commands

```clojure
;; Lifecycle
(stop)      ; Stop backend + frontend watch
(start)     ; Start backend + frontend watch
(restart)   ; Stop and start
(reset)     ; Reload namespaces + restart

;; Frontend REPL
(cljs-repl) ; Connect to browser for ClojureScript REPL
```

### Reloading Backend Code

**Simple changes (pure functions):**
```clojure
(require 'lasso.lastfm.client :reload)
```

**Route/handler changes:**
```clojure
(restart)  ; Restart server to reload routes
```

**Deep changes (dependencies, schema):**
```clojure
(reset)    ; Reload all namespaces
```

## Best Practices

### During Development

1. **Keep terminal visible** - Watch for compilation errors
2. **Keep browser console open** - Watch for hot reload feedback
3. **Use (reset) liberally** - When in doubt, reload everything
4. **Test hot reload** - Make a small UI change to verify it's working
5. **Clear cache occasionally** - Run `bb clean:frontend` weekly

### Before Commits

1. **Stop dev environment:** `(stop)` in REPL or Ctrl+C
2. **Run tests:** `bb test`
3. **Run linter:** `bb lint`
4. **Clean build:** `bb clean && bb build`

### Debugging

**Frontend issues:**
- Check browser console for errors
- Use React DevTools to inspect component tree
- Use re-frame-10x (future enhancement) for state debugging

**Backend issues:**
- Check terminal output for errors
- Add `log/debug` statements
- Use REPL to test functions interactively

**Hot reload issues:**
- Check shadow-cljs output for compilation errors
- Verify hooks are being called (check browser console)
- Clear cache with `bb clean:frontend`
- Hard refresh browser

## Performance

### Compilation Times

| Task | First Run | Subsequent | Notes |
|------|-----------|------------|-------|
| Frontend compile | ~20s | <2s | Cached in `.shadow-cljs/` |
| CSS rebuild | <1s | <1s | Tailwind is fast |
| Backend startup | <2s | <2s | No compilation needed |
| Namespace reload | <1s | <1s | Only changed namespaces |

### Memory Usage

shadow-cljs can use significant memory (~500MB) for watching and compiling. This is normal for ClojureScript development.

If experiencing performance issues:
1. Close unused browser tabs
2. Restart shadow-cljs: `(restart)`
3. Clear cache: `bb clean:frontend`
4. Increase JVM heap size if needed

## Troubleshooting

### "Port 8080 already in use"

```bash
lsof -i :8080  # Find process
kill -9 <PID>  # Kill it
```

Or change port in `.env`:
```
PORT=8081
```

### "shadow-cljs build failed"

1. Clear cache: `bb clean:frontend`
2. Check shadow-cljs.edn syntax
3. Verify all namespaces are valid
4. Check browser console for errors

### "Hot reload not working"

1. Verify shadow-cljs watch is running (check terminal)
2. Check browser console for reload logs
3. Hard refresh browser (Cmd+Shift+R)
4. Restart dev environment: `(restart)`

### "Changes require full restart"

Some changes can't be hot reloaded:
- Database schema (db.cljs)
- App initialization (init function)
- Route configuration

For these, use `(reset)` or restart the browser.
