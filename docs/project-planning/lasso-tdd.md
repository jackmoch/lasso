# TECHNICAL DESIGN DOCUMENT: LASSO

**Project Name:** Lasso  
**Document Version:** 1.0  
**Document Date:** February 4, 2026  
**Document Owner:** Developer  
**Status:** Draft

---

## 1. Executive Summary

This Technical Design Document (TDD) defines the architecture, technology stack, and implementation details for Lasso, a web application that enables Last.fm users to mirror scrobbles during Spotify Jam sessions.

**Key Technical Decisions:**
- **Frontend:** ClojureScript with Reagent/Re-frame, shadow-cljs for builds
- **Backend:** Clojure with Pedestal framework, Jetty server
- **Build System:** tools.deps with deps.edn
- **Deployment:** Docker containers on Google Cloud Platform
- **CI/CD:** GitHub Actions
- **Architecture:** SPA with RESTful JSON API

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        User Browser                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         ClojureScript SPA (Reagent/Re-frame)        │   │
│  │  - UI Components                                     │   │
│  │  - State Management (Re-frame app-db)               │   │
│  │  - Routing (reitit)                                 │   │
│  │  - HTTP Client (cljs-ajax)                          │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS/REST API
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Clojure Backend (Pedestal/Jetty)               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                 API Routes                           │   │
│  │  - Auth endpoints                                    │   │
│  │  - Session management                                │   │
│  │  - Following control                                 │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Business Logic Layer                    │   │
│  │  - OAuth flow handler                                │   │
│  │  - Scrobble polling engine                          │   │
│  │  - Scrobble mirror service                          │   │
│  │  - Session state manager                            │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Integration Layer                          │   │
│  │  - Last.fm API client (clj-http)                    │   │
│  │  - API signature generator                          │   │
│  │  - Rate limiter                                      │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Data Layer                              │   │
│  │  - Session store (in-memory atom)                   │   │
│  │  - Scrobble cache (in-memory)                       │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS/OAuth/API
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Last.fm API                            │
│  - OAuth authentication                                      │
│  - User data retrieval (user.getRecentTracks)              │
│  - Scrobble submission (track.scrobble)                    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Architecture Pattern

**Pattern:** Single Page Application (SPA) with RESTful API Backend

**Frontend (SPA):**
- Rendered entirely in browser
- ClojureScript compiled to JavaScript
- State managed client-side with Re-frame
- Communicates with backend via REST API

**Backend (API Server):**
- Stateless HTTP API endpoints
- Session state maintained server-side
- Handles all Last.fm API integration
- Serves compiled frontend assets

**Benefits:**
- Clear separation of concerns
- Independent frontend/backend development
- Easy to test and maintain
- Scalable architecture

### 2.3 Communication Flow

**Initial Load:**
1. User navigates to application URL
2. Server serves static HTML + compiled ClojureScript
3. ClojureScript initializes Re-frame app-db
4. UI renders based on initial state

**Authentication Flow:**
1. User clicks "Login with Last.fm"
2. Frontend → Backend: `POST /api/auth/init`
3. Backend generates Last.fm OAuth URL
4. Frontend redirects to Last.fm
5. User authorizes, Last.fm redirects to callback
6. Backend: `GET /api/auth/callback?token=xxx`
7. Backend exchanges token for session key
8. Backend creates session, returns session cookie
9. Frontend updates state to authenticated

**Following Flow:**
1. Frontend → Backend: `POST /api/session/start` (target username)
2. Backend validates target user, starts polling
3. Backend polls Last.fm every 15-30 seconds
4. Backend identifies new scrobbles, submits to user's account
5. Frontend polls: `GET /api/session/status` for updates
6. Backend returns current state + recent scrobbles
7. Frontend updates UI with latest activity

---

## 3. Technology Stack

### 3.1 Frontend Stack

| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| **Language** | ClojureScript | Latest stable | Functional, immutable, excellent REPL experience |
| **UI Framework** | Reagent | ~1.2.0 | Most popular CLJS React wrapper, excellent docs, active community |
| **State Management** | Re-frame | ~1.4.0 | Battle-tested, great patterns for SPA state, comprehensive docs |
| **Routing** | reitit | ~0.7.0 | Modern, data-driven routing, works well with Re-frame |
| **HTTP Client** | cljs-ajax | ~0.8.4 | Simple, well-documented, good Re-frame integration |
| **Build Tool** | shadow-cljs | ~2.27.0 | Best-in-class hot reloading, npm integration, great DX |
| **CSS Framework** | Tailwind CSS | ~3.4.0 | Utility-first, mobile-responsive, excellent documentation |
| **Schema Validation** | Malli | ~0.16.0 | Fast, data-driven, great error messages |

### 3.2 Backend Stack

| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| **Language** | Clojure | 1.11+ | Functional, excellent Java interop, stable |
| **Web Framework** | Pedestal | ~0.7.0 | Async-capable, interceptor-based, production-ready |
| **HTTP Server** | Jetty | ~11.x | Reliable, well-tested, good Pedestal integration |
| **HTTP Client** | clj-http | ~3.13.0 | Industry standard, comprehensive features |
| **Session Store** | In-memory atom | Built-in | Simple, sufficient for MVP, no external deps |
| **Schema Validation** | Malli | ~0.16.0 | Consistent with frontend, excellent validation |
| **Logging** | timbre | ~6.5.0 | Flexible, powerful, Clojure-idiomatic |
| **Configuration** | Environment vars | Built-in | Simple, cloud-native, no library needed |
| **Testing** | clojure.test + test.check | Built-in | Standard, generative testing support |

### 3.3 Build & Development

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **Dependency Management** | tools.deps (deps.edn) | Official tool, flexible, git deps support |
| **Task Runner** | Babashka tasks | Fast, scriptable, Clojure-based |
| **REPL** | nREPL + Calva | VS Code integration, excellent DX |
| **Test Runner** | Kaocha | Modern, fast, great tools.deps integration |
| **Linting** | clj-kondo | Fast, accurate, editor integration |
| **Formatting** | cljfmt | Standard Clojure formatting |

### 3.4 Infrastructure & Deployment

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **Container** | Docker | Standard, portable, cloud-native |
| **Container Registry** | Google Container Registry | Native GCP integration |
| **Hosting** | Google Cloud Run | Serverless, auto-scaling, cost-effective |
| **CI/CD** | GitHub Actions | Free, powerful, good ecosystem |
| **Monitoring** | Google Cloud Logging | Native integration, simple setup |
| **SSL/TLS** | Cloud Run managed | Automatic, no configuration needed |

