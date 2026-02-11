# PRODUCT REQUIREMENTS DOCUMENT: LASSO

**Project Name:** Lasso  
**Document Version:** 1.0  
**Document Date:** February 3, 2026  
**Document Owner:** Product Owner  
**Status:** Draft

---

## 1. Executive Summary

**Product Name:** Lasso

**Product Vision:** Enable Last.fm users to maintain complete listening history during Spotify Jam sessions by allowing them to temporarily follow and mirror another user's scrobbles.

**Target Release:** Sprint 9 (per Project Charter)

**Key Stakeholders:** Product Owner, Developer, Last.fm API, End Users

**Summary:**
Lasso is a mobile-responsive web application that solves a specific gap in the Last.fm and Spotify integration. When users participate in Spotify Jams as guests, their listening activity is not scrobbled to their Last.fm profile. Lasso allows users to authenticate with Last.fm, specify another user to follow, and automatically copy that user's scrobbles to their own account during the session. This ensures complete listening history and accurate music statistics for all Jam participants.

---

## 2. Product Overview

### 2.1 Problem Statement
Last.fm users who participate in Spotify Jams as guests (non-owners) do not receive scrobbles for tracks played during the session. Only the Jam owner's Last.fm account records the listening activity. This results in:
- Incomplete listening histories
- Inaccurate music discovery tracking
- Missing data for recommendations and statistics
- Reduced value from Last.fm's tracking features

### 2.2 Solution Overview
Lasso provides a manual workaround by allowing users to:
1. Authenticate with their Last.fm account
2. Specify another Last.fm user to follow (typically the Jam owner)
3. Start a following session that mirrors the target user's scrobbles in real-time
4. Pause or stop the session when the Jam ends

### 2.3 Target Users
- **Primary:** Last.fm users who regularly participate in Spotify Jams as guests
- **Secondary:** Last.fm power users who value complete listening history and accurate statistics
- **User Characteristics:**
  - Active Last.fm users with established scrobbling habits
  - Participate in collaborative listening sessions
  - Tech-savvy enough to use a web application
  - Understand the manual nature of the solution

---

## 3. User Personas

### Persona 1: The Completionist
- **Name:** Alex
- **Age:** 28
- **Background:** Music enthusiast, tracks every song on Last.fm for 5+ years
- **Goals:** Maintain 100% accurate listening history, discover new music through friends
- **Pain Points:** Missing scrobbles from Jam sessions create gaps in their meticulously tracked history
- **Use Case:** Uses Lasso every time they join a friend's Spotify Jam to ensure no listening data is lost

### Persona 2: The Social Listener
- **Name:** Jordan
- **Age:** 24
- **Background:** Enjoys music discovery through friends, casual Last.fm user
- **Goals:** Track interesting music discovered in social listening sessions
- **Pain Points:** Forgets what songs were played during Jams, wants to revisit them later
- **Use Case:** Uses Lasso during weekend listening sessions with roommates

---

## 4. User Stories

### Epic 1: User Authentication
**As a** Last.fm user  
**I want to** securely log in to Lasso with my Last.fm account  
**So that** the application can scrobble tracks on my behalf

**User Stories:**
- **US-1.1:** As a user, I want to initiate Last.fm OAuth authentication so that I don't have to share my password
- **US-1.2:** As a user, I want to see confirmation that I'm logged in so that I know the app has access to my account
- **US-1.3:** As a user, I want to log out when I'm done so that my session is terminated securely

### Epic 2: Target User Selection
**As a** logged-in user  
**I want to** specify which Last.fm user to follow  
**So that** I can mirror their scrobbles to my account

**User Stories:**
- **US-2.1:** As a user, I want to enter a Last.fm username in a text field so that I can specify who to follow
- **US-2.2:** As a user, I want to see validation that the username exists and is accessible so that I know the following will work
- **US-2.3:** As a user, I want to see an error if the target user's profile is private so that I understand why I can't follow them

### Epic 3: Session Management
**As a** user with a specified target  
**I want to** control when scrobble following starts and stops  
**So that** I only mirror scrobbles during my Jam session

**User Stories:**
- **US-3.1:** As a user, I want to start following with a clear button/action so that scrobbling begins
- **US-3.2:** As a user, I want to see a clear indicator that following is active so that I know it's working
- **US-3.3:** As a user, I want to pause following temporarily so that I can stop without ending the session
- **US-3.4:** As a user, I want to resume following after pausing so that I can continue the session
- **US-3.5:** As a user, I want to stop following completely so that I can end the session
- **US-3.6:** As a user, I want to see recently scrobbled tracks in real-time so that I can verify it's working

### Epic 4: Scrobble Processing
**As a** user with active following  
**I want** the app to accurately mirror scrobbles  
**So that** my listening history is complete and correct

**User Stories:**
- **US-4.1:** As a user, I want scrobbles to include accurate metadata (artist, track, album, timestamp) so that my Last.fm data is correct
- **US-4.2:** As a user, I want duplicate scrobbles to be prevented so that I don't have repeated entries
- **US-4.3:** As a user, I want to see errors if scrobbling fails so that I know something went wrong

### Epic 5: User Experience & Feedback
**As a** user of the application  
**I want** clear feedback and guidance  
**So that** I understand how to use the app effectively

**User Stories:**
- **US-5.1:** As a user, I want clear instructions on how to use Lasso so that I don't get confused
- **US-5.2:** As a user, I want error messages to be helpful so that I know how to fix problems
- **US-5.3:** As a user, I want the interface to work well on mobile so that I can use it on my phone during Jams

