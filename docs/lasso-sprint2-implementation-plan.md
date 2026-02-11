# SPRINT 2 IMPLEMENTATION PLAN: LASSO

**Sprint:** 2  
**Milestone:** M2 - Design Phase  
**Sprint Goal:** Complete technical design and establish a working development environment with full project scaffolding  
**Document Version:** 1.0  
**Document Date:** February 4, 2026

---

## Table of Contents
1. [Sprint Overview](#sprint-overview)
2. [Definition of Done](#definition-of-done)
3. [Task Breakdown](#task-breakdown)
4. [Detailed Implementation Guide](#detailed-implementation-guide)
5. [Verification Checklist](#verification-checklist)
6. [Troubleshooting](#troubleshooting)

---

## 1. Sprint Overview

### Sprint Objectives
- ✅ Complete Technical Design Document (COMPLETE)
- ✅ Define system architecture (COMPLETE)
- ✅ Plan API integration (COMPLETE)
- 🔲 Set up local development environment
- 🔲 Create project structure and scaffolding
- 🔲 Configure all build tools
- 🔲 Verify end-to-end toolchain works
- 🔲 Establish CI/CD pipeline skeleton

### Success Criteria
By the end of Sprint 2, you should be able to:
1. Start backend server via REPL and see "Hello World"
2. Start frontend development server and see rendered page
3. Make a code change and see hot reload work
4. Run tests (even if just example tests)
5. Build Docker image successfully
6. Push code and see CI pipeline run

### Prerequisites
- Last.fm API account and API keys obtained
- Google Cloud Platform project created
- GitHub repository ready
- VS Code installed

---

## 2. Definition of Done

### Task-Level DoD
A task is complete when:
- [ ] Implementation steps completed
- [ ] Success criteria met (can be verified)
- [ ] Documentation updated (if applicable)
- [ ] Committed to git with descriptive message
- [ ] No blocking errors or warnings

### Sprint-Level DoD
Sprint 2 is complete when:
- [ ] All tasks marked complete
- [ ] Full Verification Checklist passes
- [ ] Can demonstrate end-to-end: edit code → save → see change
- [ ] CI pipeline runs successfully
- [ ] Docker image builds successfully
- [ ] README.md has setup instructions

---

## 3. Task Breakdown

### Epic: Development Environment Setup

#### **Task 2.1: Install Development Tools**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 30 minutes  
**Dependencies:** None

**Description:**
Install all required software for Clojure/ClojureScript development.

**Subtasks:**
1. Install JDK 11 or higher
2. Install Clojure CLI tools
3. Install Node.js 18+
4. Install VS Code (if not already installed)
5. Install Calva extension for VS Code
6. Install Git (if not already installed)

**Acceptance Criteria:**
- [ ] `java -version` shows Java 11+
- [ ] `clojure --version` works
- [ ] `node --version` shows v18+
- [ ] `npm --version` works
- [ ] VS Code opens
- [ ] Calva extension visible in VS Code extensions
- [ ] `git --version` works

**Verification:**
```bash
java -version        # Should show 11 or higher
clojure --version   # Should show Clojure CLI version
node --version      # Should show v18+
npm --version       # Should show npm version
git --version       # Should show git version
```

---

#### **Task 2.2: Configure Last.fm API Access**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 15 minutes  
**Dependencies:** Task 2.1

**Description:**
Set up Last.fm API credentials for development.

**Subtasks:**
1. Log into Last.fm API account
2. Create new API application
3. Copy API Key and Shared Secret
4. Document callback URL requirements

**Acceptance Criteria:**
- [ ] Last.fm API Key obtained
- [ ] Last.fm API Secret obtained
- [ ] API credentials documented securely (password manager or secure notes)
- [ ] Callback URL noted: `http://localhost:8080/api/auth/callback`

**Verification:**
- Can view API credentials in Last.fm account
- Have both API Key and Secret available for configuration

---

#### **Task 2.3: Set Up Google Cloud Project**
**Priority:** P1 (Important)  
**Estimated Effort:** 20 minutes  
**Dependencies:** Task 2.1

**Description:**
Prepare Google Cloud Platform for deployment.

**Subtasks:**
1. Create new GCP project (or select existing)
2. Enable required APIs (Cloud Run, Container Registry, Cloud Build)
3. Create service account for CI/CD
4. Generate and download service account key
5. Set up billing (if not already configured)

**Acceptance Criteria:**
- [ ] GCP project exists and is accessible
- [ ] Cloud Run API enabled
- [ ] Container Registry API enabled
- [ ] Cloud Build API enabled
- [ ] Service account created with appropriate permissions
- [ ] Service account JSON key downloaded
- [ ] Billing configured (required for Cloud Run)

**Verification:**
- Can access GCP Console for project
- Can run: `gcloud config set project YOUR_PROJECT_ID`
- Service account key file exists

---

### Epic: Project Scaffolding

#### **Task 2.4: Initialize Git Repository**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 10 minutes  
**Dependencies:** Task 2.1

**Description:**
Create Git repository with proper structure.

**Subtasks:**
1. Create GitHub repository named "lasso"
2. Clone repository locally
3. Create initial directory structure
4. Create .gitignore file
5. Create initial README.md
6. Make initial commit

**Acceptance Criteria:**
- [ ] GitHub repository exists and is accessible
- [ ] Local clone exists in workspace
- [ ] Directory structure created (see structure below)
- [ ] .gitignore includes proper ignores
- [ ] README.md has project title and basic description
- [ ] Initial commit pushed to GitHub

**Directory Structure:**
```
lasso/
├── .github/
│   └── workflows/
├── src/
│   ├── clj/
│   │   └── lasso/
│   └── cljs/
│       └── lasso/
├── test/
│   ├── clj/
│   │   └── lasso/
│   └── cljs/
│       └── lasso/
├── resources/
│   └── public/
│       └── css/
├── dev/
├── .gitignore
└── README.md
```

**.gitignore contents:**
```
# Clojure
.cpcache/
.nrepl-port
.rebel_readline_history
*.class

# ClojureScript
.shadow-cljs/
node_modules/
package-lock.json

# Build artifacts
target/
out/
resources/public/js/

# IDE
.calva/
.clj-kondo/.cache/
.lsp/
.vscode/

# OS
.DS_Store
Thumbs.db

# Environment
.env
.env.local

# Logs
*.log
```

**Verification:**
- Repository exists on GitHub
- `git status` works in local directory
- Can push and pull from remote

---

#### **Task 2.5: Configure Clojure Dependencies (deps.edn)**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 30 minutes  
**Dependencies:** Task 2.4

**Description:**
Create deps.edn with all backend dependencies and aliases.

**Subtasks:**
1. Create deps.edn in project root
2. Add core Clojure dependencies
3. Add Pedestal dependencies
4. Add utility libraries (timbre, malli, buddy, etc.)
5. Configure development alias
6. Configure REPL alias
7. Configure test alias
8. Configure uberjar alias
9. Test dependency resolution

**Acceptance Criteria:**
- [ ] deps.edn file exists in project root
- [ ] All dependencies from TDD included
- [ ] All aliases configured
- [ ] Dependencies resolve without errors
- [ ] Versions match TDD specifications

**deps.edn contents:**
```clojure
{:paths ["src/clj" "resources"]
 
 :deps {org.clojure/clojure {:mvn/version "1.11.1"}
        org.clojure/core.async {:mvn/version "1.6.681"}
        
        ;; Web Framework
        io.pedestal/pedestal.service {:mvn/version "0.7.0"}
        io.pedestal/pedestal.jetty {:mvn/version "0.7.0"}
        
        ;; HTTP Client
        clj-http/clj-http {:mvn/version "3.13.0"}
        
        ;; Validation
        metosin/malli {:mvn/version "0.16.0"}
        
        ;; Security
        buddy/buddy-core {:mvn/version "1.11.1"}
        
        ;; Logging
        com.taoensso/timbre {:mvn/version "6.5.0"}
        
        ;; JSON
        cheshire/cheshire {:mvn/version "5.12.0"}}
 
 :aliases
 {:dev {:extra-paths ["dev" "test/clj"]
        :extra-deps {org.clojure/tools.namespace {:mvn/version "1.4.4"}}}
  
  :repl {:main-opts ["-m" "nrepl.cmdline"
                     "--middleware" "[cider.nrepl/cider-middleware]"]
         :extra-deps {nrepl/nrepl {:mvn/version "1.1.1"}
                      cider/cider-nrepl {:mvn/version "0.47.1"}}}
  
  :test {:extra-paths ["test/clj"]
         :extra-deps {lambdaisland/kaocha {:mvn/version "1.87.1366"}
                      org.clojure/test.check {:mvn/version "1.1.1"}}
         :main-opts ["-m" "kaocha.runner"]}
  
  :uberjar {:replace-deps {com.github.seancorfield/depstar {:mvn/version "2.1.303"}}
            :exec-fn hf.depstar/uberjar
            :exec-args {:jar "target/lasso.jar"
                        :aot true
                        :main-class lasso.server}}}}
```

**Verification:**
```bash
# Download dependencies
clojure -P

# Test REPL alias
clojure -M:repl
# Should start nREPL server
```

**Commit Message:**
```
chore: configure Clojure dependencies and build aliases
```

---

#### **Task 2.6: Configure ClojureScript Build (shadow-cljs.edn)**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 30 minutes  
**Dependencies:** Task 2.5

**Description:**
Set up shadow-cljs for ClojureScript compilation and hot reloading.

**Subtasks:**
1. Install shadow-cljs via npm
2. Create shadow-cljs.edn configuration
3. Add ClojureScript dependencies
4. Configure development build
5. Configure release build
6. Set up dev HTTP server

**Acceptance Criteria:**
- [ ] package.json exists with shadow-cljs dependency
- [ ] shadow-cljs.edn exists with proper configuration
- [ ] Development build configured
- [ ] Release build configured
- [ ] Dev server port configured (8280)

**package.json:**
```json
{
  "name": "lasso",
  "version": "1.0.0",
  "description": "Last.fm scrobble following for Spotify Jams",
  "scripts": {
    "watch": "shadow-cljs watch app",
    "release": "shadow-cljs release app",
    "clean": "rm -rf resources/public/js .shadow-cljs"
  },
  "dependencies": {
    "shadow-cljs": "^2.27.1",
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "tailwindcss": "^3.4.0"
  }
}
```

**shadow-cljs.edn:**
```clojure
{:source-paths ["src/cljs"]
 
 :dependencies [[reagent "1.2.0"]
                [re-frame "1.4.3"]
                [metosin/reitit-frontend "0.7.0"]
                [cljs-ajax "0.8.4"]
                [metosin/malli "0.16.0"]
                [day8.re-frame/http-fx "0.2.4"]]
 
 :dev-http {8280 "resources/public"}
 
 :builds
 {:app {:target :browser
        :output-dir "resources/public/js"
        :asset-path "/js"
        :modules {:main {:init-fn lasso.core/init}}
        :devtools {:after-load lasso.core/mount-root
                   :preloads [devtools.preload]}
        :dev {:compiler-options {:closure-defines {re-frame.trace.trace-enabled? true}}}
        :release {:compiler-options {:optimizations :advanced
                                     :infer-externs :auto}}}}}
```

**Verification:**
```bash
# Install npm dependencies
npm install

# Verify shadow-cljs works
npx shadow-cljs --version
```

**Commit Message:**
```
chore: configure shadow-cljs for frontend builds
```

---

#### **Task 2.7: Configure Tailwind CSS**
**Priority:** P1 (Important)  
**Estimated Effort:** 20 minutes  
**Dependencies:** Task 2.6

**Description:**
Set up Tailwind CSS for styling.

**Subtasks:**
1. Install Tailwind CSS
2. Create tailwind.config.js
3. Create input CSS file
4. Configure build script
5. Test CSS compilation

**Acceptance Criteria:**
- [ ] Tailwind installed via npm
- [ ] tailwind.config.js exists
- [ ] Input CSS file created
- [ ] CSS compiles successfully
- [ ] Output CSS file generated

**tailwind.config.js:**
```javascript
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/cljs/**/*.cljs",
    "./resources/public/index.html"
  ],
  theme: {
    extend: {
      colors: {
        'lastfm-red': '#D51007',
      }
    },
  },
  plugins: [],
}
```

**resources/public/css/input.css:**
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Custom styles */
@layer components {
  .btn-primary {
    @apply bg-lastfm-red hover:bg-red-700 text-white font-bold py-2 px-4 rounded;
  }
  
  .card {
    @apply bg-white shadow-md rounded-lg p-6;
  }
}
```

**Update package.json scripts:**
```json
{
  "scripts": {
    "watch": "shadow-cljs watch app",
    "watch:css": "tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css --watch",
    "build:css": "tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css --minify",
    "release": "npm run build:css && shadow-cljs release app",
    "clean": "rm -rf resources/public/js resources/public/css/tailwind.css .shadow-cljs"
  }
}
```

**Verification:**
```bash
# Build CSS
npm run build:css

# Verify output file exists
ls resources/public/css/tailwind.css
```

**Commit Message:**
```
chore: configure Tailwind CSS for styling
```

---

#### **Task 2.8: Create Basic HTML Template**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 15 minutes  
**Dependencies:** Task 2.7

**Description:**
Create the base HTML file that loads the ClojureScript app.

**Subtasks:**
1. Create index.html in resources/public
2. Add meta tags
3. Link CSS
4. Add app mount point
5. Link JavaScript bundle

**Acceptance Criteria:**
- [ ] index.html exists in resources/public
- [ ] Links to Tailwind CSS
- [ ] Has div with id="app"
- [ ] Links to compiled JavaScript
- [ ] Has appropriate meta tags

**resources/public/index.html:**
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Lasso - Track your Spotify Jam scrobbles on Last.fm">
    <title>Lasso - Last.fm Scrobble Following</title>
    <link rel="stylesheet" href="/css/tailwind.css">
</head>
<body class="bg-gray-100">
    <div id="app"></div>
    <script src="/js/main.js"></script>
</body>
</html>
```

**Verification:**
- File exists at correct path
- Can open in browser (won't render properly yet, but should load)

**Commit Message:**
```
feat: add base HTML template
```

---

#### **Task 2.9: Create Basic Backend Namespaces**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 45 minutes  
**Dependencies:** Task 2.5

**Description:**
Create skeleton backend namespaces with basic "Hello World" functionality.

**Subtasks:**
1. Create lasso.server namespace
2. Create lasso.config namespace
3. Create lasso.routes namespace
4. Create dev/user.clj for REPL utilities
5. Implement basic server start/stop
6. Implement basic "Hello World" route
7. Test server startup

**Acceptance Criteria:**
- [ ] src/clj/lasso/server.clj exists and compiles
- [ ] src/clj/lasso/config.clj exists and compiles
- [ ] src/clj/lasso/routes.clj exists and compiles
- [ ] dev/user.clj exists with REPL helpers
- [ ] Server can start and stop
- [ ] Can access http://localhost:8080 and see response

**src/clj/lasso/server.clj:**
```clojure
(ns lasso.server
  (:require [io.pedestal.http :as http]
            [lasso.config :as config]
            [lasso.routes :as routes]))

(defonce server (atom nil))

(defn create-server []
  (http/create-server
    {::http/routes routes/routes
     ::http/type :jetty
     ::http/host (config/get :host "0.0.0.0")
     ::http/port (config/get :port 8080)
     ::http/resource-path "public"}))

(defn start []
  (reset! server (http/start (create-server)))
  (println "Server started on port" (config/get :port 8080)))

(defn stop []
  (when @server
    (http/stop @server)
    (println "Server stopped")))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (start))
```

**src/clj/lasso/config.clj:**
```clojure
(ns lasso.config)

(def config
  (atom {:port (or (some-> (System/getenv "PORT") Integer/parseInt) 8080)
         :host (or (System/getenv "HOST") "0.0.0.0")
         :environment (keyword (or (System/getenv "ENVIRONMENT") "development"))}))

(defn get 
  ([k] (clojure.core/get @config k))
  ([k default] (clojure.core/get @config k default)))

(defn dev-mode? []
  (= :development (get :environment)))
```

**src/clj/lasso/routes.clj:**
```clojure
(ns lasso.routes
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn home-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "resources/public/index.html")})

(defn health-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"status\":\"ok\"}"})

(def routes
  (route/expand-routes
    #{["/" :get home-handler :route-name :home]
      ["/health" :get health-handler :route-name :health]}))
```

**dev/user.clj:**
```clojure
(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [lasso.server :as server]))

(defn start []
  (server/start))

(defn stop []
  (server/stop))

(defn restart []
  (server/restart))

(defn reset []
  (stop)
  (repl/refresh :after 'user/start))

(comment
  ;; Start server
  (start)
  
  ;; Stop server
  (stop)
  
  ;; Restart server
  (restart)
  
  ;; Full reset (reload namespaces)
  (reset)
  )
```

**Verification:**
```bash
# Start REPL
clj -M:dev:repl

# In REPL:
user=> (start)
# Should see "Server started on port 8080"

# In browser, visit:
# http://localhost:8080/health
# Should see: {"status":"ok"}

# http://localhost:8080/
# Should see index.html (blank page for now)
```

**Commit Message:**
```
feat: add basic backend server with hello world routes
```

---

#### **Task 2.10: Create Basic Frontend Namespaces**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 45 minutes  
**Dependencies:** Task 2.8

**Description:**
Create skeleton ClojureScript namespaces with basic React rendering.

**Subtasks:**
1. Create lasso.core namespace
2. Create lasso.views namespace
3. Implement basic Re-frame initialization
4. Implement basic view rendering
5. Test frontend compilation
6. Test hot reload

**Acceptance Criteria:**
- [ ] src/cljs/lasso/core.cljs exists and compiles
- [ ] src/cljs/lasso/views.cljs exists and compiles
- [ ] Frontend compiles without errors
- [ ] Can see "Hello World" in browser
- [ ] Hot reload works (change text, see update)

**src/cljs/lasso/core.cljs:**
```clojure
(ns lasso.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [lasso.views :as views]))

;; -- Re-frame Event Handlers --

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   {:app-name "Lasso"
    :message "Track your Spotify Jam scrobbles on Last.fm"}))

;; -- Re-frame Subscriptions --

(rf/reg-sub
 :app-name
 (fn [db _]
   (:app-name db)))

(rf/reg-sub
 :message
 (fn [db _]
   (:message db)))

;; -- Entry Point --

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (rdom/render [views/main-panel]
               (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))
```

**src/cljs/lasso/views.cljs:**
```clojure
(ns lasso.views
  (:require [re-frame.core :as rf]))

(defn main-panel []
  (let [app-name @(rf/subscribe [:app-name])
        message @(rf/subscribe [:message])]
    [:div.min-h-screen.bg-gray-100.flex.items-center.justify-center
     [:div.text-center
      [:h1.text-4xl.font-bold.text-gray-900.mb-4 app-name]
      [:p.text-xl.text-gray-600 message]
      [:div.mt-8
       [:button.btn-primary "Get Started"]]]]))
```

**Verification:**
```bash
# Start shadow-cljs watch (in separate terminal)
npx shadow-cljs watch app

# Should see:
# [:app] Build completed. (XXX files, X compiled, X warnings, X.XXs)

# Visit http://localhost:8280 (or :8080 if backend serving)
# Should see "Lasso" title and message

# Edit message in core.cljs and save
# Should see page update automatically
```

**Commit Message:**
```
feat: add basic frontend with Re-frame and Reagent
```

---

#### **Task 2.11: Create Environment Configuration Template**
**Priority:** P1 (Important)  
**Estimated Effort:** 10 minutes  
**Dependencies:** Task 2.9

**Description:**
Create .env.example template for environment variables.

**Subtasks:**
1. Create .env.example file
2. Document all required variables
3. Add instructions in README

**Acceptance Criteria:**
- [ ] .env.example exists in project root
- [ ] All environment variables documented
- [ ] .env added to .gitignore (already done in Task 2.4)
- [ ] README updated with configuration instructions

**.env.example:**
```bash
# Last.fm API Configuration
LASTFM_API_KEY=your_api_key_here
LASTFM_API_SECRET=your_api_secret_here
OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback

# Session Configuration
SESSION_SECRET=generate_random_secret_at_least_32_chars

# Server Configuration
PORT=8080
HOST=0.0.0.0
ENVIRONMENT=development

# Polling Configuration (milliseconds)
POLLING_INTERVAL_MS=20000
```

**Update README.md:**
Add configuration section:
```markdown
## Configuration

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and fill in your Last.fm API credentials:
   - Get API credentials from: https://www.last.fm/api/account/create
   - Generate a secure SESSION_SECRET: `openssl rand -base64 32`

3. For development, the defaults in `.env.example` should work.
```

**Verification:**
- .env.example exists
- Can copy to .env and edit

**Commit Message:**
```
chore: add environment configuration template
```

---

### Epic: Docker & CI/CD Setup

#### **Task 2.12: Create Dockerfile**
**Priority:** P1 (Important)  
**Estimated Effort:** 30 minutes  
**Dependencies:** Task 2.10

**Description:**
Create multi-stage Dockerfile for building and running application.

**Subtasks:**
1. Create Dockerfile
2. Create .dockerignore
3. Test Docker build locally
4. Document Docker commands

**Acceptance Criteria:**
- [ ] Dockerfile exists in project root
- [ ] .dockerignore exists
- [ ] Docker image builds successfully
- [ ] Docker container runs and serves app
- [ ] Image size is reasonable (<500MB)

**Dockerfile:**
```dockerfile
# Multi-stage build

# Stage 1: Build frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm install

# Copy source and config
COPY shadow-cljs.edn tailwind.config.js ./
COPY src/cljs ./src/cljs
COPY resources/public ./resources/public

# Build frontend
RUN npx shadow-cljs release app
RUN npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css --minify

# Stage 2: Build backend
FROM clojure:temurin-11-tools-deps AS backend-builder
WORKDIR /app

# Copy deps file and download dependencies
COPY deps.edn ./
RUN clojure -P -X:uberjar

# Copy source
COPY src/clj ./src/clj

# Build uberjar
RUN clojure -X:uberjar

# Stage 3: Runtime
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

# Copy uberjar from backend builder
COPY --from=backend-builder /app/target/lasso.jar ./lasso.jar

# Copy built frontend from frontend builder
COPY --from=frontend-builder /app/resources/public ./resources/public

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Run application
CMD ["java", "-jar", "lasso.jar"]
```

**.dockerignore:**
```
.git
.github
.gitignore
node_modules
target
.shadow-cljs
.calva
.clj-kondo
.lsp
*.log
.env
.env.*
!.env.example
README.md
*.md
```

**Verification:**
```bash
# Build image
docker build -t lasso:dev .

# Run container
docker run -p 8080:8080 \
  -e LASTFM_API_KEY=test \
  -e LASTFM_API_SECRET=test \
  -e OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback \
  -e SESSION_SECRET=test-secret-at-least-32-chars \
  lasso:dev

# Visit http://localhost:8080
# Should see app running

# Check image size
docker images lasso:dev
# Should be under 500MB
```

**Commit Message:**
```
chore: add Dockerfile for containerized deployment
```

---

#### **Task 2.13: Create GitHub Actions CI Workflow**
**Priority:** P1 (Important)  
**Estimated Effort:** 30 minutes  
**Dependencies:** Task 2.12

**Description:**
Set up basic CI pipeline for automated testing and linting.

**Subtasks:**
1. Create .github/workflows/ci.yml
2. Configure linting step
3. Configure test step (when tests exist)
4. Test workflow triggers

**Acceptance Criteria:**
- [ ] .github/workflows/ci.yml exists
- [ ] Workflow runs on pull requests
- [ ] Workflow runs on push to main
- [ ] Linting step executes
- [ ] Build verification succeeds

**.github/workflows/ci.yml:**
```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '11'
    
    - name: Setup Clojure
      uses: DeLaGuardo/setup-clojure@12.5
      with:
        cli: 1.11.1.1435
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
    
    - name: Cache Clojure dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.m2
          ~/.gitlibs
        key: ${{ runner.os }}-clojure-${{ hashFiles('deps.edn') }}
        restore-keys: |
          ${{ runner.os }}-clojure-
    
    - name: Cache npm dependencies
      uses: actions/cache@v3
      with:
        path: node_modules
        key: ${{ runner.os }}-node-${{ hashFiles('package-lock.json') }}
        restore-keys: |
          ${{ runner.os }}-node-
    
    - name: Install Clojure dependencies
      run: clojure -P
    
    - name: Install npm dependencies
      run: npm install
    
    - name: Lint Clojure code
      run: |
        curl -sLO https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo
        chmod +x install-clj-kondo
        ./install-clj-kondo
        ./clj-kondo --lint src
    
    - name: Build frontend
      run: npx shadow-cljs release app
    
    - name: Build CSS
      run: npm run build:css
    
    - name: Build backend
      run: clojure -X:uberjar
    
    # - name: Run tests
    #   run: clojure -M:test
    #   # Uncomment when tests are implemented

  docker-build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Build Docker image
      run: docker build -t lasso:ci .
```

**Verification:**
```bash
# Push changes to trigger workflow
git add .github/workflows/ci.yml
git commit -m "chore: add CI workflow"
git push

# Check GitHub Actions tab
# Workflow should run and pass
```

**Commit Message:**
```
chore: add GitHub Actions CI workflow
```

---

#### **Task 2.14: Create GitHub Actions Deploy Workflow (Skeleton)**
**Priority:** P2 (Nice to have)  
**Estimated Effort:** 20 minutes  
**Dependencies:** Task 2.13

**Description:**
Create skeleton deployment workflow for Cloud Run (will be completed in Sprint 8).

**Subtasks:**
1. Create .github/workflows/deploy.yml
2. Add placeholder steps
3. Document required secrets
4. Add comments for future implementation

**Acceptance Criteria:**
- [ ] .github/workflows/deploy.yml exists
- [ ] Workflow disabled or manual trigger only
- [ ] Required secrets documented
- [ ] Clear TODOs for Sprint 8

**.github/workflows/deploy.yml:**
```yaml
name: Deploy to Google Cloud Run

on:
  # Disabled for now - will enable in Sprint 8
  workflow_dispatch:
  # push:
  #   branches: [main]

env:
  PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
  SERVICE: lasso
  REGION: us-central1

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    # TODO Sprint 8: Uncomment and configure
    # - name: Setup Google Cloud
    #   uses: google-github-actions/setup-gcloud@v1
    #   with:
    #     service_account_key: ${{ secrets.GCP_SA_KEY }}
    #     project_id: ${{ secrets.GCP_PROJECT_ID }}
    
    # - name: Configure Docker
    #   run: gcloud auth configure-docker
    
    # - name: Build container
    #   run: docker build -t gcr.io/$PROJECT_ID/$SERVICE:$GITHUB_SHA .
    
    # - name: Push container
    #   run: docker push gcr.io/$PROJECT_ID/$SERVICE:$GITHUB_SHA
    
    # - name: Deploy to Cloud Run
    #   run: |
    #     gcloud run deploy $SERVICE \
    #       --image gcr.io/$PROJECT_ID/$SERVICE:$GITHUB_SHA \
    #       --platform managed \
    #       --region $REGION \
    #       --allow-unauthenticated
    
    - name: Placeholder
      run: echo "Deployment workflow will be completed in Sprint 8"
```

**Create docs/DEPLOYMENT_SECRETS.md:**
```markdown
# Required GitHub Secrets for Deployment

The following secrets need to be configured in GitHub Settings > Secrets:

## GCP_PROJECT_ID
Your Google Cloud Platform project ID.

Example: `my-lasso-project-12345`

## GCP_SA_KEY
Service account JSON key with permissions:
- Cloud Run Admin
- Cloud Build Editor
- Storage Admin (for Container Registry)

To create:
1. Go to GCP Console > IAM & Admin > Service Accounts
2. Create service account named "github-actions"
3. Grant required roles
4. Create JSON key
5. Copy entire JSON content to this secret

## Configuration for Sprint 8
These secrets will be configured during Sprint 8 deployment setup.
```

**Commit Message:**
```
chore: add skeleton deployment workflow
```

---

#### **Task 2.15: Update README with Setup Instructions**
**Priority:** P0 (Blocking)  
**Estimated Effort:** 30 minutes  
**Dependencies:** All previous tasks

**Description:**
Create comprehensive README with setup and development instructions.

**Subtasks:**
1. Add project description
2. Add prerequisites
3. Add setup instructions
4. Add development workflow
5. Add testing instructions
6. Add deployment notes

**Acceptance Criteria:**
- [ ] README is comprehensive
- [ ] Setup instructions are step-by-step
- [ ] All commands are documented
- [ ] Links to relevant documentation included

**README.md:**
```markdown
# Lasso

Track your Spotify Jam scrobbles on Last.fm.

## Overview

Lasso enables Last.fm users to maintain complete listening history during Spotify Jam sessions by allowing them to temporarily follow and mirror another user's scrobbles.

## Prerequisites

- Java 11 or higher
- Clojure CLI tools (1.11+)
- Node.js 18+
- Docker (for deployment)
- Last.fm API account

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/lasso.git
cd lasso
```

### 2. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env and add your Last.fm API credentials
# Get credentials from: https://www.last.fm/api/account/create
```

### 3. Install Dependencies

```bash
# Install Clojure dependencies
clojure -P

# Install Node dependencies
npm install
```

### 4. Start Development Servers

**Terminal 1: Backend (REPL)**
```bash
clj -M:dev:repl

# In REPL:
user=> (start)
```

**Terminal 2: Frontend (shadow-cljs)**
```bash
npx shadow-cljs watch app
```

**Terminal 3: CSS (Tailwind)**
```bash
npm run watch:css
```

### 5. Open Application

- Backend: http://localhost:8080
- Frontend Dev Server: http://localhost:8280

## Development Workflow

### REPL-Driven Development

```clojure
;; Start server
user=> (start)

;; Make changes to code, then:
user=> (restart)  ; Restart server with changes

;; Or for full reload:
user=> (reset)    ; Stop, reload all namespaces, restart
```

### Frontend Hot Reload

1. Edit files in `src/cljs/`
2. Save
3. See changes immediately in browser

### Running Tests

```bash
# Backend tests
clojure -M:test

# Frontend tests (when implemented)
npx shadow-cljs compile test
node target/test.js
```

### Linting

```bash
# Install clj-kondo (one time)
brew install clj-kondo  # macOS
# or download from: https://github.com/clj-kondo/clj-kondo

# Run linter
clj-kondo --lint src
```

### Building for Production

```bash
# Build frontend
npx shadow-cljs release app

# Build CSS
npm run build:css

# Build backend uberjar
clojure -X:uberjar

# Or build Docker image
docker build -t lasso:latest .
```

## Project Structure

```
lasso/
├── src/
│   ├── clj/          # Clojure backend code
│   └── cljs/         # ClojureScript frontend code
├── test/             # Tests
├── resources/        # Static assets
├── dev/              # REPL utilities
├── deps.edn          # Clojure dependencies
├── shadow-cljs.edn   # ClojureScript build config
└── Dockerfile        # Container definition
```

## Documentation

- [Project Charter](docs/lasso-project-charter.md)
- [Product Requirements](docs/lasso-prd.md)
- [Technical Design](docs/lasso-tdd.md)

## License

MIT

## Contributing

This is a personal project, but feedback and suggestions are welcome via issues.
```

**Commit Message:**
```
docs: add comprehensive README with setup instructions
```

---

## 4. Detailed Implementation Guide

### Getting Started

1. **Start with Task 2.1** - Install all development tools first
2. **Work through tasks in order** - They have dependencies
3. **Commit after each task** - Use provided commit messages
4. **Test as you go** - Run verification steps
5. **Ask for help** - If stuck, refer to troubleshooting section

### Daily Workflow

Each day, start with:
```bash
# Terminal 1: Backend REPL
clj -M:dev:repl
# Then: user=> (start)

# Terminal 2: Frontend
npx shadow-cljs watch app

# Terminal 3: CSS
npm run watch:css
```

### Testing Your Setup

After completing all tasks, run through this sequence:

1. **Backend Health Check**
   ```bash
   curl http://localhost:8080/health
   # Should return: {"status":"ok"}
   ```

2. **Frontend Rendering**
   - Visit http://localhost:8080
   - Should see "Lasso" title and button

3. **Hot Reload Test**
   - Edit message in `src/cljs/lasso/core.cljs`
   - Save file
   - See immediate update in browser

4. **REPL Test**
   ```clojure
   user=> (restart)
   # Server should restart
   ```

5. **Build Test**
   ```bash
   docker build -t lasso:test .
   # Should complete successfully
   ```

---

## 5. Verification Checklist

Use this checklist to verify Sprint 2 completion:

### Environment Setup
- [ ] Java 11+ installed and working
- [ ] Clojure CLI installed and working
- [ ] Node.js 18+ installed and working
- [ ] VS Code with Calva installed
- [ ] Last.fm API credentials obtained
- [ ] Google Cloud project configured

### Project Structure
- [ ] Git repository initialized
- [ ] Directory structure created
- [ ] .gitignore configured
- [ ] deps.edn configured
- [ ] shadow-cljs.edn configured
- [ ] package.json configured
- [ ] Tailwind configured

### Backend
- [ ] Backend REPL starts successfully
- [ ] Server starts on port 8080
- [ ] /health endpoint returns 200 OK
- [ ] / endpoint serves index.html
- [ ] Can restart server from REPL

### Frontend
- [ ] shadow-cljs compiles without errors
- [ ] Frontend displays in browser
- [ ] Hot reload works
- [ ] Tailwind styles are applied
- [ ] React DevTools shows component tree

### Build & Deploy
- [ ] CSS builds successfully
- [ ] Frontend release build works
- [ ] Backend uberjar builds successfully
- [ ] Docker image builds successfully
- [ ] Docker container runs
- [ ] CI workflow runs successfully

### Documentation
- [ ] README.md is complete
- [ ] .env.example exists
- [ ] All files committed to git
- [ ] No sensitive data in repository

### Final Verification
- [ ] Can run full development environment
- [ ] Can make code change and see it reflected
- [ ] No errors in browser console
- [ ] No errors in backend logs
- [ ] Docker build completes in <5 minutes

---

## 6. Troubleshooting

### Common Issues

#### Issue: `clojure: command not found`
**Solution:**
```bash
# macOS
brew install clojure/tools/clojure

# Linux
curl -O https://download.clojure.org/install/linux-install-1.11.1.1435.sh
chmod +x linux-install-1.11.1.1435.sh
sudo ./linux-install-1.11.1.1435.sh
```

#### Issue: `npm: command not found`
**Solution:**
```bash
# Install Node.js from https://nodejs.org/
# Or use nvm:
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
```

#### Issue: shadow-cljs build fails
**Solution:**
```bash
# Clear shadow-cljs cache
rm -rf .shadow-cljs
npx shadow-cljs clean

# Reinstall dependencies
rm -rf node_modules
npm install

# Try build again
npx shadow-cljs watch app
```

#### Issue: Backend won't start
**Solution:**
```bash
# Check if port 8080 is in use
lsof -i :8080

# Kill process if needed
kill -9 <PID>

# Clear .cpcache
rm -rf .cpcache

# Download dependencies again
clojure -P

# Try starting again
```

#### Issue: REPL won't connect in Calva
**Solution:**
1. Open Command Palette (Cmd/Ctrl+Shift+P)
2. Select "Calva: Start a Project REPL and Connect"
3. Choose "deps.edn"
4. Select `:dev:repl` alias
5. Wait for REPL to start

#### Issue: Hot reload not working
**Solution:**
```bash
# Check shadow-cljs is watching
# Should see output like:
# [:app] Build completed. (127 files, 0 compiled, 0 warnings, 0.89s)

# If not, restart:
npx shadow-cljs stop
npx shadow-cljs watch app

# Clear browser cache
# Hard refresh: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows/Linux)
```

#### Issue: Tailwind classes not working
**Solution:**
```bash
# Rebuild CSS
npm run build:css

# Check output file exists
ls resources/public/css/tailwind.css

# Verify tailwind.config.js content paths are correct

# Hard refresh browser
```

#### Issue: Docker build fails
**Solution:**
```bash
# Check Docker is running
docker ps

# Clean Docker build cache
docker builder prune

# Build with no cache
docker build --no-cache -t lasso:test .

# Check Dockerfile syntax
# Verify all COPY paths exist
```

### Getting Help

If you encounter issues not covered here:

1. Check the error message carefully
2. Search for the error online
3. Check relevant documentation:
   - Clojure: https://clojure.org
   - Pedestal: https://pedestal.io
   - shadow-cljs: https://shadow-cljs.github.io/docs/UsersGuide.html
   - Re-frame: https://day8.github.io/re-frame/
4. Ask in Clojure community:
   - Clojurians Slack: https://clojurians.slack.com
   - r/Clojure: https://reddit.com/r/Clojure

---

## Appendix A: Task Dependency Graph

```
Task 2.1 (Install Tools) ─┬─→ Task 2.2 (Last.fm API)
                          ├─→ Task 2.3 (Google Cloud)
                          └─→ Task 2.4 (Git Repo) ─→ Task 2.5 (deps.edn) ─→ Task 2.9 (Backend NS) ─→ Task 2.11 (Env Config)
                                                                          ├─→ Task 2.6 (shadow-cljs) ─→ Task 2.7 (Tailwind) ─→ Task 2.8 (HTML) ─→ Task 2.10 (Frontend NS)
                                                                          └─→ Task 2.12 (Docker) ─→ Task 2.13 (CI) ─→ Task 2.14 (Deploy)

All tasks ─→ Task 2.15 (README)
```

## Appendix B: Estimated Timeline

| Task | Estimated Time | Cumulative Time |
|------|---------------|-----------------|
| 2.1  | 30 min        | 30 min          |
| 2.2  | 15 min        | 45 min          |
| 2.3  | 20 min        | 1h 5min         |
| 2.4  | 10 min        | 1h 15min        |
| 2.5  | 30 min        | 1h 45min        |
| 2.6  | 30 min        | 2h 15min        |
| 2.7  | 20 min        | 2h 35min        |
| 2.8  | 15 min        | 2h 50min        |
| 2.9  | 45 min        | 3h 35min        |
| 2.10 | 45 min        | 4h 20min        |
| 2.11 | 10 min        | 4h 30min        |
| 2.12 | 30 min        | 5h              |
| 2.13 | 30 min        | 5h 30min        |
| 2.14 | 20 min        | 5h 50min        |
| 2.15 | 30 min        | 6h 20min        |

**Total Estimated Time: ~6-7 hours**

This can be completed in:
- One focused day
- Two half-days
- Several evening sessions

## Appendix C: Success Criteria Summary

Sprint 2 is complete when you can:

1. ✅ Start backend REPL and run `(start)`
2. ✅ Visit http://localhost:8080 and see the app
3. ✅ Edit ClojureScript code and see hot reload
4. ✅ Run `clj-kondo --lint src` without errors
5. ✅ Build Docker image successfully
6. ✅ Push code and see CI pipeline pass
7. ✅ Have all code committed to git
8. ✅ Have README that explains setup

---

**End of Sprint 2 Implementation Plan**
