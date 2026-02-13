# Hot Reload Test

Follow these steps to verify hot reload is working:

## Setup

1. **Start dev environment:**
   ```bash
   bb dev
   ```

2. **Wait for "✅ Lasso Ready" message**

3. **Open browser to http://localhost:8080**

4. **Open browser console (F12 or Cmd+Option+I)**

## Test Hot Reload

### Step 1: Verify Initial Setup

In browser console, you should see:
```
🚀 Lasso initializing...
✅ Lasso ready
```

### Step 2: Make a Visible Change

Open `src/cljs/lasso/views.cljs` and find the navbar function (around line 14).

**Change this:**
```clojure
[:h1.text-3xl.font-bold.text-gray-900.mb-1
 "Lasso"]
```

**To this:**
```clojure
[:h1.text-3xl.font-bold.text-gray-900.mb-1
 "Lasso [HOT RELOAD TEST]"]
```

**Save the file.**

### Step 3: Watch for Reload

**In browser console**, you should see:
```
🔄 Hot reload starting...
🔄 Reloading UI...
✅ Hot reload complete! Changes applied.
```

**On the page**, the title should change from "Lasso" to "Lasso [HOT RELOAD TEST]" **without** refreshing the page.

### Step 4: Verify Terminal Output

**In your terminal** running `bb dev`, you should see:
```
[:app] Build completed. (1 files, 1 compiled, 0 warnings, 0.5s)
```

## Troubleshooting

### ❌ No console logs appear

**Problem:** shadow-cljs watch isn't running or browser isn't connected.

**Fix:**
1. Check terminal - do you see `[:app] Build completed` when you save?
2. If not, restart: `(stop)` then `(start)` in REPL
3. Hard refresh browser (Cmd+Shift+R)

### ❌ Logs appear but UI doesn't update

**Problem:** React isn't re-rendering or components are cached.

**Fix:**
1. Check that you saved the file
2. Hard refresh browser (Cmd+Shift+R)
3. Verify file path is correct: `src/cljs/lasso/views.cljs`
4. Try editing a different component (e.g., session-controls.cljs)

### ❌ Build error appears

**Problem:** Syntax error in your change.

**Fix:**
1. Check terminal for error message
2. Undo your change
3. Save file again
4. Wait for `Build completed` message

### ❌ Build completed but nothing happens

**Problem:** Browser cache or stale build.

**Fix:**
```bash
# In REPL or new terminal:
bb clean:frontend

# Restart dev environment
(stop)
(start)

# Hard refresh browser
```

## Success Criteria

✅ Console logs appear when you save a file
✅ UI updates without manual refresh
✅ Terminal shows "Build completed" message
✅ Changes appear within 1-2 seconds

## What Should Hot Reload

**These update automatically:**
- UI component changes (views, components)
- Event handlers (events.cljs)
- Subscriptions (subs.cljs)
- Style changes (Tailwind classes in hiccup)

**These require browser refresh:**
- Database schema (db.cljs)
- App initialization (init function)
- Route configuration

**These require backend restart:**
- Backend handlers (routes, handlers)
- Configuration changes
- Dependency changes