---

## 5. Functional Requirements

### 5.1 Authentication (FR-AUTH)

**FR-AUTH-001: OAuth Integration**
- System shall implement Last.fm OAuth 2.0 authentication flow
- System shall redirect users to Last.fm for authorization
- System shall handle OAuth callback and token exchange
- System shall securely store session tokens
- Priority: Must Have

**FR-AUTH-002: Session Management**
- System shall maintain user authentication state during active sessions
- System shall provide a logout function that clears session data
- System shall handle token expiration gracefully
- Priority: Must Have

**FR-AUTH-003: Authentication Status Display**
- System shall display the authenticated user's Last.fm username
- System shall show clear indication of login/logout state
- Priority: Must Have

### 5.2 Target User Management (FR-TARGET)

**FR-TARGET-001: Username Input**
- System shall provide a text input field for entering target Last.fm username
- System shall validate username format before submission
- Priority: Must Have

**FR-TARGET-002: Profile Validation**
- System shall verify that the target username exists on Last.fm
- System shall verify that the target user's recent tracks are publicly accessible
- System shall display appropriate error messages for invalid or private profiles
- Priority: Must Have

**FR-TARGET-003: Target User Display**
- System shall display the currently selected target username
- System shall allow changing the target user when not actively following
- Priority: Must Have

### 5.3 Scrobble Following (FR-FOLLOW)

**FR-FOLLOW-001: Following Control**
- System shall provide a "Start Following" button to begin session
- System shall provide a "Pause" button to temporarily halt scrobbling
- System shall provide a "Resume" button to continue after pausing
- System shall provide a "Stop" button to end the session
- Priority: Must Have

**FR-FOLLOW-002: Session State Management**
- System shall maintain session state: Not Started, Active, Paused, Stopped
- System shall visually indicate current session state
- System shall prevent state transitions that don't make logical sense
- Priority: Must Have

**FR-FOLLOW-003: Real-Time Scrobble Polling**
- System shall poll target user's recent tracks at regular intervals (respect API rate limits)
- System shall only poll when session is in Active state
- System shall handle API failures gracefully with retry logic
- Priority: Must Have

**FR-FOLLOW-004: Scrobble Mirroring**
- System shall identify new scrobbles from the target user since last poll
- System shall submit new scrobbles to authenticated user's Last.fm account
- System shall preserve original metadata: artist, track, album, timestamp
- Priority: Must Have

**FR-FOLLOW-005: Duplicate Prevention**
- System shall track which scrobbles have already been mirrored
- System shall not submit duplicate scrobbles for the same track at the same timestamp
- Priority: Must Have

**FR-FOLLOW-006: Real-Time Activity Display**
- System shall display recently mirrored scrobbles in the interface
- System shall show timestamp of last successful poll
- System shall display scrobble count for current session
- Priority: Should Have

### 5.4 Error Handling (FR-ERROR)

**FR-ERROR-001: API Error Handling**
- System shall handle Last.fm API errors (rate limiting, network failures, invalid tokens)
- System shall display user-friendly error messages
- System shall log technical error details for debugging
- Priority: Must Have

**FR-ERROR-002: User Input Validation**
- System shall validate all user inputs before submission
- System shall display inline validation messages
- Priority: Must Have

**FR-ERROR-003: Session Recovery**
- System shall allow users to retry failed operations
- System shall maintain session state through temporary failures
- Priority: Should Have

### 5.5 User Interface (FR-UI)

**FR-UI-001: Responsive Design**
- System shall provide mobile-responsive interface that works on phones, tablets, and desktop
- System shall use touch-friendly controls for mobile devices
- Priority: Must Have

**FR-UI-002: Visual Feedback**
- System shall provide loading indicators during API calls
- System shall provide success/error notifications for user actions
- System shall use clear visual states for session status
- Priority: Must Have

**FR-UI-003: Help & Instructions**
- System shall provide onboarding instructions for first-time users
- System shall include help text explaining the manual process
- Priority: Should Have

---

## 6. Non-Functional Requirements

### 6.1 Performance (NFR-PERF)

**NFR-PERF-001: Response Time**
- Web pages shall load within 3 seconds on standard broadband connection
- User actions (button clicks, form submissions) shall provide feedback within 1 second
- Priority: Must Have

**NFR-PERF-002: API Rate Limit Compliance**
- System shall respect Last.fm API rate limits (5 requests per second)
- System shall implement intelligent polling intervals to avoid hitting rate limits
- Suggested polling interval: 10-30 seconds during active following
- Priority: Must Have

**NFR-PERF-003: Scrobble Latency**
- System shall mirror scrobbles within 60 seconds of target user's scrobble
- System shall prioritize accuracy over speed
- Priority: Should Have

### 6.2 Security (NFR-SEC)

**NFR-SEC-001: Authentication Security**
- System shall use OAuth 2.0 exclusively (no password storage)
- System shall store OAuth tokens securely (encrypted, server-side only)
- System shall use HTTPS for all communications
- Priority: Must Have

**NFR-SEC-002: Data Privacy**
- System shall not store user listening history beyond active session
- System shall not share user data with third parties
- System shall clear session data on logout
- Priority: Must Have

**NFR-SEC-003: API Key Security**
- System shall store Last.fm API keys securely (environment variables, not in code)
- System shall not expose API keys in client-side code
- Priority: Must Have

### 6.3 Reliability (NFR-REL)