---

## 4. Project Structure

### 4.1 Monorepo Layout

```
lasso/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI pipeline
│       └── deploy.yml          # Deployment pipeline
├── src/
│   ├── clj/
│   │   └── lasso/
│   │       ├── server.clj      # Server entry point
│   │       ├── config.clj      # Configuration
│   │       ├── routes.clj      # API routes
│   │       ├── middleware.clj  # Custom middleware
│   │       ├── auth/
│   │       │   ├── core.clj    # OAuth implementation
│   │       │   └── session.clj # Session management
│   │       ├── lastfm/
│   │       │   ├── client.clj  # Last.fm API client
│   │       │   ├── oauth.clj   # OAuth specific
│   │       │   └── scrobble.clj # Scrobble operations
│   │       ├── session/
│   │       │   ├── store.clj   # Session storage
│   │       │   └── manager.clj # Session lifecycle
│   │       ├── polling/
│   │       │   ├── engine.clj  # Polling orchestration
│   │       │   └── scheduler.clj # Scheduling logic
│   │       ├── validation/
│   │       │   └── schemas.clj # Malli schemas
│   │       └── util/
│   │           ├── crypto.clj  # Encryption utilities
│   │           └── http.clj    # HTTP utilities
│   └── cljs/
│       └── lasso/
│           ├── core.cljs       # App entry point
│           ├── events.cljs     # Re-frame events
│           ├── subs.cljs       # Re-frame subscriptions
│           ├── views.cljs      # Main views
│           ├── routes.cljs     # Frontend routing
│           ├── api.cljs        # API client
│           ├── components/
│           │   ├── auth.cljs   # Auth components
│           │   ├── session.cljs # Session controls
│           │   ├── activity.cljs # Activity feed
│           │   └── common.cljs  # Shared components
│           ├── validation/
│           │   └── schemas.cljs # Malli schemas (CLJC)
│           └── util/
│               └── format.cljs  # Formatting utilities
├── test/
│   ├── clj/
│   │   └── lasso/
│   │       ├── auth/
│   │       │   └── core_test.clj
│   │       ├── lastfm/
│   │       │   ├── client_test.clj
│   │       │   └── scrobble_test.clj
│   │       └── session/
│   │           └── manager_test.clj
│   └── cljs/
│       └── lasso/
│           └── core_test.cljs
├── resources/
│   └── public/
│       ├── index.html          # HTML template
│       ├── css/
│       │   └── tailwind.css    # Tailwind styles
│       └── images/
│           └── logo.png
├── dev/
│   └── user.clj                # REPL utilities
├── deps.edn                     # Dependencies
├── shadow-cljs.edn             # ClojureScript build config
├── tailwind.config.js          # Tailwind configuration
├── Dockerfile                  # Container definition
├── .dockerignore
├── .gitignore
└── README.md
```

### 4.2 Namespace Organization

**Backend Namespaces:**
- `lasso.server` - Entry point, server initialization
- `lasso.config` - Configuration loading
- `lasso.routes` - Pedestal routes and handlers
- `lasso.middleware` - Custom interceptors/middleware
- `lasso.auth.*` - Authentication and authorization
- `lasso.lastfm.*` - Last.fm API integration
- `lasso.session.*` - Session management
- `lasso.polling.*` - Scrobble polling engine
- `lasso.validation.*` - Schema validation
- `lasso.util.*` - Utility functions

**Frontend Namespaces:**
- `lasso.core` - App initialization
- `lasso.events` - Re-frame event handlers
- `lasso.subs` - Re-frame subscriptions
- `lasso.views` - Top-level views
- `lasso.routes` - Client-side routing
- `lasso.api` - Backend API client
- `lasso.components.*` - UI components
- `lasso.validation.*` - Schema validation (CLJC)
- `lasso.util.*` - Utility functions

---

## 5. Detailed Component Design

### 5.1 Backend Components

#### 5.1.1 Server Entry Point

**File:** `src/clj/lasso/server.clj`

```clojure
(ns lasso.server
  (:require [io.pedestal.http :as http]
            [lasso.config :as config]
            [lasso.routes :as routes]
            [lasso.polling.engine :as polling]))

(defonce server (atom nil))

(defn create-server []
  (http/create-server
    {::http/routes routes/routes
     ::http/type :jetty
     ::http/host (config/get :host "0.0.0.0")
     ::http/port (config/get :port 8080)
     ::http/resource-path "public"
     ::http/secure-headers {:content-security-policy-settings
                           {:default-src "'self'"
                            :script-src "'self' 'unsafe-inline'"
                            :style-src "'self' 'unsafe-inline'"}}}))

(defn start []
  (reset! server (http/start (create-server)))
  (polling/start-scheduler!)
  (println "Server started on port" (config/get :port 8080)))

(defn stop []
  (polling/stop-scheduler!)
  (http/stop @server)
  (println "Server stopped"))

(defn -main [& args]
  (start))
```

#### 5.1.2 Configuration Management

**File:** `src/clj/lasso/config.clj`

```clojure
(ns lasso.config
  (:require [clojure.string :as str]))

(def config
  (atom {:lastfm-api-key (System/getenv "LASTFM_API_KEY")
         :lastfm-api-secret (System/getenv "LASTFM_API_SECRET")
         :oauth-callback-url (System/getenv "OAUTH_CALLBACK_URL")
         :session-secret (System/getenv "SESSION_SECRET")
         :port (or (some-> (System/getenv "PORT") Integer/parseInt) 8080)
         :host (or (System/getenv "HOST") "0.0.0.0")
         :environment (keyword (or (System/getenv "ENVIRONMENT") "development"))
         :polling-interval-ms (or (some-> (System/getenv "POLLING_INTERVAL_MS") 
                                          Integer/parseInt) 
                                  20000)}))

(defn get 
  ([k] (clojure.core/get @config k))
  ([k default] (clojure.core/get @config k default)))

(defn valid? []
  (and (get :lastfm-api-key)
       (get :lastfm-api-secret)
       (get :oauth-callback-url)
       (get :session-secret)))
```

#### 5.1.3 API Routes

**File:** `src/clj/lasso/routes.clj`

