# PROJECT CHARTER: LASSO

**Project Name:** Lasso  
**Document Version:** 1.0  
**Document Date:** February 3, 2026  
**Document Owner:** Product Owner

---

## 1. Vision & Business Justification

### 1.1 Vision Statement
Lasso enables Last.fm users to accurately track their listening history during collaborative Spotify Jam sessions, ensuring complete scrobble data regardless of their role in the jam.

### 1.2 Problem Statement
Currently, when a Last.fm user participates in a Spotify Jam as a guest (non-owner), their listening activity during that session is not scrobbled to their Last.fm profile. Only the jam owner receives scrobbles for the music played. This creates:

- **Incomplete listening history** for jam participants
- **Inaccurate music statistics** (top artists, albums, tracks)
- **Loss of music discovery tracking** during collaborative listening sessions
- **Reduced value** of Last.fm's social and recommendation features

### 1.3 Business Justification
While Spotify Jams facilitate social music discovery and shared listening experiences, the current limitation diminishes the value proposition for Last.fm users who actively track their listening habits. 

**User Impact:**
- Last.fm users who value complete listening history lose data from jam sessions
- Music discovery during jams goes unrecorded, affecting recommendations
- Social listening context is missing from user profiles

**Opportunity:**
- Fill a clear gap in the Spotify + Last.fm integration ecosystem
- Serve the intersection of collaborative listening enthusiasts and data-conscious music fans
- Enable accurate listening history during an increasingly popular Spotify feature

### 1.4 Target Users
Primary: Last.fm users who participate in Spotify Jams as guests and want to maintain complete scrobble history.

---

## 2. Objectives & Success Criteria

### 2.1 Project Objectives

**Primary Objective:**
Enable Last.fm users to manually track scrobbles from another user's listening activity during Spotify Jam sessions.

**Secondary Objectives:**
- Provide a simple, intuitive interface for starting and stopping scrobble following
- Ensure data accuracy and integrity when mirroring scrobbles
- Maintain user privacy and security when handling Last.fm credentials
- Create a reliable, low-maintenance solution that works within Last.fm API constraints

### 2.2 Success Criteria

**Must Have (Launch Criteria):**
- Users can authenticate with Last.fm
- Users can input another Last.fm username to follow
- Users can start following that user's scrobbles (scrobbles are copied to their account in real-time)
- Users can pause/stop following
- Scrobbles are accurately recorded with correct metadata (artist, track, album, timestamp)

**Should Have (Post-Launch):**
- Mobile-responsive web interface
- Session history showing when/who was followed
- Basic error handling and user feedback

**Success Metrics:**
- Scrobble accuracy rate: >95% of followed scrobbles successfully recorded
- User can complete the full flow (login → follow → stop) in <2 minutes
- Zero data loss or corruption to user's Last.fm account

---

## 3. Stakeholders & Roles

### 3.1 Project Stakeholders

| Role | Responsibility | Decision Authority |
|------|---------------|-------------------|
| **Product Owner** | Defines requirements, prioritizes features, validates deliverables | Final approval on scope and feature set |
| **Developer/Engineer** | Designs, develops, tests, and deploys the application | Technical implementation decisions |
| **End User** | Uses the application, provides feedback | N/A |

*Note: In this project structure, you serve as Product Owner, Developer, and primary End User.*

### 3.2 External Stakeholders

| Party | Relationship | Constraints/Considerations |
|-------|-------------|---------------------------|
| **Last.fm** | API Provider | Must comply with Last.fm API Terms of Service, rate limits, and authentication requirements |
| **Spotify** | Indirect (via user behavior) | No direct integration; users participate in Spotify Jams through Spotify's native interface |

### 3.3 Communication & Decision Making

**Decision Framework:**
- Product decisions: Product Owner approves
- Technical decisions: Developer decides based on feasibility and best practices
- Scope changes: Documented and evaluated against project objectives

**Documentation:**
All decisions, changes, and rationale will be documented within SDLC artifacts.

---

## 4. Scope & Deliverables

### 4.1 In Scope

**Core Functionality:**
- Last.fm OAuth authentication
- User interface to input target Last.fm username to follow
- Real-time polling/fetching of target user's recent scrobbles
- Scrobbling fetched tracks to authenticated user's Last.fm account
- Start/pause/stop following controls
- Basic session state management (active/paused/stopped)

