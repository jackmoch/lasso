# Task Implementation Skill

## When to Use
Use this skill when asked to:
- "Implement TASK-X.Y"
- "Work on [task/ticket]"
- "Complete this task"
- Following a sprint implementation plan

## Implementation Workflow

```
Read Ticket
    ↓
Verify Dependencies
    ↓
Write Code
    ↓
Write Tests
    ↓
Write Documentation
    ↓
Create Examples
    ↓
Update CHANGELOG
    ↓
Verify Acceptance Criteria
    ↓
Commit
    ↓
Update Task Tracking
```

## Phase 1: Read & Verify

### 1.1 Extract from Ticket
- Task ID and title
- All acceptance criteria
- Files to create/modify
- Dependencies
- Definition of Done

### 1.2 Check Dependencies
```bash
# Verify prerequisite tasks are complete
# Check task tracking file shows them as ✅

# Verify required files exist
ls -la path/to/required/file.ext

# If dependencies not met: STOP and report
```

### 1.3 Mark Task as Started
Update tracking file:
```markdown
## TASK-X.Y: [Title]
**Status:** 🚧 In Progress
**Started:** [timestamp]
**Assigned:** Claude
```

## Phase 2: Write Code

### 2.1 Follow Project Conventions

**Clojure/ClojureScript:**
- Use `kebab-case` for function names
- Use `?` suffix for predicates: `valid?`
- Use `!` suffix for state mutation: `reset!`
- Prefer pure functions
- Keep functions small (<20 lines)
- Use descriptive names