**NFR-REL-001: Uptime**
- System shall target 95% uptime (acknowledging this is a personal project)
- System shall handle graceful degradation during partial failures
- Priority: Should Have

**NFR-REL-002: Data Integrity**
- System shall ensure 95%+ accuracy of scrobbled metadata
- System shall prevent data corruption or loss to user's Last.fm account
- Priority: Must Have

**NFR-REL-003: Failure Recovery**
- System shall recover from temporary network failures without losing session state
- System shall allow users to manually retry failed operations
- Priority: Must Have

### 6.4 Usability (NFR-USE)

**NFR-USE-001: Ease of Use**
- New users shall be able to complete full workflow (login → follow → stop) within 2 minutes
- Interface shall be intuitive without requiring extensive documentation
- Priority: Must Have

**NFR-USE-002: Accessibility**
- System shall support modern web browsers (Chrome, Firefox, Safari, Edge - latest 2 versions)
- System shall be usable on mobile devices (iOS Safari, Android Chrome)
- Priority: Must Have

**NFR-USE-003: Visual Clarity**
- Interface shall clearly indicate current session state at all times
- Error messages shall be clear and actionable
- Priority: Must Have

### 6.5 Maintainability (NFR-MAINT)

**NFR-MAINT-001: Code Quality**
- Code shall be well-documented with comments
- Code shall follow consistent style guidelines
- System shall have modular architecture for easier updates
- Priority: Should Have

**NFR-MAINT-002: API Abstraction**
- Last.fm API integration shall be isolated in a separate module
- API version dependencies shall be documented
- Priority: Should Have

**NFR-MAINT-003: Logging**
- System shall log errors and important events for debugging
- Logs shall not contain sensitive user data
- Priority: Should Have

### 6.6 Scalability (NFR-SCALE)

**NFR-SCALE-001: User Capacity**
- System shall support at least 10 concurrent users (initial target)
- System shall be designed to scale horizontally if demand grows
- Priority: Should Have

**NFR-SCALE-002: Resource Efficiency**
- System shall minimize hosting costs by using efficient polling strategies
- System shall use free tier services where possible
- Priority: Must Have

### 6.7 Compatibility (NFR-COMPAT)

**NFR-COMPAT-001: Browser Support**
- Chrome (latest 2 versions)
- Firefox (latest 2 versions)
- Safari (latest 2 versions)
- Edge (latest 2 versions)
- Mobile browsers (iOS Safari, Android Chrome)
- Priority: Must Have

**NFR-COMPAT-002: Device Support**
- Desktop/laptop computers (Windows, macOS, Linux)
- Tablets (iOS, Android)
- Smartphones (iOS, Android)
- Priority: Must Have

---

## 7. User Interface & Experience

### 7.1 User Flow

**Primary User Flow:**
1. User lands on Lasso home page
2. User clicks "Login with Last.fm"
3. User is redirected to Last.fm OAuth authorization
4. User authorizes Lasso
5. User is redirected back to Lasso (now authenticated)
6. User enters target Last.fm username
7. System validates target username
8. User clicks "Start Following"
9. System begins polling and mirroring scrobbles
10. User sees real-time updates of mirrored tracks
11. User clicks "Stop Following" when Jam ends
12. Session ends, user can start a new session or logout

**Alternative Flows:**
- Pause/Resume: User can pause following temporarily and resume later
- Error Recovery: If validation or scrobbling fails, user sees error and can retry
- Re-authentication: If token expires, user is prompted to re-authenticate

### 7.2 Screen/Page Requirements

**Page 1: Landing/Login Page**
- Application title and brief description
- "Login with Last.fm" button (prominent)
- Brief instructions explaining what Lasso does
- Help/FAQ link (optional)

**Page 2: Main Application Page (Authenticated)**
- Header showing logged-in username with logout option
- Section 1: Target User Input
  - Text field for Last.fm username
  - Validate button (or auto-validate on blur)
  - Error message display area