**Technical Components:**
- Mobile-responsive web application
- Last.fm API integration
- User session management
- Basic error handling and user notifications

**Documentation:**
- Technical design documentation
- API integration specifications
- Deployment documentation
- User-facing instructions/help text

### 4.2 Out of Scope

**Explicitly Excluded:**
- Automatic detection of Spotify Jam sessions
- Direct Spotify API integration
- Native mobile applications (iOS/Android)
- User account management beyond Last.fm OAuth
- Persistent user data storage (listening history, preferences)
- Multi-user following (following multiple users simultaneously)
- Social features (sharing, recommendations, user discovery)
- Automated scrobble conflict resolution
- Historical scrobble backfilling (only real-time scrobbles during active session)

### 4.3 Key Deliverables

**Phase 1 - Foundation:**
1. Project Charter (this document)
2. Product Requirements Document (PRD)
3. Technical Design Document (TDD)

**Phase 2 - Development:**
4. Functional web application with core features
5. Last.fm API integration layer
6. User interface (mobile-responsive)

**Phase 3 - Testing & Launch:**
7. Test Plan and test results
8. Deployment & Operations Plan
9. Deployed, production-ready application

---

## 5. Constraints & Assumptions

### 5.1 Technical Constraints

**Last.fm API Limitations:**
- API rate limits must be respected (typically 5 requests per second per API key)
- Scrobble submission has timing requirements (cannot scrobble tracks from the future, minimum track duration requirements)
- OAuth token expiration and refresh requirements

**Technology Constraints:**
- Web-based application only (no native mobile apps)
- Must work across modern web browsers (Chrome, Firefox, Safari, Edge)
- Dependent on Last.fm API availability and uptime

### 5.2 Resource Constraints

**Time:**
- Personal project with flexible timeline
- Development occurs during available personal time

**Budget:**
- Minimal hosting/infrastructure costs
- Free tier services where possible
- No budget for third-party services or APIs beyond free tiers

**Personnel:**
- Single developer/product owner
- No dedicated QA, design, or DevOps resources

### 5.3 Assumptions

**User Behavior:**
- Users understand they need to manually start/stop following when joining/leaving a Spotify Jam
- Users have both Last.fm and Spotify accounts
- Users trust the application with their Last.fm credentials (OAuth)

**Technical:**
- Last.fm API will remain stable and available
- Last.fm Terms of Service permit this use case
- Target user's scrobbles are publicly visible (not private profile)
- Polling interval for checking new scrobbles will be sufficient for real-time experience

**Business:**
- The gap between Spotify Jams and Last.fm scrobbling will continue to exist
- There is user demand for this functionality
- Last.fm will not implement this feature natively in the near term

---

## 6. Risks & Mitigation Strategies

### 6.1 Technical Risks

| Risk | Likelihood | Impact | Mitigation Strategy |
|------|-----------|--------|---------------------|
| **Last.fm API rate limiting** | High | High | Implement intelligent polling intervals, caching, and rate limit monitoring; graceful degradation if limits approached |
| **Last.fm API changes/deprecation** | Medium | High | Monitor Last.fm developer communications; design modular API layer for easier updates; document API version dependencies |
| **Duplicate scrobbles** | Medium | Medium | Implement deduplication logic; track already-scrobbled items; provide timestamp comparison |
| **Authentication token expiration** | Medium | Medium | Implement token refresh logic; clear user messaging when re-authentication needed |
| **Target user profile is private** | Low | Medium | Validate profile accessibility before starting session; provide clear error messaging |

### 6.2 User Experience Risks

| Risk | Likelihood | Impact | Mitigation Strategy |
|------|-----------|--------|---------------------|
| **User forgets to stop following** | High | Medium | Provide clear visual indicators of active session; consider session timeout warnings; allow easy session termination |
| **Confusion about manual process** | Medium | Medium | Clear onboarding/instruction text; prominent UI indicators; help documentation |
| **Mobile usability issues** | Medium | High | Prioritize mobile-responsive design; test on multiple devices; simple, touch-friendly UI |

### 6.3 Compliance & Legal Risks