**General:**
- DRY (Don't Repeat Yourself)
- KISS (Keep It Simple)
- Single Responsibility
- Consistent with existing codebase patterns

### 2.2 Code Structure Template

```clojure
(ns project.component.module
  "Brief description of this namespace.
   
   Key functions:
   - main-fn: Primary functionality
   - helper-fn: Supporting functionality"
  (:require [dep.one :as d1]
            [dep.two :as d2]))

;; ===== Private Helpers =====

(defn- internal-helper
  "Helper function docs."
  [arg]
  (implementation))

;; ===== Public API =====

(defn public-function
  "What this function does.
   
   Args:
     arg1: Type and description
     arg2: Type and description
     
   Returns:
     Type and description
     
   Example:
     (public-function \"test\" 42)
     ;; => {:result \"success\"}"
  [arg1 arg2]
  (implementation))
```

### 2.3 Error Handling

Always handle errors:
```clojure
(defn risky-operation
  "Operation that might fail."
  [input]
  (try
    (let [result (external-call input)]
      {:success true :data result})
    (catch Exception e
      (log/error e "Operation failed" {:input input})
      {:success false :error (.getMessage e)})))
```

### 2.4 REPL-Driven Development

Test as you write:
```clojure
;; In REPL:
user=> (require '[project.module :as m] :reload)
user=> (m/new-function test-input)
;; => verify output

;; Iterate until correct
user=> (m/new-function edge-case)
;; => check edge cases work
```

## Phase 3: Write Tests

### 3.1 Test Coverage Requirements
- **Minimum:** 80% of business logic
- **Happy path:** Expected inputs → expected outputs
- **Edge cases:** Empty, nil, boundaries
- **Error cases:** Invalid inputs, failures
- **Integration:** API calls, external systems

### 3.2 Test Structure

```clojure
(ns project.module-test
  (:require [clojure.test :refer :all]
            [project.module :as m]))

(deftest function-name-test
  (testing "happy path description"
    (let [result (m/function-name valid-input)]
      (is (= expected result))
      (is (contains? result :key))))
  
  (testing "handles error case"
    (is (thrown? Exception (m/function-name invalid-input))))
  
  (testing "edge case: empty input"
    (is (= default-value (m/function-name "")))))
```

### 3.3 Run Tests

```bash
# All tests
clj -M:test

# Specific namespace
clj -M:test --focus project.module-test

# Watch mode (if configured)
clj -M:test --watch

# Tests MUST pass before proceeding
```

### 3.4 Test Edge Cases

Common edge cases:
- Empty strings: `""`
- Nil values: `nil`
- Empty collections: `[]`, `{}`
- Boundaries: `0`, `-1`, `MAX_VALUE`
- Unicode/special characters
- Concurrent access (if applicable)
- Network failures (for APIs)

## Phase 4: Write Documentation

### 4.1 Code Documentation

**Docstrings (Required for Public Functions):**
```clojure
(defn process-items
  "Processes items according to specified rules.
   
   Takes a collection of items and applies transformation rules,
   returning a summary of processing results.
   
   Args:
     items: Collection of maps with required keys [:id :value]
     rules: Map of transformation rules {:transform-fn fn, :filter-fn fn}
     
   Returns:
     Map with keys:
       :processed - Count of successfully processed items
       :failed - Vector of {:id item-id :error error-msg}
       :skipped - Count of items skipped by filter
       
   Example:
     (process-items [{:id 1 :value 10}]
                   {:transform-fn inc :filter-fn pos?})
     ;; => {:processed 1 :failed [] :skipped 0}
     
   Throws:
     IllegalArgumentException if items is not a collection"
  [items rules]
  ...)
```

### 4.2 Update README (if applicable)

If changes affect:
- **User features:** Update main README
- **API endpoints:** Update API section
- **Configuration:** Update config section
- **Setup:** Update installation/setup

Example addition:
```markdown
### Authentication

POST /api/auth/init - Initiate OAuth flow
GET /api/auth/callback - OAuth callback

See [Authentication Guide](docs/authentication.md) for details.
```

### 4.3 Create Separate Documentation (for complex features)

For major features, create dedicated docs:
```markdown
docs/authentication.md
docs/api-endpoints.md
docs/configuration.md
```

## Phase 5: Create Examples

### 5.1 Code Examples

Create `examples/` directory with usage examples:

```clojure
;; examples/auth-example.clj

(ns examples.auth
  (:require [project.auth :as auth]))

;; Example 1: Complete authentication flow
(comment
  ;; Generate auth URL
  (def url (auth/init-oauth))
  ;; => "https://api.example.com/auth?..."
  
  ;; User authorizes, returns with token
  (def token "abc123")
  
  ;; Complete authentication
  (def session (auth/complete-oauth token))
  ;; => {:success true :session-key "xyz" :user "john"}
  )

;; Example 2: Error handling
(comment
  ;; Invalid token
  (auth/complete-oauth "invalid")
  ;; => {:success false :error "Invalid token"}
  )
```

### 5.2 Test Examples as Documentation

Tests also serve as examples:
```clojure
(deftest example-usage-test
  (testing "demonstrates typical usage"
    ;; This test shows how to use the API
    (let [input {:user "test" :action "login"}
          result (process-request input)]
      (is (:success result))
      (is (= "test" (:user result))))))
```

### 5.3 README Examples

For user-facing features, add examples to README:
```markdown
## Example Usage

```clojure
;; Start server
(require '[project.server :as server])
(server/start {:port 8080})

;; Access at http://localhost:8080
```
```

## Phase 6: Update CHANGELOG

### 6.1 When to Update CHANGELOG
Update for:
- ✅ New features (user-facing)
- ✅ Breaking changes
- ✅ Bug fixes
- ✅ API changes
- ❌ Internal refactoring
- ❌ Test-only changes
- ❌ Documentation-only changes

### 6.2 CHANGELOG Format

```markdown
# Changelog

## [Unreleased]

### Added
- OAuth authentication with Last.fm (TASK-3.1, TASK-3.2)
- Session management with 24h expiration (TASK-3.3)
- Login and logout endpoints (TASK-3.4)

### Changed
- API endpoint `/auth` renamed to `/auth/init` for clarity

### Fixed
- Handle expired OAuth tokens gracefully

### Security
- Add CSRF protection to OAuth callback
```

## Phase 7: Verify Acceptance Criteria

### 7.1 Check Each Criterion

Go through ticket's acceptance criteria:
```markdown
Acceptance Criteria:
✅ POST /api/auth/init endpoint exists
✅ Endpoint calls Last.fm auth.getToken API
✅ Returns valid OAuth URL
✅ Handles errors gracefully
✅ Has unit tests with >80% coverage
```

### 7.2 Run Verification Steps

Execute from ticket:
```bash
# 1. Start server
clj -M:dev:repl
# user=> (start)

# 2. Test endpoint
curl -X POST http://localhost:8080/api/auth/init
# Verify response

# 3. Run all tests
clj -M:test
# All must pass
```

### 7.3 Definition of Done Checklist

```markdown
- [ ] Code written following style guide
- [ ] Unit tests written and passing (>80% coverage)
- [ ] Integration tests (if applicable)
- [ ] Documentation updated (docstrings, README)
- [ ] Examples provided
- [ ] CHANGELOG.md updated (if user-facing)
- [ ] No compiler warnings
- [ ] No linter errors
- [ ] All acceptance criteria met
```

## Phase 8: Commit

### 8.1 Review Changes

```bash
git status
git diff
```

### 8.2 Stage Files

```bash
git add src/clj/project/module.clj
git add test/clj/project/module_test.clj
git add README.md
git add CHANGELOG.md
git add examples/module-example.clj
```

### 8.3 Write Commit Message

Format:
```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

Example:
```bash
git commit -m "feat(auth): implement OAuth initialization endpoint

- Add POST /api/auth/init endpoint
- Implement Last.fm auth.getToken integration  
- Add comprehensive unit tests (85% coverage)
- Add error handling for API failures
- Update README with auth documentation
- Add usage examples

Closes TASK-3.1"
```

### 8.4 Verify Commit

```bash
git log -1 --stat
# Verify files and message
```

## Phase 9: Update Task Tracking

### 9.1 Mark Task Complete

Update `progress/sprint-X-progress.md`:

```markdown
## TASK-3.1: OAuth Initialization Endpoint
**Status:** ✅ Complete
**Started:** 2026-02-04 14:00
**Completed:** 2026-02-04 16:30
**Time:** 2.5h (est: 2h)
**Commit:** abc123def456
**Coverage:** 85%
**Notes:** Added retry logic for API resilience
**Unblocks:** TASK-3.2, TASK-3.3
```

### 9.2 Update Summary Stats

```markdown
## Summary
- Tasks Complete: 8 / 15 (53%)
- Time Spent: 18.5h / 30h estimated
```

### 9.3 Update Session Log

```markdown
## Completed This Session
1. ✅ TASK-3.1 - OAuth Init (2.5h)
```

### 9.4 Identify Next Task

```markdown
## Next Up
1. **Immediate:** TASK-3.2 - OAuth Callback Handler
   - Dependencies: ✅ All met (3.1 complete)
   - Priority: P0
   - Estimated: 3h
```

### 9.5 Commit Tracking Update

```bash
git add progress/sprint-X-progress.md
git commit -m "docs: mark TASK-3.1 complete"
```

## Quality Checklist

Before marking task complete:
- [ ] All acceptance criteria met
- [ ] All tests passing
- [ ] Code has docstrings
- [ ] README updated (if needed)
- [ ] Examples created (if complex)
- [ ] CHANGELOG updated (if user-facing)
- [ ] No TODOs or FIXMEs in code
- [ ] No debug print statements
- [ ] No hardcoded values
- [ ] Error messages are clear
- [ ] Task tracking updated
- [ ] Changes committed

## Common Patterns

### Pattern: API Endpoint Implementation
1. Create handler function
2. Add route definition
3. Write request/response tests
4. Document in API docs
5. Add example API call

### Pattern: Data Transformation
1. Implement pure function
2. Test with varied inputs
3. Test edge cases
4. Document input/output format
5. Provide examples

### Pattern: Integration with External API
1. Create API client namespace
2. Implement API calls
3. Add error handling
4. Mock in tests
5. Document rate limits/quirks

## Output Format

### Progress Updates
```
📝 Implementing TASK-3.1: OAuth Initialization

✅ Created auth/core.clj namespace
✅ Implemented generate-auth-url function
✅ Added route handler
✅ Wrote 8 unit tests (85% coverage)
✅ Updated README
✅ Added examples
⏳ Updating CHANGELOG...
```

### Completion Report
```
✅ TASK-3.1 COMPLETE: OAuth Initialization Endpoint

Files Modified:
- src/clj/project/auth/core.clj (new)
- src/clj/project/routes.clj
- test/clj/project/auth/core_test.clj (new)
- README.md
- CHANGELOG.md
- examples/auth-example.clj (new)

Tests: 8 new tests, 85% coverage, all passing ✅
Acceptance Criteria: 5/5 met ✅

Commit: abc123def456
Time: 2.5h (estimated: 2h)

Next: TASK-3.2 - OAuth Callback Handler
```

## Anti-Patterns to Avoid

❌ Skip tests
❌ Skip documentation
❌ Forget to update CHANGELOG
❌ Don't verify acceptance criteria
❌ Don't update task tracking
❌ Commit without testing
❌ Leave TODOs in code
❌ Skip examples for complex features

✅ Complete all phases
✅ Test thoroughly
✅ Document clearly
✅ Update all artifacts
✅ Verify before committing
