# Lasso

> Track your Spotify Jam listening on Last.fm

Lasso enables Last.fm users to scrobble their listening history during Spotify Jam sessions. When participating in Spotify Jams as a guest, your listening activity isn't automatically scrobbled to Last.fm. Lasso solves this by allowing you to temporarily follow another Last.fm user and mirror their scrobbles in real-time.

## Features

- **Real-time Scrobbling**: Automatically track plays from Spotify Jams
- **Temporary Following**: Mirror scrobbles from any public Last.fm user
- **Session Control**: Start, pause, resume, and stop following at any time
- **Activity Feed**: View recent scrobbles as they happen
- **Secure Authentication**: OAuth 2.0 with Last.fm (no password storage)

## Technology Stack

**Backend (Clojure)**
- Pedestal web framework with Jetty
- Last.fm API integration
- OAuth 2.0 authentication
- Real-time polling engine

**Frontend (ClojureScript)**
- Reagent (React wrapper)
- Re-frame state management
- Tailwind CSS styling
- Hot reload development

**Infrastructure**
- Docker containerization
- Google Cloud Run deployment
- GitHub Actions CI/CD

## Prerequisites

- Java 11 or higher
- Clojure CLI (tools.deps)
- Node.js 18 or higher
- npm
- **Babashka** (recommended for simplified development workflow) - [Installation guide](https://github.com/babashka/babashka#installation)
- Docker (optional, for containerized deployment)
- Last.fm API credentials ([Register here](https://www.last.fm/api/account/create))

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/jackmoch/lasso.git
cd lasso
```

### 2. Configure environment

Copy the example environment file and add your Last.fm API credentials:

```bash
cp .env.example .env
```

Edit `.env` and set:
```bash
LASTFM_API_KEY=your_api_key_here
LASTFM_API_SECRET=your_api_secret_here
OAUTH_CALLBACK_URL=http://localhost:8080/api/auth/callback
SESSION_SECRET=$(openssl rand -base64 32)
```

### 3. Install dependencies

```bash
# Download Clojure dependencies
clojure -P

# Install Node dependencies
npm install

# Build Tailwind CSS
npm run build:css
```

### 4. Start development environment

**🎯 Recommended: One-command startup with Babashka**

```bash
# Install babashka (one-time setup)
brew install babashka  # macOS
# See https://github.com/babashka/babashka#installation for other platforms

# Start everything (backend + frontend + hot reload)
bb dev
```

That's it! The integrated REPL will start with both backend and frontend running.

**Alternative: Direct REPL control**

```bash
clj -M:dev
```

Once the REPL starts, run:
```clojure
user=> (start)  ; Starts backend + frontend watch together
```

**Manual control (individual processes)**

If you prefer to control each process separately:

```bash
# Terminal 1 - Backend only
clj -M:dev
user=> (start-backend)

# Terminal 2 - Frontend only
bb frontend
# or: npx shadow-cljs watch app

# Terminal 3 - CSS watch (optional)
bb css
# or: npm run watch:css
```

### 5. Open the application

Visit [http://localhost:8080](http://localhost:8080) in your browser.

The frontend development server also runs on [http://localhost:8280](http://localhost:8280) with additional shadow-cljs tooling.

## Development Workflow

### Integrated REPL

The development environment uses an integrated REPL with shadow-cljs, giving you full control over both backend and frontend from one place.

**REPL Commands (in `user` namespace):**

```clojure
(start)          ; Start backend + frontend together (or use 'go')
(stop)           ; Stop everything
(restart)        ; Restart everything
(reset)          ; Reload namespaces + restart

; Individual control
(start-backend)  ; Start only backend server
(stop-backend)   ; Stop only backend
(start-frontend) ; Start only frontend watch
(stop-frontend)  ; Stop only frontend

; ClojureScript REPL
(cljs-repl)      ; Connect to browser for interactive CLJS development
```

### Hot Reload

Everything reloads automatically:
- **Frontend**: Edit `.cljs` files → instant browser update (state preserved)
- **Backend**: Edit `.clj` files → use `(restart)` or `(reset)` in REPL
- **CSS**: Edit Tailwind classes → automatic rebuild (when using `bb css`)

### Babashka Tasks

Quick commands for common operations:

```bash
bb dev           # Start full development environment
bb test          # Run all tests
bb test:watch    # Run tests in watch mode
bb build         # Build production artifacts
bb clean         # Clean build artifacts
bb lint          # Lint code
bb tasks         # List all available tasks
```

See `bb.edn` for all available tasks.

### Project Structure

```
lasso/
├── src/
│   ├── clj/lasso/          # Backend Clojure code
│   │   ├── server.clj      # Server lifecycle
│   │   ├── config.clj      # Configuration
│   │   └── routes.clj      # HTTP routes
│   └── cljs/lasso/         # Frontend ClojureScript code
│       ├── core.cljs       # App initialization
│       └── views.cljs      # UI components
├── dev/
│   └── user.clj            # REPL utilities
├── resources/
│   └── public/             # Static assets
│       ├── index.html      # HTML template
│       └── css/            # Stylesheets
├── test/                   # Tests
├── deps.edn                # Clojure dependencies
├── package.json            # Node dependencies
├── shadow-cljs.edn         # ClojureScript build config
├── tailwind.config.js      # Tailwind CSS config
└── Dockerfile              # Container definition
```

## Available Scripts

### Development

```bash
npm run watch          # Start shadow-cljs in watch mode
npm run watch:css      # Watch and rebuild Tailwind CSS
```

### Building

```bash
npm run build:css      # Build minified CSS
npm run release        # Build production frontend + CSS
clj -X:uberjar        # Build backend JAR
```

### Testing

```bash
clj -M:test           # Run backend tests
clj-kondo --lint src  # Lint Clojure/ClojureScript code
```

### Cleanup

```bash
npm run clean         # Remove build artifacts
```

## Docker

### Build the image

```bash
docker build -t lasso:latest .
```

### Run the container

```bash
docker run -p 8080:8080 \
  -e LASTFM_API_KEY=your_key \
  -e LASTFM_API_SECRET=your_secret \
  -e SESSION_SECRET=your_session_secret \
  lasso:latest
```

## Testing

### Backend Tests

```bash
# Run all tests
clj -M:test

# Run specific namespace
clj -M:test --focus lasso.auth.core-test

# Run with watch mode (in REPL)
(require '[kaocha.repl :as k])
(k/run-all)
```

### Linting

```bash
# Lint all source code
clj-kondo --lint src

# Lint specific directory
clj-kondo --lint src/clj/lasso
```

## Deployment

### GitHub Actions CI/CD

The repository includes two workflows:

**CI (`ci.yml`)** - Runs on every push and PR:
- Lints Clojure code with clj-kondo
- Builds frontend and backend
- Builds Docker image

**Deploy (`deploy.yml`)** - Manual deployment trigger:
- Builds and pushes Docker image to Google Container Registry
- Deploys to Google Cloud Run
- *Note: Requires configuration in Sprint 8*

### Manual Deployment to Cloud Run

See [docs/DEPLOYMENT_SECRETS.md](docs/DEPLOYMENT_SECRETS.md) for detailed setup instructions.

```bash
# Build and tag
docker build -t gcr.io/PROJECT_ID/lasso:latest .

# Push to GCR
docker push gcr.io/PROJECT_ID/lasso:latest

# Deploy to Cloud Run
gcloud run deploy lasso \
  --image gcr.io/PROJECT_ID/lasso:latest \
  --platform managed \
  --region us-central1 \
  --set-env-vars ENVIRONMENT=production \
  --set-secrets LASTFM_API_KEY=lastfm-api-key:latest,LASTFM_API_SECRET=lastfm-api-secret:latest,SESSION_SECRET=session-secret:latest
```

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `LASTFM_API_KEY` | Last.fm API key | - | Yes |
| `LASTFM_API_SECRET` | Last.fm API shared secret | - | Yes |
| `OAUTH_CALLBACK_URL` | OAuth callback URL | `http://localhost:8080/api/auth/callback` | Yes |
| `SESSION_SECRET` | Session encryption key (min 32 chars) | - | Yes |
| `PORT` | Server port | `8080` | No |
| `HOST` | Server host | `0.0.0.0` | No |
| `ENVIRONMENT` | Environment name (`development`/`production`) | `development` | No |
| `POLLING_INTERVAL_MS` | Scrobble polling interval | `20000` | No |

## Troubleshooting

### REPL won't start

```bash
# Clear dependency cache and re-download
rm -rf .cpcache
clojure -P
```

### shadow-cljs build fails

```bash
# Clear shadow-cljs cache
rm -rf .shadow-cljs
npx shadow-cljs clean

# Reinstall Node dependencies
rm -rf node_modules package-lock.json
npm install
```

### Hot reload not working

- Hard refresh browser: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows/Linux)
- Check shadow-cljs terminal output for compilation errors
- Verify shadow-cljs watch is running
- Check browser console for errors

### Port 8080 already in use

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process (replace PID with actual process ID)
kill -9 PID
```

### Tailwind classes not applying

```bash
# Rebuild CSS
npm run build:css

# Verify tailwind.config.js content paths include your files
# Check resources/public/css/tailwind.css was generated
```

### Docker build fails

```bash
# Clear Docker build cache
docker builder prune

# Build with no cache
docker build --no-cache -t lasso:latest .
```

## Project Status

**Current Sprint**: Sprint 2 - Development Environment Setup ✅

**Completed:**
- ✅ Project scaffolding
- ✅ Build system configuration
- ✅ Development environment
- ✅ Basic "Hello World" application
- ✅ Docker containerization
- ✅ CI/CD pipeline skeleton

**Upcoming Sprints:**
- Sprint 3-4: Backend development (Last.fm API, OAuth, session management)
- Sprint 5-6: Frontend development (UI components, session controls, activity feed)
- Sprint 7: Integration testing
- Sprint 8: Production deployment
- Sprint 9: Launch preparation

## Contributing

This is a personal project currently in active development. Contributions are welcome!

**Please read [CONTRIBUTING.md](CONTRIBUTING.md) for detailed information on:**
- Branching strategy and Git workflow
- Commit message conventions
- Pull request process
- Code standards and style guidelines
- Release process and versioning

### Quick Start for Contributors

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes following our [code standards](CONTRIBUTING.md#code-standards)
4. Update [CHANGELOG.md](CHANGELOG.md) under `[Unreleased]`
5. Run tests and linter: `clj -M:test && clj-kondo --lint src`
6. Commit using [Conventional Commits](https://www.conventionalcommits.org/)
7. Push and create a Pull Request to `develop` branch

### Development Guidelines

- Follow the [Clojure Style Guide](https://guide.clojure.style/)
- Use `cljfmt` for code formatting
- Run tests before committing: `clj -M:test`
- Run linter: `clj-kondo --lint src`
- Keep line length under 100 characters
- Document public functions with docstrings
- Use semantic versioning for releases

## License

MIT License - See LICENSE file for details

## Documentation

- **Setup & Usage**: This README
- **Contributing**: [CONTRIBUTING.md](CONTRIBUTING.md)
- **Changelog**: [CHANGELOG.md](CHANGELOG.md)
- **Claude Code Instructions**: [CLAUDE.md](CLAUDE.md)

**Additional Documentation** (see [docs/README.md](docs/README.md) for full structure):
- **Project Planning**: [docs/project-planning/](docs/project-planning/) - PRD, TDD, Project Charter
- **Development**: [docs/development/](docs/development/) - Workflows and processes
- **Deployment**: [docs/deployment/](docs/deployment/) - Deployment guides
- **Sprints**: [docs/sprints/](docs/sprints/) - Sprint plans and summaries

## Support

- **Issues**: [GitHub Issues](https://github.com/jackmoch/lasso/issues)
- **Documentation**: See [docs/](docs/) directory organized by category
- **Last.fm API Docs**: [Last.fm API Documentation](https://www.last.fm/api)

## Acknowledgments

- Built with [Clojure](https://clojure.org/) and [ClojureScript](https://clojurescript.org/)
- Powered by [Last.fm API](https://www.last.fm/api)
- Inspired by the need to track Spotify Jam sessions

---

**Made with ♥ for music lovers who care about their listening history**
