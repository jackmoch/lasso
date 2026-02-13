# Development Guide

## Quick Start - Single Command! ⚡

```bash
bb dev
```

**What happens:**
- 🚀 Starts both frontend (shadow-cljs watch) and backend (Pedestal + REPL) in parallel
- 📺 Shows output from both processes in one terminal
- 🔥 Hot reload enabled automatically
- 🛑 Ctrl+C stops everything cleanly

**What you'll see:**
```
🚀 Starting Lasso development environment...
   • Frontend: shadow-cljs watch (port 8280)
   • Backend: Pedestal server (port 8080) + REPL

⏳ Waiting for services to start...
   Watch for:
   - shadow-cljs: '[:app] Build completed'
   - backend: '✅ Backend ready'

[shadow-cljs output...]
[:app] Compiling ...
[:app] Build completed. (180 files, 179 compiled, 0 warnings, 18s)

[backend output...]
✅ Backend ready on http://localhost:8080
```

**Access the app:**
- Open http://localhost:8080
- Open browser console (F12) to see hot reload messages

---

## Alternative: Two Terminal Mode

If you prefer separate control:

### Terminal 1: Frontend
```bash
npx shadow-cljs watch app
```

### Terminal 2: Backend
```bash
bb backend
```

---

## Hot Reload Testing

1. **Start both terminals** (frontend + backend)
2. **Open http://localhost:8080** in browser
3. **Open browser console** (F12)
4. **Edit** `src/cljs/lasso/views.cljs` line 15:
   ```clojure
   ;; Change this:
   "Lasso"

   ;; To this:
   "Lasso [HOT RELOAD TEST]"
   ```
5. **Save the file**
6. **Watch Terminal 1** for:
   ```
   [:app] Build completed. (1 files, 1 compiled, 0 warnings, 0.5s)
   ```
7. **Watch browser console** for colored logs:
   ```
   🔄 Hot reload starting...
   🔄 Reloading UI...
   ✅ Hot reload complete! Changes applied.
   ```
8. **See title update** without page refresh!

---

## Alternative: One-Terminal Mode

If you prefer one terminal:

```bash
bb dev
```

**Note:** This runs shadow-cljs in background, so you won't see compilation output. Use two-terminal mode for easier debugging.

---

## REPL Commands

```clojure
;; Lifecycle
(start-backend)  ; Start Pedestal server
(stop-backend)   ; Stop server
(restart)        ; Restart server
(reset)          ; Reload namespaces + restart

;; Status
(server/status)  ; Check if server is running
```

---

## Common Issues

### Hot reload not working

**Check Terminal 1** (shadow-cljs):
- Is it showing "Build completed" when you save?
- Any compilation errors?

**Check browser console:**
- Do you see hot reload logs?
- Any JavaScript errors?

**Fix:**
```bash
# Terminal 1: Restart shadow-cljs
Ctrl+C
bb clean:frontend
npx shadow-cljs watch app

# Browser: Hard refresh
Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows)
```

### Port conflicts

```bash
# Kill processes on ports
lsof -ti :8080 | xargs kill -9  # Backend
lsof -ti :8280 | xargs kill -9  # Shadow-cljs
lsof -ti :9630 | xargs kill -9  # Shadow-cljs nREPL
```

### Slow compilation

First compile takes ~20s, subsequent compiles are <2s. If all compiles are slow:

```bash
bb clean:frontend
```

---

## Project Structure

```
src/
├── clj/lasso/          # Backend (Clojure)
│   ├── server.clj      # Server lifecycle
│   ├── routes.clj      # HTTP routes
│   └── ...
└── cljs/lasso/         # Frontend (ClojureScript)
    ├── core.cljs       # App init + hot reload hooks
    ├── views.cljs      # Main UI
    ├── events.cljs     # Re-frame events
    ├── subs.cljs       # Re-frame subscriptions
    └── components/     # UI components
```

---

## Development Tips

1. **Keep both terminals visible** - spot issues immediately
2. **Keep browser console open** - see hot reload feedback
3. **Save often** - hot reload is instant
4. **Use REPL for backend** - test functions interactively
5. **Check logs** - both terminal and browser console