```clojure
(ns lasso.routes
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [lasso.auth.core :as auth]
            [lasso.session.manager :as session-manager]
            [lasso.middleware :as middleware]))

(defn home-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "resources/public/index.html")})

(defn auth-init-handler [request]
  (let [auth-url (auth/generate-auth-url)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body {:auth-url auth-url}}))

(defn auth-callback-handler [request]
  (let [token (get-in request [:params :token])
        session-result (auth/complete-auth token)]
    (if (:success session-result)
      {:status 200
       :headers {"Content-Type" "application/json"
                 "Set-Cookie" (str "session-id=" (:session-id session-result)
                                   "; HttpOnly; Secure; SameSite=Lax")}
       :body {:username (:username session-result)}}
      {:status 401
       :headers {"Content-Type" "application/json"}
       :body {:error "Authentication failed"}})))

(defn session-start-handler [request]
  (let [session-id (middleware/get-session-id request)
        target-username (get-in request [:json-params :target-username])
        result (session-manager/start-following session-id target-username)]
    {:status (if (:success result) 200 400)
     :headers {"Content-Type" "application/json"}
     :body result}))

(defn session-pause-handler [request]
  (let [session-id (middleware/get-session-id request)
        result (session-manager/pause-following session-id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body result}))

(defn session-resume-handler [request]
  (let [session-id (middleware/get-session-id request)
        result (session-manager/resume-following session-id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body result}))

(defn session-stop-handler [request]
  (let [session-id (middleware/get-session-id request)
        result (session-manager/stop-following session-id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body result}))

(defn session-status-handler [request]
  (let [session-id (middleware/get-session-id request)
        status (session-manager/get-session-status session-id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body status}))

(defn logout-handler [request]
  (let [session-id (middleware/get-session-id request)]
    (session-manager/destroy-session session-id)
    {:status 200
     :headers {"Content-Type" "application/json"
               "Set-Cookie" "session-id=; Max-Age=0"}
     :body {:success true}}))

(def common-interceptors 
  [(body-params/body-params)
   http/json-body])

(def auth-interceptors
  (conj common-interceptors middleware/require-auth))

(def routes
  (route/expand-routes
    #{["/" :get home-handler :route-name :home]
      ["/api/auth/init" :post (conj common-interceptors auth-init-handler) :route-name :auth-init]
      ["/api/auth/callback" :get auth-callback-handler :route-name :auth-callback]
      ["/api/auth/logout" :post (conj auth-interceptors logout-handler) :route-name :logout]
      ["/api/session/start" :post (conj auth-interceptors session-start-handler) :route-name :session-start]
      ["/api/session/pause" :post (conj auth-interceptors session-pause-handler) :route-name :session-pause]
      ["/api/session/resume" :post (conj auth-interceptors session-resume-handler) :route-name :session-resume]
      ["/api/session/stop" :post (conj auth-interceptors session-stop-handler) :route-name :session-stop]
      ["/api/session/status" :get (conj auth-interceptors session-status-handler) :route-name :session-status]}))
```

#### 5.1.4 OAuth Implementation

**File:** `src/clj/lasso/auth/core.clj`

```clojure
(ns lasso.auth.core
  (:require [clj-http.client :as http]
            [lasso.config :as config]
            [lasso.lastfm.oauth :as oauth]
            [lasso.auth.session :as session]))

(defn generate-auth-url []
  (let [token (oauth/get-token)]
    (str "https://www.last.fm/api/auth/?api_key=" 
         (config/get :lastfm-api-key)
         "&token=" token)))

(defn complete-auth [token]
  (try
    (let [session-key (oauth/get-session-key token)
          username (oauth/get-username session-key)
          session-id (session/create-session username session-key)]
      {:success true
       :session-id session-id
       :username username})
    (catch Exception e
      {:success false
       :error (.getMessage e)})))
```

#### 5.1.5 Last.fm API Client

**File:** `src/clj/lasso/lastfm/client.clj`

```clojure
(ns lasso.lastfm.client
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [lasso.config :as config]
            [lasso.util.crypto :as crypto]))

(def api-base "https://ws.audioscrobbler.com/2.0/")

(defn generate-api-signature [params]
  (let [api-secret (config/get :lastfm-api-secret)
        sorted-params (sort-by first params)
        param-string (str/join "" (mapcat (fn [[k v]] [(name k) v]) sorted-params))
        signature-input (str param-string api-secret)]
    (crypto/md5 signature-input)))

(defn api-request 
  ([method params] (api-request method params false))
  ([method params authenticated?]
   (let [base-params {:method method
                      :api_key (config/get :lastfm-api-key)
                      :format "json"}
         all-params (merge base-params params)
         signed-params (if authenticated?
                        (assoc all-params :api_sig (generate-api-signature all-params))
                        all-params)
         response (http/post api-base
                            {:form-params signed-params
                             :as :json
                             :throw-exceptions false})]
     (if (= 200 (:status response))
       {:success true :data (:body response)}
       {:success false :error (:body response)}))))

(defn get-recent-tracks 
  ([username] (get-recent-tracks username nil))
  ([username from-timestamp]
   (let [params (cond-> {:user username :limit 50}
                  from-timestamp (assoc :from from-timestamp))]
     (api-request "user.getRecentTracks" params false))))

(defn get-user-info [username]
  (api-request "user.getInfo" {:user username} false))

(defn scrobble-track [session-key artist track timestamp & {:keys [album]}]
  (let [params (cond-> {:artist artist
                        :track track
                        :timestamp timestamp
                        :sk session-key}
                 album (assoc :album album))]
    (api-request "track.scrobble" params true)))
```

#### 5.1.6 Session Store

**File:** `src/clj/lasso/session/store.clj`

```clojure
(ns lasso.session.store
  (:require [clojure.core.async :as async]))

;; Session structure:
;; {:session-id "uuid"
;;  :username "lastfm-username"
;;  :session-key "lastfm-session-key"
;;  :created-at timestamp
;;  :last-activity timestamp
;;  :following-session {:target-username "target"
;;                      :state :active|:paused|:stopped
;;                      :started-at timestamp
;;                      :last-poll timestamp
;;                      :scrobble-count 0
;;                      :scrobble-cache #{}}}

(defonce sessions (atom {}))

(defn create-session [session-id username session-key]
  (let [now (System/currentTimeMillis)
        session {:session-id session-id
                 :username username
                 :session-key session-key
                 :created-at now
                 :last-activity now
                 :following-session nil}]
    (swap! sessions assoc session-id session)
    session))

(defn get-session [session-id]
  (get @sessions session-id))

(defn update-session [session-id update-fn]
  (swap! sessions update session-id update-fn))

(defn delete-session [session-id]
  (swap! sessions dissoc session-id))

(defn get-active-following-sessions []
  (filter (fn [[_ session]]
            (= :active (get-in session [:following-session :state])))
          @sessions))

(defn touch-session [session-id]
  (update-session session-id 
                  #(assoc % :last-activity (System/currentTimeMillis))))
```