| Risk | Likelihood | Impact | Mitigation Strategy |
|------|-----------|--------|---------------------|
| **Last.fm ToS violation** | Low | Critical | Review Last.fm API Terms of Service thoroughly; ensure compliance; avoid prohibited use cases |
| **User data privacy concerns** | Low | High | Use OAuth (no password storage); minimal data retention; clear privacy messaging |

### 6.4 Project Risks

| Risk | Likelihood | Impact | Mitigation Strategy |
|------|-----------|--------|---------------------|
| **Scope creep** | Medium | Medium | Maintain clear scope boundaries; document any scope changes; refer to project charter for decisions |
| **Loss of motivation/time** | Medium | High | Break into small, achievable milestones; maintain clear documentation for project resumption |
| **Technical complexity underestimation** | Medium | Medium | Start with MVP; iterate based on actual complexity; time-box exploration of difficult problems |

---

## 7. Timeline & Milestones

### 7.1 Project Phases

The project will be organized into sprints, allowing for flexible pacing based on available development time and complexity encountered during implementation.

### 7.2 Key Milestones

| Milestone | Deliverables | Target Sprint |
|-----------|-------------|-------------------|
| **M1: Project Initiation** | Project Charter (complete), PRD drafted | Sprint 1 |
| **M2: Design Phase** | Technical Design Document, Architecture defined, API integration plan, Development environment setup, Project scaffolding complete, Build tools configured | Sprint 2 |
| **M3: Development - Core Backend** | Last.fm API integration, Authentication flow, Scrobble fetching/posting logic | Sprint 3-4 |
| **M4: Development - Frontend** | User interface, Session controls, Mobile-responsive design | Sprint 5-6 |
| **M5: Integration & Testing** | End-to-end functionality, Test plan execution, Bug fixes | Sprint 7 |
| **M6: Deployment** | Production deployment, Operations documentation, User documentation | Sprint 8 |
| **M7: Launch** | Live application, Initial user testing (self), Iteration based on feedback | Sprint 9 |

**M2 Detailed Activities:**
- **Design Documentation:** Complete Technical Design Document
- **Architecture:** Define system architecture, component interactions, data models
- **API Planning:** Design REST API endpoints, define request/response formats
- **Environment Setup:**
  - Install required tools (JDK, Clojure CLI, Node.js, VS Code/Calva)
  - Configure Last.fm API access (obtain API keys)
  - Set up Google Cloud project for deployment
- **Project Scaffolding:**
  - Initialize Git repository
  - Create project directory structure (monorepo layout)
  - Configure deps.edn with dependencies
  - Configure shadow-cljs.edn for ClojureScript builds
  - Set up Tailwind CSS configuration
  - Create basic Dockerfile
  - Set up GitHub Actions workflow skeleton
- **Verification:**
  - Backend REPL connects successfully
  - Frontend hot-reloading works
  - Basic "Hello World" runs end-to-end
  - Docker build completes successfully
  - CI pipeline runs (even if just linting initially)

### 7.3 Phase Breakdown

**Phase 1: Planning & Design (Sprints 1-2)**
- Complete Project Charter
- Develop Product Requirements Document
- Create Technical Design Document
- Define implementation approach
- Set up development environment
- Create project structure and scaffolding
- Configure build tools and verify REPL workflow
- Establish CI/CD pipeline skeleton

**Phase 2: Development (Sprints 3-6)**
- Backend development (API integration, business logic)
- Frontend development (UI, user flows)
- Iterative testing during development

**Phase 3: Testing & Launch (Sprints 7-9)**
- Comprehensive testing
- Bug fixes and refinements
- Deployment and go-live
- Post-launch monitoring and iteration

### 7.4 Dependencies & Critical Path

**Critical Path Items:**
1. Project setup and scaffolding (blocks all development)
2. Last.fm API OAuth implementation (blocks all user features)
3. Scrobble fetching logic (blocks core functionality)
4. Scrobble posting logic (blocks core functionality)

**Key Dependencies:**
- Last.fm API access and documentation
- Hosting/deployment environment selection
- OAuth callback URL configuration

---

## 8. Approval & Sign-off

**Project Charter Approved By:**

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Product Owner | [Your Name] | [Date] | _________ |
| Developer | [Your Name] | [Date] | _________ |

---

**End of Project Charter**