- Section 2: Session Controls
  - Current session status indicator (Not Started/Active/Paused/Stopped)
  - Target user display (who you're following)
  - Start/Pause/Resume/Stop buttons (contextual based on state)
- Section 3: Activity Feed (optional but recommended)
  - List of recently mirrored scrobbles
  - Last poll timestamp
  - Session scrobble count
- Section 4: Help/Instructions
  - Collapsible section with usage instructions

### 7.3 UI States

**Session States:**
1. **Not Started:** Default state after login
   - Show target user input
   - Show "Start Following" button (disabled until valid target entered)
   
2. **Active:** Following in progress
   - Show target user (locked, can't change)
   - Show "Pause" and "Stop" buttons
   - Show activity feed with real-time updates
   - Visual indicator (pulsing dot, "LIVE" badge, etc.)
   
3. **Paused:** Following temporarily stopped
   - Show target user (locked, can't change)
   - Show "Resume" and "Stop" buttons
   - Show activity feed (no new updates)
   - Visual indicator (pause icon)
   
4. **Stopped:** Session ended
   - Clear target user, allow new input
   - Reset to "Not Started" state
   - Show summary of completed session (optional)

### 7.4 Visual Design Guidelines

**Color Scheme:**
- Use Last.fm brand colors as inspiration (red/black/white)
- Clear distinction between active/paused/stopped states
- High contrast for accessibility

**Typography:**
- Clean, readable fonts
- Clear hierarchy (headings, body text, labels)
- Mobile-friendly sizes

**Layout:**
- Single-page application (SPA) design
- Vertical layout for mobile, potentially multi-column for desktop
- Sticky header with logout option
- Clear visual separation between sections

---

## 8. Data Requirements

### 8.1 Data Entities

**Entity 1: User Session**
- **Description:** Represents an authenticated user's session
- **Attributes:**
  - Session ID (generated)
  - Last.fm Username
  - OAuth Access Token (encrypted)
  - OAuth Token Secret (encrypted)
  - Session Start Time
  - Last Activity Time
- **Storage:** Server-side session storage (temporary, cleared on logout)
- **Retention:** Duration of active session only, deleted on logout or expiration

**Entity 2: Following Session**
- **Description:** Represents an active following session
- **Attributes:**
  - Session ID (references User Session)
  - Target Username
  - Session State (Not Started, Active, Paused, Stopped)
  - Start Timestamp
  - Pause Timestamp (if applicable)
  - Stop Timestamp (if applicable)
  - Last Poll Timestamp
  - Scrobble Count
- **Storage:** Server-side session storage (temporary)
- **Retention:** Duration of active session only

**Entity 3: Scrobble Cache**
- **Description:** Temporary cache of already-mirrored scrobbles to prevent duplicates
- **Attributes:**
  - Session ID (references Following Session)
  - Track Artist
  - Track Name
  - Track Album (optional)
  - Original Timestamp
  - Mirror Timestamp
- **Storage:** Server-side session storage or in-memory cache
- **Retention:** Duration of active session only, cleared when session stops

### 8.2 Data Flow

**Authentication Flow:**
1. User initiates login → System redirects to Last.fm OAuth
2. Last.fm returns authorization code → System exchanges for access token
3. System stores encrypted tokens in server-side session
4. System retrieves authenticated user's Last.fm username
5. Session created and maintained

**Following Flow:**
1. User enters target username → System validates via Last.fm API
2. User starts following → Following Session created
3. System polls target user's recent tracks (API call)
4. System compares with Scrobble Cache to identify new tracks
5. System submits new tracks to authenticated user's account (API call)
6. System updates Scrobble Cache with mirrored tracks
7. Repeat steps 3-6 at polling intervals while Active

**Session End Flow:**
1. User stops following → Following Session marked as Stopped
2. Scrobble Cache cleared
3. User can start new session or logout

### 8.3 Data Privacy & Security

**Privacy Requirements:**
- No persistent storage of listening history
- No collection of user data beyond what's necessary for functionality
- OAuth tokens never exposed to client-side code
- Session data isolated per user
- All data deleted on logout

**Security Measures:**
- HTTPS encryption for all data transmission
- OAuth tokens encrypted at rest
- Session tokens with expiration
- No logging of sensitive user data
- API keys stored in environment variables

---

## 9. API Specifications

### 9.1 Last.fm API Integration

**API Documentation:** https://www.last.fm/api

**Required API Methods:**

**9.1.1 Authentication**

**Method:** `auth.getToken`
- **Purpose:** Initiate OAuth flow
- **Parameters:** api_key
- **Returns:** Token for authorization URL
- **Usage:** Step 1 of OAuth flow

**Method:** `auth.getSession`
- **Purpose:** Exchange authorized token for session key
- **Parameters:** api_key, token
- **Returns:** Session key and username
- **Usage:** Step 2 of OAuth flow (after user authorizes)

**9.1.2 User Data Retrieval**

**Method:** `user.getRecentTracks`
- **Purpose:** Fetch target user's recent scrobbles
- **Parameters:**
  - user (required): Target Last.fm username
  - api_key (required)
  - limit (optional): Number of tracks to return (suggest 10-50)
  - from (optional): Unix timestamp to fetch tracks from
  - to (optional): Unix timestamp to fetch tracks until
- **Returns:** Array of recent tracks with metadata
- **Rate Limit:** 5 requests per second
- **Usage:** Poll during active following session

**Method:** `user.getInfo`
- **Purpose:** Validate target username and check profile accessibility
- **Parameters:**
  - user (required): Target Last.fm username
  - api_key (required)
- **Returns:** User profile information
- **Usage:** Validate target before starting session

**9.1.3 Scrobble Submission**

**Method:** `track.scrobble`
- **Purpose:** Submit scrobble to authenticated user's account
- **Parameters:**
  - artist (required): Artist name
  - track (required): Track name
  - timestamp (required): Unix timestamp when track was played
  - album (optional): Album name
  - api_key (required)
  - sk (required): Session key from authentication
  - api_sig (required): Method signature for authentication
- **Returns:** Scrobble confirmation
- **Rate Limit:** 5 requests per second
- **Usage:** Mirror each new track from target user

**9.1.4 Method Signature Generation**

All write operations (including track.scrobble) require an `api_sig` parameter:
- Concatenate all parameters (except format) in alphabetical order: `param1value1param2value2...`
- Append API secret
- Generate MD5 hash
- Include as `api_sig` parameter

### 9.2 API Rate Limiting Strategy

**Compliance Measures:**
- Maximum 5 requests per second per API key (Last.fm limit)
- Implement request queuing to avoid bursts
- Suggested polling interval: 15-30 seconds for active sessions
- Exponential backoff on rate limit errors
- Circuit breaker pattern for repeated failures

**Polling Strategy:**
- Active session: Poll every 15-30 seconds
- Paused session: No polling
- Adjust interval based on rate limit warnings
- Use `from` timestamp to only fetch new scrobbles since last poll

### 9.3 Error Handling

**Common API Errors:**

| Error Code | Description | Handling Strategy |
|------------|-------------|------------------|
| 8 | Operation failed | Retry with exponential backoff |
| 9 | Invalid session key | Prompt user to re-authenticate |
| 10 | Invalid API key | Critical error, check configuration |
| 11 | Service offline | Graceful degradation, inform user |
| 13 | Invalid method signature | Log error, fix signature generation |
| 16 | Service temporarily unavailable | Retry with backoff |
| 17 | Login required | Re-authenticate user |
| 29 | Rate limit exceeded | Pause polling, increase interval |

**Error Response Strategy:**
- Log all API errors for debugging
- Display user-friendly messages
- Implement automatic retry for transient errors
- Prompt re-authentication for auth errors
- Gracefully pause/stop session on critical errors

### 9.4 API Request Examples

**Example 1: Get Recent Tracks**
```
GET https://ws.audioscrobbler.com/2.0/
?method=user.getRecentTracks
&user=targetusername
&api_key=YOUR_API_KEY
&format=json
&limit=10
&from=1234567890
```

**Example 2: Scrobble Track**
```
POST https://ws.audioscrobbler.com/2.0/
method=track.scrobble
&artist=Radiohead
&track=Creep
&timestamp=1234567890
&album=Pablo Honey
&api_key=YOUR_API_KEY
&sk=SESSION_KEY
&api_sig=GENERATED_SIGNATURE
```

---

## 10. Technical Constraints & Dependencies

### 10.1 External Dependencies

**Last.fm API:**
- Availability: Dependent on Last.fm service uptime
- Rate Limits: 5 requests per second
- Authentication: OAuth 1.0a
- Documentation: https://www.last.fm/api

**Browser Requirements:**
- JavaScript enabled
- LocalStorage support (for session management)
- Fetch API or XMLHttpRequest support
- CSS3 support for responsive design

### 10.2 Technical Stack Assumptions

**Frontend:**
- ClojureScript
- HTML5, CSS3
- Responsive framework (Bootstrap, Tailwind, or custom CSS)
- ClojureScript libraries for state management and UI (Reagent, Re-frame, or similar)

**Backend:**
- Clojure
- Web framework (Ring, Compojure, Reitit, or similar)
- Session management
- HTTPS support

**Hosting:**
- Web server with HTTPS
- JVM-compatible hosting environment
- Environment variable support for API keys
- Sufficient bandwidth for API polling

**Build Tools:**
- Leiningen or tools.deps for dependency management
- ClojureScript compiler
- Build configuration for production deployment

*(Specific library choices and architecture will be defined in Technical Design Document)*

---

## 11. Acceptance Criteria

### 11.1 Feature Acceptance Criteria

**AC-AUTH: Authentication**

**AC-AUTH-001: Last.fm OAuth Login**
- Given I am an unauthenticated user
- When I click "Login with Last.fm"
- Then I am redirected to Last.fm authorization page
- And I can authorize the application
- And I am redirected back to Lasso
- And I see my Last.fm username displayed
- And I am in an authenticated state

**AC-AUTH-002: Session Persistence**
- Given I am authenticated
- When I refresh the page
- Then I remain authenticated
- And my session state is preserved

**AC-AUTH-003: Logout**
- Given I am authenticated
- When I click logout
- Then my session is cleared
- And I am returned to the login page
- And I cannot access authenticated features

**AC-TARGET: Target User Management**

**AC-TARGET-001: Valid Username Entry**
- Given I am authenticated
- When I enter a valid, public Last.fm username
- Then the system validates the username
- And I see confirmation that the user is valid
- And I can proceed to start following

**AC-TARGET-002: Invalid Username Handling**
- Given I am authenticated
- When I enter an invalid Last.fm username
- Then I see an error message indicating the username doesn't exist
- And I cannot start following

**AC-TARGET-003: Private Profile Handling**
- Given I am authenticated
- When I enter a username with a private profile
- Then I see an error message indicating the profile is not accessible
- And I cannot start following

**AC-FOLLOW: Following Session**

**AC-FOLLOW-001: Start Following**
- Given I am authenticated
- And I have entered a valid target username
- When I click "Start Following"
- Then the session state changes to "Active"
- And the system begins polling the target user's recent tracks
- And I see a visual indicator that following is active

**AC-FOLLOW-002: Scrobble Mirroring**
- Given I have an active following session
- When the target user scrobbles a new track
- Then the track is scrobbled to my account within 60 seconds
- And the scrobble includes correct metadata (artist, track, album, timestamp)
- And I see the scrobble in the activity feed

**AC-FOLLOW-003: Duplicate Prevention**
- Given I have an active following session
- When the same track appears multiple times in polling results
- Then only the first occurrence is scrobbled to my account
- And duplicate scrobbles are prevented

**AC-FOLLOW-004: Pause Following**
- Given I have an active following session
- When I click "Pause"
- Then the session state changes to "Paused"
- And polling stops
- And no new scrobbles are mirrored
- And I see a visual indicator that following is paused

**AC-FOLLOW-005: Resume Following**
- Given I have a paused following session
- When I click "Resume"
- Then the session state changes to "Active"
- And polling resumes
- And new scrobbles are mirrored again

**AC-FOLLOW-006: Stop Following**
- Given I have an active or paused following session
- When I click "Stop"
- Then the session state changes to "Stopped"
- And polling stops
- And the session cache is cleared
- And I can start a new session

**AC-ERROR: Error Handling**

**AC-ERROR-001: API Rate Limit**
- Given the system encounters a rate limit error
- When polling or scrobbling
- Then the system automatically adjusts polling interval
- And I see a notification about the rate limit
- And the session continues when rate limit clears

**AC-ERROR-002: Network Failure**
- Given the system encounters a network failure
- When polling or scrobbling
- Then the system retries with exponential backoff
- And I see a notification about the connection issue
- And the session recovers when network is restored

**AC-ERROR-003: Token Expiration**
- Given my OAuth token expires
- When the system attempts an authenticated request
- Then I see a message that re-authentication is required
- And I can re-authenticate without losing my session configuration

**AC-UI: User Interface**

**AC-UI-001: Mobile Responsiveness**
- Given I access Lasso on a mobile device
- When I view any page
- Then the interface is readable and usable
- And all controls are touch-friendly
- And the layout adapts to screen size

**AC-UI-002: Visual Feedback**
- Given I perform any user action
- When the action is processing
- Then I see a loading indicator
- And when the action completes
- Then I see success or error feedback

**AC-UI-003: Session State Clarity**
- Given I am using the application
- When I view the main page
- Then I can clearly see the current session state
- And I can see which user I'm following (if applicable)
- And I can see appropriate action buttons for my current state

---

## 12. Testing Requirements

### 12.1 Testing Strategy

**Testing Levels:**
1. Unit Testing: Test individual functions and components
2. Integration Testing: Test API integration and component interaction
3. End-to-End Testing: Test complete user workflows
4. Manual Testing: Test UI/UX and edge cases

**Testing Approach:**
- Test-driven development where practical
- Automated tests for core business logic
- Manual testing for UI/UX
- Real-world testing with actual Last.fm accounts

### 12.2 Unit Testing Requirements

**UT-001: Authentication Module**
- Test OAuth token exchange logic
- Test session creation and management
- Test token encryption/decryption
- Test logout functionality

**UT-002: Target User Validation**
- Test username format validation
- Test profile accessibility checking
- Test error handling for invalid users

**UT-003: Scrobble Processing**
- Test new scrobble detection logic
- Test duplicate prevention algorithm
- Test metadata extraction and formatting
- Test timestamp handling

**UT-004: Session State Management**
- Test state transitions (Not Started → Active → Paused → Active → Stopped)
- Test invalid state transition prevention
- Test session data persistence

**UT-005: API Integration Layer**
- Test API request formation
- Test method signature generation
- Test response parsing
- Test error handling for each API method

### 12.3 Integration Testing Requirements

**IT-001: Last.fm API Integration**
- Test complete OAuth flow with Last.fm
- Test user.getRecentTracks API call
- Test track.scrobble API call
- Test rate limit handling
- Test error response handling

**IT-002: End-to-End Scrobble Flow**
- Test complete flow: fetch target tracks → identify new → scrobble to user
- Test session state changes during scrobbling
- Test cache updates
- Test concurrent polling and scrobbling

**IT-003: Session Management**
- Test session creation and persistence
- Test session expiration
- Test session cleanup on logout

### 12.4 End-to-End Testing Requirements

**E2E-001: Complete User Journey - Happy Path**
1. User logs in with Last.fm
2. User enters valid target username
3. User starts following
4. Target user scrobbles a track
5. Track appears in user's Last.fm account
6. Track appears in activity feed
7. User stops following
8. User logs out

**E2E-002: Pause and Resume Flow**
1. User starts following session
2. User pauses session
3. Target user scrobbles during pause (should not be mirrored)
4. User resumes session
5. New target scrobbles are mirrored again

**E2E-003: Error Recovery Flow**
1. User starts following session
2. Network failure occurs
3. System shows error notification
4. Network recovers
5. System automatically resumes
6. Scrobbling continues

**E2E-004: Mobile User Journey**
1. User accesses on mobile device
2. User completes full workflow on mobile
3. All interactions work with touch
4. Layout is readable and functional

### 12.5 Manual Testing Requirements

**MT-001: UI/UX Testing**
- Visual design consistency
- Button and control usability
- Error message clarity
- Loading states and transitions
- Mobile responsiveness across devices
- Browser compatibility (Chrome, Firefox, Safari, Edge)

**MT-002: Edge Case Testing**
- Target user has no recent scrobbles
- Target user scrobbles very rapidly (multiple per second)
- User leaves browser tab inactive for extended period
- Multiple quick state changes (start/pause/resume/stop)
- Very long session duration
- Large number of scrobbles in single session

**MT-003: Security Testing**
- OAuth flow security
- Token storage security
- Session isolation between users
- HTTPS enforcement
- API key protection

### 12.6 Test Data Requirements

**Test Accounts:**
- Primary test Last.fm account (authenticated user)
- Secondary test Last.fm account (target user)
- Account with private profile (for error testing)

**Test Scenarios:**
- Target user with recent scrobbles
- Target user with no recent scrobbles
- Target user scrobbling in real-time during test
- Variety of track metadata (with/without album, special characters, etc.)

### 12.7 Success Criteria for Testing

**Test Coverage Goals:**
- Unit test coverage: >80% for core business logic
- All critical user paths tested end-to-end
- All acceptance criteria validated
- Zero critical bugs at launch
- All "Must Have" requirements verified

**Definition of Done for Testing:**
- All unit tests passing
- All integration tests passing
- All E2E critical paths tested successfully
- Manual testing completed on target browsers/devices
- Security review completed
- Performance requirements verified
- No P0 or P1 bugs remaining

---

## 13. Deployment and Release Planning

### 13.1 Deployment Strategy

**Deployment Model:**
- Single production environment
- Optional staging/development environment for pre-release testing
- Continuous deployment approach (deploy when ready)
- No formal release schedule (deploy features as completed)

**Deployment Approach:**
- Manual deployment initially
- Potential automation as project matures
- Database-less architecture simplifies deployment
- Session-based state (no data migration required)

### 13.2 Environment Requirements

**Development Environment:**
- Local development machine
- Local Clojure/ClojureScript development setup
- Test Last.fm API key and secret
- Test OAuth callback URL (localhost)

**Staging Environment (Optional):**
- Public URL for testing OAuth flow
- Staging Last.fm API credentials
- Same infrastructure as production
- Used for pre-release validation

**Production Environment:**
- Public web server with HTTPS
- Domain name or subdomain
- JVM-compatible hosting (e.g., Heroku, DigitalOcean, AWS, etc.)
- Environment variables for API keys
- SSL certificate
- Production Last.fm API credentials

### 13.3 Infrastructure Requirements

**Hosting Requirements:**
- JVM support (Java 8+)
- HTTPS/SSL support
- Custom domain support
- Environment variable configuration
- Adequate bandwidth for API polling
- Uptime monitoring capabilities

**Recommended Hosting Options:**
- Heroku (free/hobby tier for MVP)
- DigitalOcean App Platform
- AWS Elastic Beanstalk
- Google Cloud Run
- Railway
- Render

**Cost Considerations:**
- Target: Free tier or <$10/month
- Minimize infrastructure costs
- Scale only if user demand justifies

### 13.4 Configuration Management

**Environment Variables:**
```
LASTFM_API_KEY=<api_key>
LASTFM_API_SECRET=<api_secret>
OAUTH_CALLBACK_URL=<callback_url>
SESSION_SECRET=<random_secret>
PORT=<port_number>
ENVIRONMENT=<dev|staging|production>
```

**Configuration Strategy:**
- No secrets in code or version control
- Environment-specific configuration files
- Separate API keys for dev/staging/production
- Document all required environment variables

### 13.5 Release Criteria

**Pre-Launch Checklist:**

**Functional Completeness:**
- [ ] All "Must Have" features implemented
- [ ] All acceptance criteria met
- [ ] OAuth flow working end-to-end
- [ ] Scrobble mirroring functioning accurately
- [ ] Session management working correctly

**Quality Assurance:**
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] E2E testing completed successfully
- [ ] Manual testing completed on all target browsers
- [ ] Mobile testing completed on iOS and Android
- [ ] No P0 or P1 bugs remaining

**Security & Compliance:**
- [ ] OAuth implementation secure
- [ ] HTTPS enforced
- [ ] API keys protected
- [ ] Last.fm Terms of Service reviewed and complied with
- [ ] Privacy considerations addressed

**Documentation:**
- [ ] User-facing help text complete
- [ ] README with setup instructions
- [ ] API integration documented
- [ ] Deployment guide created
- [ ] Known issues/limitations documented

**Infrastructure:**
- [ ] Production environment configured
- [ ] Domain/subdomain configured
- [ ] SSL certificate installed
- [ ] Environment variables set
- [ ] Monitoring configured

### 13.6 Rollout Plan

**Phase 1: Soft Launch (Sprint 9)**
- Deploy to production
- Initial testing by product owner (self)
- Verify all functionality in production environment
- Monitor for errors and performance issues
- Duration: 1-2 weeks

**Phase 2: Limited Release**
- Share with close friends/trusted users
- Gather initial feedback
- Monitor usage patterns
- Fix any critical issues discovered
- Duration: 2-4 weeks

**Phase 3: Public Release**
- Announce on relevant communities (if desired)
- Monitor for scaling issues
- Respond to user feedback
- Iterate based on real-world usage

**Rollback Plan:**
- Keep previous version available
- Document rollback procedure
- Ability to quickly disable application if critical issues arise
- Communication plan for users if downtime required

### 13.7 Monitoring and Maintenance

**Monitoring Requirements:**

**Application Health:**
- Uptime monitoring (UptimeRobot, Pingdom, or similar)
- Error logging and alerting
- API request success/failure rates
- Session creation/termination tracking

**Performance Monitoring:**
- API response times
- Scrobble latency (time from target scrobble to mirror)
- Polling interval effectiveness
- Rate limit hit frequency

**User Metrics:**
- Number of active sessions
- Number of scrobbles mirrored
- Average session duration
- Error frequency by type

**Monitoring Tools:**
- Application logs (stdout/stderr)
- External uptime monitoring service
- Error tracking (Sentry, Rollbar, or similar - optional)
- Simple analytics (optional, privacy-conscious)

**Maintenance Plan:**

**Regular Maintenance:**
- Monitor Last.fm API for changes or deprecations
- Review error logs weekly
- Update dependencies periodically
- Renew SSL certificates as needed
- Monitor hosting costs

**Incident Response:**
- Check error notifications daily
- Respond to critical issues within 24 hours
- Document all incidents and resolutions
- Update documentation based on learnings

**Updates and Enhancements:**
- Bug fixes deployed as needed
- New features based on feedback and "Should Have" list
- Performance optimizations as usage patterns emerge

### 13.8 Backup and Recovery

**Data Backup:**
- No persistent user data to backup
- Session data is ephemeral
- Configuration backed up in version control

**Disaster Recovery:**
- Code and configuration in version control (Git)
- Infrastructure as code documentation
- Ability to redeploy from scratch within hours
- Document recovery procedures

**Service Continuity:**
- Last.fm API outage: Graceful degradation, inform users
- Hosting provider outage: Wait for restoration or migrate to backup provider
- Critical bug: Roll back to previous version

---

## 14. Success Metrics and KPIs

### 14.1 Launch Success Metrics

**Immediate Success (First 30 Days):**
- Application successfully deployed and accessible
- Zero data loss incidents
- >95% scrobble accuracy rate
- <3 critical bugs reported
- Successful completion of at least 10 user sessions (self-testing)

**User Experience Metrics:**
- Average time to complete workflow: <2 minutes
- Mobile usability: Successfully usable on iOS and Android
- Error rate: <5% of user actions result in errors

### 14.2 Ongoing Success Metrics

**Quality Metrics:**
- Scrobble accuracy: >95%
- Uptime: >95%
- API rate limit compliance: 100%
- Zero data corruption incidents

**Usage Metrics (Optional):**
- Number of active sessions per week
- Average session duration
- Average scrobbles per session
- Repeat user rate

**Technical Metrics:**
- Average API response time: <2 seconds
- Average scrobble latency: <60 seconds
- Error rate by type
- Rate of successful vs. failed scrobbles

### 14.3 Post-Launch Evaluation Criteria

**Success Indicators:**
- Product Owner can reliably use the application for Spotify Jams
- Scrobbles are accurately mirrored without manual intervention
- No significant security or privacy issues discovered
- Application is maintainable with minimal time investment

**Iteration Triggers:**
- User feedback indicates missing features
- Performance issues emerge with real usage
- Last.fm API changes require updates
- Security vulnerabilities discovered

---

## 15. Dependencies and Risks (PRD Level)

### 15.1 External Dependencies

**Last.fm API:**
- Dependency: Last.fm service availability
- Risk: API changes, deprecations, or downtime
- Mitigation: Monitor Last.fm developer communications, build modular API layer

**OAuth Provider:**
- Dependency: Last.fm OAuth service
- Risk: Changes to OAuth implementation
- Mitigation: Follow OAuth best practices, test regularly

**Hosting Provider:**
- Dependency: Chosen hosting service availability
- Risk: Service outages, pricing changes
- Mitigation: Document deployment process for easy migration

### 15.2 Internal Dependencies

**Development Timeline:**
- Dependency: Available development time
- Risk: Delays due to complexity or personal time constraints
- Mitigation: Maintain clear milestones, break into small increments

**Technical Expertise:**
- Dependency: ClojureScript/Clojure proficiency
- Risk: Learning curve impacts timeline
- Mitigation: Allow extra time for learning, leverage community resources

**Testing:**
- Dependency: Access to test Last.fm accounts
- Risk: Limited testing scenarios
- Mitigation: Create multiple test accounts, test with real-world scenarios

### 15.3 Assumptions and Constraints

**Key Assumptions:**
- Last.fm API will remain accessible for this use case
- Users understand the manual nature of the solution
- Polling-based approach is acceptable for user experience
- Free/low-cost hosting is sufficient for expected usage

**Key Constraints:**
- Single developer/product owner
- Limited budget for infrastructure
- No native mobile app (web-only)
- Dependent on external API (Last.fm)

---

## 16. Future Enhancements (Out of Scope for V1)

### 16.1 Potential Future Features

**Phase 2 Enhancements:**
- Session history showing past following sessions
- Multiple concurrent target users (follow several people at once)
- Automatic Spotify Jam detection (if API becomes available)
- User preferences and settings persistence
- Browser extension for easier access

**Advanced Features:**
- Social features (sharing sessions, recommendations)
- Analytics dashboard (listening patterns, statistics)
- Conflict resolution (if user and target both scrobble same track)
- Historical backfilling (scrobble past tracks from target user)
- Email notifications for session events

**Technical Enhancements:**
- WebSocket-based real-time updates (instead of polling)
- Progressive Web App (PWA) capabilities
- Offline mode support
- Advanced caching strategies
- Performance optimizations

### 16.2 Enhancement Prioritization

Enhancements will be prioritized based on:
1. User feedback and demand
2. Technical feasibility
3. Development time required
4. Value vs. complexity ratio
5. Alignment with core product vision

---

## 17. Glossary

**Last.fm:** A music service that tracks listening habits and provides music recommendations.

**Scrobble:** A record of a song that a user has listened to, tracked by Last.fm. The act of recording this data is called "scrobbling."

**Spotify Jam:** A collaborative listening feature in Spotify that allows multiple users to control and contribute to a shared queue.

**OAuth:** An open standard for access delegation, allowing users to grant applications access to their accounts without sharing passwords.

**Session:** In the context of Lasso, a period during which a user is actively following another user's scrobbles.

**Target User:** The Last.fm user whose scrobbles are being followed and mirrored.

**Authenticated User:** The user logged into Lasso who is receiving the mirrored scrobbles.

**Polling:** The process of repeatedly checking for new data at regular intervals.

**Rate Limit:** A restriction on the number of API requests that can be made within a specific time period.

**API Key:** A unique identifier used to authenticate requests to the Last.fm API.

**Session Key:** An authentication token received after OAuth authorization, used for authenticated API requests.

---

## 18. Approval and Sign-off

**Product Requirements Document Approved By:**

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Product Owner | [Your Name] | [Date] | _________ |
| Developer | [Your Name] | [Date] | _________ |

**Document Revision History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | February 3, 2026 | Product Owner | Initial PRD creation |

---

**End of Product Requirements Document**