#### 5.1.7 Polling Engine

**File:** `src/clj/lasso/polling/engine.clj`

```clojure
(ns lasso.polling.engine
  (:require [clojure.core.async :as async]
            [lasso.session.store :as store]
            [lasso.lastfm.client :as lastfm]
            [lasso.config :as config]
            [taoensso.timbre :as log]))

(defonce scheduler-control (atom nil))

(defn process-new-scrobbles [session-id target-username session-key scrobble-cache]
  (let [last-poll (get-in (store/get-session session-id) 
                          [:following-session :last-poll] 
                          0)
        result (lastfm/get-recent-tracks target-username 
                                        (when (> last-poll 0) 
                                          (quot last-poll 1000)))]
    (if (:success result)
      (let [tracks (get-in result [:data :recenttracks :track])
            new-tracks (filter 
                        (fn [track]
                          (let [track-id (str (:artist track) "|" 
                                            (:name track) "|" 
                                            (:date track))]
                            (not (contains? scrobble-cache track-id))))
                        tracks)]
        (doseq [track new-tracks]
          (let [scrobble-result (lastfm/scrobble-track 
                                  session-key
                                  (get-in track [:artist :text])
                                  (:name track)
                                  (get-in track [:date :uts])
                                  :album (get-in track [:album :text]))]
            (when (:success scrobble-result)
              (let [track-id (str (get-in track [:artist :text]) "|" 
                                (:name track) "|" 
                                (get-in track [:date :uts]))]
                (store/update-session 
                  session-id
                  #(-> %
                       (update-in [:following-session :scrobble-cache] 
                                 (fnil conj #{}) track-id)
                       (update-in [:following-session :scrobble-count] 
                                 (fnil inc 0))
                       (assoc-in [:following-session :last-poll] 
                                (System/currentTimeMillis))))))))
        {:success true :processed (count new-tracks)})
      {:success false :error (:error result)})))

(defn poll-active-sessions []
  (doseq [[session-id session] (store/get-active-following-sessions)]
    (try
      (let [target-username (get-in session [:following-session :target-username])
            session-key (:session-key session)
            scrobble-cache (get-in session [:following-session :scrobble-cache] #{})]
        (process-new-scrobbles session-id target-username session-key scrobble-cache))
      (catch Exception e
        (log/error e "Error polling session" session-id)))))

(defn start-scheduler! []
  (when-not @scheduler-control
    (let [control-chan (async/chan)
          interval-ms (config/get :polling-interval-ms 20000)]
      (reset! scheduler-control control-chan)
      (async/go-loop []
        (let [[v _] (async/alts! [control-chan (async/timeout interval-ms)])]
          (when-not (= v :stop)
            (poll-active-sessions)
            (recur))))
      (log/info "Polling scheduler started with interval" interval-ms "ms"))))

(defn stop-scheduler! []
  (when-let [control-chan @scheduler-control]
    (async/>!! control-chan :stop)
    (async/close! control-chan)
    (reset! scheduler-control nil)
    (log/info "Polling scheduler stopped")))
```

### 5.2 Frontend Components

#### 5.2.1 Core Entry Point

**File:** `src/cljs/lasso/core.cljs`

```clojure
(ns lasso.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [lasso.events]
            [lasso.subs]
            [lasso.views :as views]
            [lasso.routes :as routes]))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (routes/init-routes!)
  (rdom/render [views/main-panel]
               (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))
```

#### 5.2.2 Re-frame Events

**File:** `src/cljs/lasso/events.cljs`

```clojure
(ns lasso.events
  (:require [re-frame.core :as rf]
            [lasso.api :as api]
            [day8.re-frame.http-fx]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   {:auth {:authenticated? false
           :username nil}
    :session {:state :not-started
              :target-username nil
              :scrobble-count 0
              :recent-scrobbles []
              :last-poll nil}
    :ui {:loading? false
         :error nil}}))

(rf/reg-event-fx
 :auth/initiate-login
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:ui :loading?] true)
    :http-xhrio {:method :post
                 :uri "/api/auth/init"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/login-url-received]
                 :on-failure [:api/request-failed]}}))

(rf/reg-event-fx
 :auth/login-url-received
 (fn [{:keys [db]} [_ response]]
   {:db (assoc-in db [:ui :loading?] false)
    :redirect-external (:auth-url response)}))

(rf/reg-event-fx
 :session/start-following
 (fn [{:keys [db]} [_ target-username]]
   {:db (assoc-in db [:ui :loading?] true)
    :http-xhrio {:method :post
                 :uri "/api/session/start"
                 :params {:target-username target-username}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:session/started]
                 :on-failure [:api/request-failed]}}))

(rf/reg-event-db
 :session/started
 (fn [db [_ response]]
   (-> db
       (assoc-in [:ui :loading?] false)
       (assoc-in [:session :state] :active)
       (assoc-in [:session :target-username] (:target-username response)))))

;; ... additional event handlers for pause, resume, stop, etc.
```

#### 5.2.3 Re-frame Subscriptions

**File:** `src/cljs/lasso/subs.cljs`

```clojure
(ns lasso.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :auth/authenticated?
 (fn [db _]
   (get-in db [:auth :authenticated?])))

(rf/reg-sub
 :auth/username
 (fn [db _]
   (get-in db [:auth :username])))

(rf/reg-sub
 :session/state
 (fn [db _]
   (get-in db [:session :state])))

(rf/reg-sub
 :session/target-username
 (fn [db _]
   (get-in db [:session :target-username])))

(rf/reg-sub
 :session/recent-scrobbles
 (fn [db _]
   (get-in db [:session :recent-scrobbles])))

(rf/reg-sub
 :ui/loading?
 (fn [db _]
   (get-in db [:ui :loading?])))

(rf/reg-sub
 :ui/error
 (fn [db _]
   (get-in db [:ui :error])))
```

