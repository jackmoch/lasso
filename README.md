# Lasso

[![CI](https://github.com/jackmoch/lasso/workflows/CI/badge.svg)](https://github.com/jackmoch/lasso/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/jackmoch/lasso/branch/main/graph/badge.svg)](https://codecov.io/gh/jackmoch/lasso)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Live](https://img.shields.io/badge/live-production-brightgreen)](https://lasso-ngqcsb2bpa-uc.a.run.app)

> Scrobble your Spotify Jam listening to Last.fm — automatically.

**[Try it now →](https://lasso-ngqcsb2bpa-uc.a.run.app)**

When you join a Spotify Jam as a guest, your listens don't get scrobbled to Last.fm. Lasso fixes that: enter the host's Last.fm username, and Lasso mirrors their scrobbles to your account in real time.

## How It Works

1. **Login** with your Last.fm account (OAuth — no password stored)
2. **Enter** the Last.fm username of the Spotify Jam host
3. **Start** — Lasso polls their recent tracks every ~20 seconds and scrobbles matches to your account
4. **Pause, resume, or stop** the session at any time

> **Note:** Only tracks scrobbled _after_ you start a session are mirrored. There is no backfill.

## Features

- Real-time scrobble mirroring from any public Last.fm profile
- Session controls: start, pause, resume, stop
- Activity feed showing recent mirrored scrobbles
- Secure OAuth 2.0 login (no passwords stored)
- Respects Last.fm API rate limits

## Technology Stack

**Backend (Clojure)**
- Pedestal web framework with Jetty
- Last.fm API integration with rate limiting
- OAuth 2.0 authentication
- Core.async polling engine

**Frontend (ClojureScript)**
- Reagent (React wrapper) + Re-frame state management
- Tailwind CSS styling

**Infrastructure**
- Docker on Google Cloud Run
- GitHub Actions CI/CD (lint → test → build → deploy)

## Self-Hosting

### Prerequisites

- Java 11+
- Clojure CLI (tools.deps)
- Node.js 18+
- [Babashka](https://github.com/babashka/babashka#installation) (recommended)
- Last.fm API credentials ([register here](https://www.last.fm/api/account/create))

### 1. Clone and configure

```bash
git clone https://github.com/jackmoch/lasso.git
cd lasso
cp .env.example .env
```

Edit `.env`:
```bash
LASTFM_API_KEY=your_api_key_here
LASTFM_API_SECRET=your_api_secret_here
OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback
SESSION_SECRET=$(openssl rand -base64 32)
```

### 2. Install dependencies

```bash
clojure -P          # Clojure deps
npm install         # Node deps
npm run build:css   # Tailwind CSS
```

### 3. Start the development server

```bash
bb dev   # Starts backend + frontend with hot reload
```

Open [http://localhost:8080](http://localhost:8080).

**Alternative (direct REPL):**
```bash
clj -M:dev
user=> (start)
```

### Docker

```bash
docker build -t lasso:latest .
docker run -p 8080:8080 \
  -e LASTFM_API_KEY=your_key \
  -e LASTFM_API_SECRET=your_secret \
  -e SESSION_SECRET=your_session_secret \
  -e OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback \
  lasso:latest
```

## Development

### Common commands

```bash
bb dev           # Start full development environment
bb test          # Run backend tests (90 tests)
bb test:watch    # Tests in watch mode
bb lint          # Lint Clojure/ClojureScript
bb build         # Build production artifacts
bb tasks         # List all Babashka tasks
```

### REPL utilities (in `user` namespace)

```clojure
(start)          ; Start backend + frontend
(stop)           ; Stop everything
(restart)        ; Restart
(reset)          ; Reload namespaces + restart
(cljs-repl)      ; Connect ClojureScript REPL to browser
```

### Project structure

```
src/
├── clj/lasso/           # Backend (Clojure)
│   ├── server.clj       # Server lifecycle
│   ├── routes.clj       # HTTP routes
│   ├── auth/            # OAuth flow
│   ├── lastfm/          # Last.fm API client
│   ├── session/         # Session store & manager
│   └── polling/         # Scrobble polling engine
└── cljs/lasso/          # Frontend (ClojureScript)
    ├── core.cljs        # App entry point
    ├── events.cljs      # Re-frame events
    ├── subs.cljs        # Re-frame subscriptions
    ├── views.cljs       # Main views
    └── components/      # UI components
```

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `LASTFM_API_KEY` | Last.fm API key | Yes |
| `LASTFM_API_SECRET` | Last.fm API shared secret | Yes |
| `OAUTH_CALLBACK_URL` | OAuth callback URL | Yes |
| `SESSION_SECRET` | Session encryption key (32+ chars) | Yes |
| `PORT` | Server port (default: `8080`) | No |
| `ENVIRONMENT` | `development` or `production` | No |
| `POLLING_INTERVAL_MS` | Scrobble poll interval (default: `20000`) | No |

## Testing

```bash
bb test                          # All backend tests (90 tests, 482 assertions)
npx playwright test              # E2E tests (25 tests, requires backend running)
npx shadow-cljs compile test && node target/test.js  # Frontend tests (66 tests)
```

Coverage: **79.5% forms / 91.0% lines** (cloverage)

## Deployment

The repository uses a unified GitHub Actions pipeline:

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| `ci.yml` | Every push / PR | Lint → test → build → Docker |
| `deploy-staging.yml` | Push to `develop` | Deploy to Cloud Run staging |
| `deploy-prod.yml` | Push to `main` | Deploy to Cloud Run production |

See [docs/deployment/](docs/deployment/) for full setup instructions including GCP configuration and required secrets.

## Contributing

1. Fork the repo and branch from `develop`
2. Make changes, run `bb test && bb lint`
3. Update `CHANGELOG.md` under `[Unreleased]`
4. Open a PR targeting `develop`

See [CONTRIBUTING.md](CONTRIBUTING.md) for branching strategy, commit conventions, and release process.

## License

MIT — see [LICENSE](LICENSE)

---

**Built for music lovers who care about their listening history**