#### 5.2.4 Main Views

**File:** `src/cljs/lasso/views.cljs`

```clojure
(ns lasso.views
  (:require [re-frame.core :as rf]
            [lasso.components.auth :as auth]
            [lasso.components.session :as session]
            [lasso.components.activity :as activity]))

(defn main-panel []
  (let [authenticated? @(rf/subscribe [:auth/authenticated?])]
    [:div.min-h-screen.bg-gray-100
     [:div.container.mx-auto.px-4.py-8
      [:header.text-center.mb-8
       [:h1.text-4xl.font-bold.text-gray-900 "Lasso"]
       [:p.text-gray-600 "Track your Spotify Jam scrobbles"]]
      
      (if authenticated?
        [:div
         [session/session-controls]
         [activity/activity-feed]]
        [auth/login-view])]]))
```

---

## 6. Data Models & Schemas

### 6.1 Malli Schemas

**File:** `src/clj/lasso/validation/schemas.clj` (CLJC for sharing)

```clojure
(ns lasso.validation.schemas
  (:require [malli.core :as m]
            [malli.error :as me]))

;; Session schemas
(def SessionState
  [:enum :not-started :active :paused :stopped])

(def FollowingSession
  [:map
   [:target-username :string]
   [:state SessionState]
   [:started-at :int]
   [:last-poll {:optional true} :int]
   [:scrobble-count :int]
   [:scrobble-cache :set]])

(def UserSession
  [:map
   [:session-id :string]
   [:username :string]
   [:session-key :string]
   [:created-at :int]
   [:last-activity :int]
   [:following-session {:optional true} [:maybe FollowingSession]]])

;; API request/response schemas
(def StartSessionRequest
  [:map
   [:target-username [:string {:min 1 :max 100}]]])

(def SessionStatusResponse
  [:map
   [:state SessionState]
   [:target-username {:optional true} :string]
   [:scrobble-count :int]
   [:recent-scrobbles :vector]
   [:last-poll {:optional true} :int]])

;; Last.fm API schemas
(def Track
  [:map
   [:artist :string]
   [:name :string]
   [:album {:optional true} :string]
   [:timestamp :int]])

;; Validation helpers
(defn validate [schema data]
  (if (m/validate schema data)
    {:valid? true :data data}
    {:valid? false 
     :errors (me/humanize (m/explain schema data))}))
```

### 6.2 Session Data Structure

```clojure
{:session-id "550e8400-e29b-41d4-a716-446655440000"
 :username "john_doe"
 :session-key "encrypted-session-key"
 :created-at 1706832000000
 :last-activity 1706832300000
 :following-session {:target-username "jane_smith"
                     :state :active
                     :started-at 1706832100000
                     :last-poll 1706832280000
                     :scrobble-count 15
                     :scrobble-cache #{"Radiohead|Creep|1706832100"
                                      "The Beatles|Yesterday|1706832200"}}}
```

### 6.3 API Request/Response Formats

**Authentication Init Response:**
```json
{
  "auth_url": "https://www.last.fm/api/auth/?api_key=xxx&token=yyy"
}
```

**Start Session Request:**
```json
{
  "target_username": "jane_smith"
}
```

**Session Status Response:**
```json
{
  "state": "active",
  "target_username": "jane_smith",
  "scrobble_count": 15,
  "recent_scrobbles": [
    {
      "artist": "Radiohead",
      "track": "Creep",
      "album": "Pablo Honey",
      "timestamp": 1706832100
    }
  ],
  "last_poll": 1706832280000
}
```

---

## 7. Security Design

### 7.1 Authentication & Authorization

**OAuth Flow:**
1. User initiates login → Backend generates Last.fm OAuth URL
2. User redirects to Last.fm, approves application
3. Last.fm redirects to callback with token
4. Backend exchanges token for session key
5. Backend creates server-side session, returns HTTP-only cookie

**Session Management:**
- Session ID stored in HTTP-only, Secure, SameSite=Lax cookie
- Session data (including Last.fm credentials) stored server-side only
- Session timeout: 24 hours of inactivity
- No client-side storage of credentials

**Authorization:**
- All API endpoints (except auth) require valid session cookie
- Session validation interceptor checks session existence and validity
- Invalid/expired sessions return 401 Unauthorized

### 7.2 Data Protection

**Sensitive Data:**
- Last.fm API keys: Environment variables, never in code
- OAuth tokens: Encrypted at rest using Buddy library
- Session keys: Server-side only, never sent to client
- User passwords: Never stored (OAuth only)

**Encryption:**
```clojure
(ns lasso.util.crypto
  (:require [buddy.core.crypto :as crypto]
            [buddy.core.codecs :as codecs]
            [lasso.config :as config]))

(defn encrypt [data]
  (let [secret (config/get :session-secret)
        iv (crypto/generate-iv 16)]
    (crypto/encrypt data secret {:iv iv :algorithm :aes256-cbc-hmac-sha512})))

(defn decrypt [encrypted-data]
  (let [secret (config/get :session-secret)]
    (crypto/decrypt encrypted-data secret {:algorithm :aes256-cbc-hmac-sha512})))
```

### 7.3 API Security

**Rate Limiting:**
- Respect Last.fm's 5 requests/second limit
- Implement request queue with throttling
- Backoff strategy on rate limit errors

**Input Validation:**
- All inputs validated with Malli schemas
- Sanitize user input before Last.fm API calls
- Validate target username exists before starting session

**HTTPS:**
- All communication over HTTPS (enforced by Cloud Run)
- Secure cookies with Secure flag
- HSTS headers enabled

### 7.4 CORS Policy

**Policy:**
- Development: Allow localhost origins
- Production: Same-origin only (SPA served from same domain)
- No cross-origin API access

---

## 8. API Design

### 8.1 REST API Endpoints

#### Authentication Endpoints

**POST /api/auth/init**
- Description: Initialize OAuth flow
- Auth Required: No
- Request Body: None
- Response: `{ "auth_url": "https://last.fm/..." }`
- Status Codes: 200 OK, 500 Internal Server Error

**GET /api/auth/callback**
- Description: OAuth callback handler
- Auth Required: No
- Query Params: `token` (from Last.fm)
- Response: `{ "username": "john_doe" }` + Set-Cookie header
- Status Codes: 200 OK, 401 Unauthorized

**POST /api/auth/logout**
- Description: Destroy user session
- Auth Required: Yes
- Request Body: None
- Response: `{ "success": true }`
- Status Codes: 200 OK

#### Session Management Endpoints

**POST /api/session/start**
- Description: Start following target user
- Auth Required: Yes
- Request Body: `{ "target_username": "jane_smith" }`
- Response: `{ "success": true, "target_username": "jane_smith" }`
- Status Codes: 200 OK, 400 Bad Request, 401 Unauthorized

**POST /api/session/pause**
- Description: Pause active following session
- Auth Required: Yes
- Request Body: None
- Response: `{ "success": true, "state": "paused" }`
- Status Codes: 200 OK, 400 Bad Request, 401 Unauthorized

**POST /api/session/resume**
- Description: Resume paused following session
- Auth Required: Yes
- Request Body: None
- Response: `{ "success": true, "state": "active" }`
- Status Codes: 200 OK, 400 Bad Request, 401 Unauthorized

**POST /api/session/stop**
- Description: Stop and clear following session
- Auth Required: Yes
- Request Body: None
- Response: `{ "success": true, "state": "stopped" }`
- Status Codes: 200 OK, 401 Unauthorized

**GET /api/session/status**
- Description: Get current session status and recent activity
- Auth Required: Yes
- Request Body: None
- Response: SessionStatusResponse (see schemas)
- Status Codes: 200 OK, 401 Unauthorized

### 8.2 Error Response Format

**Standard Error Response:**
```json
{
  "error": "Error message",
  "details": "Additional context (optional)",
  "code": "ERROR_CODE"
}
```

**Common Error Codes:**
- `AUTH_REQUIRED` - Authentication required
- `SESSION_EXPIRED` - Session has expired
- `INVALID_REQUEST` - Request validation failed
- `TARGET_NOT_FOUND` - Target username not found
- `TARGET_PRIVATE` - Target profile is private
- `RATE_LIMIT` - Rate limit exceeded
- `LASTFM_ERROR` - Last.fm API error

---

## 9. Build & Development

### 9.1 Development Setup

**Prerequisites:**
- JDK 11 or higher
- Node.js 18+ (for npm and Tailwind)
- Clojure CLI tools
- VS Code with Calva extension

**Initial Setup:**
```bash
# Clone repository
git clone https://github.com/yourusername/lasso.git
cd lasso

# Install dependencies
clojure -P  # Downloads Clojure dependencies
npm install  # Downloads npm dependencies (Tailwind, shadow-cljs)

# Set up environment variables
cp .env.example .env
# Edit .env with your Last.fm API keys

# Build CSS
npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css

# Start development servers
# Terminal 1: Backend REPL
clj -M:dev:repl

# Terminal 2: Frontend (shadow-cljs)
npx shadow-cljs watch app
```

**REPL Workflow:**
1. Start backend REPL with Calva: "Jack-in to deps.edn"
2. In REPL: `(user/start)` to start server
3. Start shadow-cljs watch in separate terminal
4. Open browser to http://localhost:8080
5. Edit code, save, see changes instantly
6. Use REPL to test functions interactively

### 9.2 deps.edn Configuration

```clojure
{:paths ["src/clj" "resources"]
 
 :deps {org.clojure/clojure {:mvn/version "1.11.1"}
        
        ;; Backend
        io.pedestal/pedestal.service {:mvn/version "0.7.0"}
        io.pedestal/pedestal.jetty {:mvn/version "0.7.0"}
        clj-http/clj-http {:mvn/version "3.13.0"}
        com.taoensso/timbre {:mvn/version "6.5.0"}
        metosin/malli {:mvn/version "0.16.0"}
        buddy/buddy-core {:mvn/version "1.11.1"}
        
        ;; Utilities
        org.clojure/core.async {:mvn/version "1.6.681"}}
 
 :aliases
 {:dev {:extra-paths ["dev" "test/clj"]
        :extra-deps {org.clojure/tools.namespace {:mvn/version "1.4.4"}
                     lambdaisland/kaocha {:mvn/version "1.87.1366"}}}
  
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

### 9.3 shadow-cljs.edn Configuration

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

### 9.4 Build Commands

**Development:**
```bash
# Start backend with REPL
clj -M:dev:repl

# Start frontend with hot reload
npx shadow-cljs watch app

# Build CSS
npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css --watch
```

**Testing:**
```bash
# Run backend tests
clj -M:test

# Run specific test
clj -M:test --focus lasso.auth.core-test

# Run frontend tests
npx shadow-cljs compile test
node target/test.js
```

**Production Build:**
```bash
# Build frontend
npx shadow-cljs release app

# Build CSS (minified)
npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css --minify

# Build backend uberjar
clj -X:uberjar
```

---

## 10. Testing Strategy

### 10.1 Unit Testing

**Backend Tests:**
- Test all core business logic functions
- Mock external API calls (Last.fm)
- Use clojure.test for assertions
- Use test.check for property-based testing where applicable

**Example Test:**
```clojure
(ns lasso.lastfm.client-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [lasso.lastfm.client :as client]))

(deftest generate-api-signature-test
  (testing "API signature generation"
    (with-redefs [config/get (constantly "test-secret")]
      (is (= "expected-md5-hash"
             (client/generate-api-signature {:method "test" :api_key "key"}))))))

(deftest api-signature-properties
  (testing "Signature is consistent for same input"
    (tc/quick-check
      100
      (prop/for-all [params (gen/map gen/keyword gen/string-alphanumeric)]
        (let [sig1 (client/generate-api-signature params)
              sig2 (client/generate-api-signature params)]
          (= sig1 sig2))))))
```

**Frontend Tests:**
- Test Re-frame event handlers
- Test subscription functions
- Test utility functions
- Mock HTTP requests

### 10.2 Integration Testing

**Last.fm API Integration:**
- Test OAuth flow end-to-end
- Test API request/response handling
- Test rate limiting behavior
- Test error handling for various API responses

**Session Management:**
- Test session creation and lifecycle
- Test concurrent session handling
- Test session expiration

### 10.3 End-to-End Testing

**Manual Test Scenarios:**
1. Complete authentication flow
2. Start → Pause → Resume → Stop session flow
3. Error recovery (network failures, API errors)
4. Mobile responsiveness
5. Session timeout handling

**Automated E2E (Optional):**
- Cypress or Playwright for critical user flows
- Deferred to post-MVP if time permits

### 10.4 Performance Testing

**Load Testing:**
- Test concurrent user sessions
- Measure API response times
- Test polling performance under load
- Verify rate limit compliance

**Tools:**
- Apache JMeter or Gatling for load testing
- Google Cloud Monitoring for production metrics

---

## 11. Deployment Architecture

### 11.1 Docker Configuration

**Dockerfile:**
```dockerfile
# Multi-stage build

# Stage 1: Build frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY shadow-cljs.edn tailwind.config.js ./
COPY src/cljs ./src/cljs
COPY resources/public ./resources/public
RUN npx shadow-cljs release app
RUN npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/tailwind.css --minify

# Stage 2: Build backend
FROM clojure:temurin-11-tools-deps AS backend-builder
WORKDIR /app
COPY deps.edn ./
RUN clojure -P
COPY src/clj ./src/clj
RUN clojure -X:uberjar

# Stage 3: Runtime
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/lasso.jar ./lasso.jar
COPY --from=frontend-builder /app/resources/public ./resources/public

EXPOSE 8080
CMD ["java", "-jar", "lasso.jar"]
```

**.dockerignore:**
```
.git
.github
node_modules
target
.shadow-cljs
.calva
.clj-kondo
.lsp
*.log
.env
```

### 11.2 Google Cloud Run Deployment

**Service Configuration:**
- Region: Choose based on target audience (e.g., us-central1)
- CPU: 1 vCPU
- Memory: 512 MB (start small, scale if needed)
- Min instances: 0 (scale to zero to save costs)
- Max instances: 10
- Request timeout: 300 seconds
- Concurrency: 80 requests per instance

**Environment Variables:**
- Set via Cloud Run console or gcloud CLI
- Stored in Google Secret Manager for sensitive values

**Deployment Command:**
```bash
# Build and push container
gcloud builds submit --tag gcr.io/PROJECT_ID/lasso

# Deploy to Cloud Run
gcloud run deploy lasso \
  --image gcr.io/PROJECT_ID/lasso \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars "ENVIRONMENT=production" \
  --set-secrets "LASTFM_API_KEY=lastfm-api-key:latest,LASTFM_API_SECRET=lastfm-api-secret:latest"
```

### 11.3 CI/CD Pipeline

**GitHub Actions Workflow (.github/workflows/deploy.yml):**
```yaml
name: Deploy to Google Cloud Run

on:
  push:
    branches: [main]

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
    
    - name: Setup Google Cloud
      uses: google-github-actions/setup-gcloud@v1
      with:
        service_account_key: ${{ secrets.GCP_SA_KEY }}
        project_id: ${{ secrets.GCP_PROJECT_ID }}
    
    - name: Configure Docker
      run: gcloud auth configure-docker
    
    - name: Build container
      run: docker build -t gcr.io/$PROJECT_ID/$SERVICE:$GITHUB_SHA .
    
    - name: Push container
      run: docker push gcr.io/$PROJECT_ID/$SERVICE:$GITHUB_SHA
    
    - name: Deploy to Cloud Run
      run: |
        gcloud run deploy $SERVICE \
          --image gcr.io/$PROJECT_ID/$SERVICE:$GITHUB_SHA \
          --platform managed \
          --region $REGION \
          --allow-unauthenticated
```

**CI Workflow (.github/workflows/ci.yml):**
```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [develop]

jobs:
  test:
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
      uses: DeLaGuardo/setup-clojure@v1
      with:
        cli: 1.11.1.1435
    
    - name: Cache dependencies
      uses: actions/cache@v3
      with:
        path: |
          ~/.m2
          ~/.gitlibs
        key: ${{ runner.os }}-clojure-${{ hashFiles('deps.edn') }}
    
    - name: Run tests
      run: clj -M:test
    
    - name: Lint
      run: clj-kondo --lint src
```

### 11.4 Monitoring & Logging

**Google Cloud Logging:**
- Automatic log collection from Cloud Run
- Structured logging with timbre
- Log levels: ERROR, WARN, INFO, DEBUG
- Custom log queries for troubleshooting

**Google Cloud Monitoring:**
- Request count, latency, error rate (automatic)
- Custom metrics:
  - Active sessions count
  - Scrobbles mirrored per hour
  - Last.fm API request rate
  - Polling cycle duration

**Alerting:**
- Error rate > 5%
- Request latency > 3 seconds
- Last.fm API rate limit hits

**Cost Monitoring:**
- Set budget alerts in Google Cloud
- Monitor Cloud Run request volume
- Track container CPU/memory usage

---

## 12. Performance Considerations

### 12.1 Backend Performance

**Optimization Strategies:**
- Use core.async for concurrent polling
- Implement connection pooling for Last.fm API calls
- Cache Last.fm API responses (cautiously, respect rate limits)
- Lazy loading of session data
- Efficient data structures (persistent data structures)

**Polling Optimization:**
- Adjust polling interval based on Last.fm rate limits
- Use `from` timestamp to only fetch new scrobbles
- Implement exponential backoff on errors
- Batch scrobble submissions where possible

### 12.2 Frontend Performance

**Optimization Strategies:**
- Code splitting (shadow-cljs advanced optimizations)
- Lazy loading of components
- Debounce user input
- Minimize Re-frame subscriptions
- Use React.memo for expensive components

**Bundle Size:**
- Target: <500KB gzipped JavaScript
- Monitor with shadow-cljs build report
- Tree-shake unused Reagent/React features

### 12.3 Scaling Considerations

**Horizontal Scaling:**
- Stateless API design allows multiple instances
- Session state in shared store (future: Redis)
- Cloud Run auto-scaling handles load

**Session Store Scaling:**
- MVP: In-memory atom (single instance)
- Future: Redis or Cloud Memorystore
- Session replication across instances

**Database Considerations:**
- No persistent database for MVP
- Future: Cloud Firestore for session history
- Keep core app database-free for simplicity

---

## 13. Security Considerations

### 13.1 Threat Model

**Identified Threats:**
1. Session hijacking
2. API key exposure
3. CSRF attacks
4. XSS attacks
5. Rate limit abuse
6. Man-in-the-middle attacks

**Mitigations:**
1. HTTP-only, Secure cookies; session timeout
2. Environment variables, Secret Manager, no client exposure
3. SameSite cookie attribute, same-origin policy
4. React escapes by default, validate all inputs
5. Backend rate limiting, polling throttling
6. HTTPS only, HSTS headers

### 13.2 Security Best Practices

**Code Security:**
- Never commit secrets to git
- Use Malli validation on all inputs
- Sanitize user input before external API calls
- Regular dependency updates (nvd-clojure)

**Operational Security:**
- Regular security audits of dependencies
- Monitor for unusual activity
- Log security events
- Rotate API keys periodically

**Compliance:**
- Review Last.fm API Terms of Service
- No storage of user listening history
- Clear privacy policy
- GDPR compliance (minimal data collection)

---

## 14. Maintenance & Operations

### 14.1 Monitoring Checklist

**Daily:**
- Check error logs for issues
- Monitor active session count
- Verify Last.fm API connectivity

**Weekly:**
- Review performance metrics
- Check for dependency updates
- Analyze usage patterns
- Review cost metrics

**Monthly:**
- Update dependencies
- Review and rotate secrets
- Analyze user feedback
- Plan feature iterations

### 14.2 Incident Response

**Severity Levels:**
- P0: App down, authentication broken
- P1: Major feature broken (scrobbling not working)
- P2: Minor feature broken (UI issue)
- P3: Enhancement or minor bug

**Response Times:**
- P0: Immediate (within 1 hour)
- P1: Within 24 hours
- P2: Within 3 days
- P3: Scheduled for next sprint

### 14.3 Backup & Recovery

**Data Backup:**
- No persistent user data to backup
- Code in Git (primary backup)
- Configuration in Secret Manager

**Recovery Procedures:**
1. Rollback to previous Cloud Run revision
2. Redeploy from Git (if needed)
3. Verify environment variables
4. Check Last.fm API connectivity
5. Monitor logs for errors

---

## 15. Future Enhancements

### 15.1 Technical Debt

**Known Limitations:**
- In-memory session store (single instance only)
- Polling-based updates (not real-time)
- Manual deployment process
- Limited test coverage initially

**Technical Debt Paydown:**
- Sprint 10: Increase test coverage to 80%
- Sprint 11: Migrate to Redis session store
- Sprint 12: Implement WebSocket for real-time updates
- Sprint 13: Add comprehensive monitoring

### 15.2 Performance Improvements

**Optimization Opportunities:**
- Implement caching layer for Last.fm API
- Use WebSocket instead of polling
- Optimize Re-frame subscriptions
- Implement service worker for offline support

### 15.3 Feature Extensions

**Planned Features (Post-MVP):**
- Session history persistence
- Multiple concurrent targets
- Analytics dashboard
- Browser extension
- Mobile PWA support

---

## 16. Development Guidelines

### 16.1 Code Style

**Clojure/ClojureScript:**
- Follow Clojure Style Guide
- Use cljfmt for automatic formatting
- Maximum line length: 100 characters
- Prefer pure functions
- Document public functions with docstrings

**Example:**
```clojure
(defn process-scrobble
  "Processes a scrobble from the target user and mirrors it to the authenticated user.
   
   Args:
     session-key: Last.fm session key for authenticated user
     track: Track map containing :artist, :name, :album, :timestamp
   
   Returns:
     Map with :success boolean and optional :error"
  [session-key track]
  ;; Implementation
  )
```

### 16.2 Git Workflow

**Branch Strategy:**
- `main` - Production-ready code
- `develop` - Integration branch
- `feature/xxx` - Feature branches
- `bugfix/xxx` - Bug fix branches

**Commit Messages:**
- Format: `[type]: description`
- Types: feat, fix, docs, style, refactor, test, chore
- Example: `feat: add session pause functionality`

**Pull Requests:**
- Create PR from feature branch to develop
- Require tests for new features
- Code review before merge
- Squash commits on merge

### 16.3 Documentation

**Required Documentation:**
- README.md with setup instructions
- API documentation (generated from code)
- Architecture Decision Records (ADRs)
- Deployment runbook
- Troubleshooting guide

---

## 17. Appendices

### Appendix A: Dependencies List

**Backend Dependencies:**
```clojure
;; Core
org.clojure/clojure "1.11.1"
org.clojure/core.async "1.6.681"

;; Web framework
io.pedestal/pedestal.service "0.7.0"
io.pedestal/pedestal.jetty "0.7.0"

;; HTTP client
clj-http/clj-http "3.13.0"

;; Validation
metosin/malli "0.16.0"

;; Crypto
buddy/buddy-core "1.11.1"

;; Logging
com.taoensso/timbre "6.5.0"

;; Testing
org.clojure/test.check "1.1.1"
lambdaisland/kaocha "1.87.1366"
```

**Frontend Dependencies:**
```clojure
;; UI
reagent "1.2.0"
re-frame "1.4.3"

;; Routing
metosin/reitit-frontend "0.7.0"

;; HTTP
cljs-ajax "0.8.4"
day8.re-frame/http-fx "0.2.4"

;; Validation
metosin/malli "0.16.0"
```

### Appendix B: Environment Variables

```bash
# Required
LASTFM_API_KEY=your_api_key_here
LASTFM_API_SECRET=your_api_secret_here
OAUTH_CALLBACK_URL=https://yourdomain.com/api/auth/callback
SESSION_SECRET=random_secret_for_encryption

# Optional
PORT=8080
HOST=0.0.0.0
ENVIRONMENT=production
POLLING_INTERVAL_MS=20000
```

### Appendix C: Useful Commands

```bash
# Development
clj -M:dev:repl              # Start backend REPL
npx shadow-cljs watch app    # Start frontend with hot reload
npx tailwindcss --watch      # Watch CSS changes

# Testing
clj -M:test                  # Run all tests
clj -M:test --focus ns       # Run specific namespace tests
npx shadow-cljs compile test # Compile ClojureScript tests

# Building
npx shadow-cljs release app  # Production frontend build
clj -X:uberjar              # Build backend uberjar
docker build -t lasso .      # Build Docker image

# Deployment
gcloud builds submit         # Build on Cloud Build
gcloud run deploy           # Deploy to Cloud Run

# Linting & Formatting
clj-kondo --lint src        # Lint Clojure code
cljfmt fix                  # Format Clojure code
```

---

## 18. Approval & Sign-off

**Technical Design Document Approved By:**

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | [Your Name] | [Date] | _________ |
| Product Owner | [Your Name] | [Date] | _________ |

**Document Revision History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | February 4, 2026 | Developer | Initial TDD creation |

---

**End of Technical Design Document**
