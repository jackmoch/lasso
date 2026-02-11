# Ticket Generation Skill

## When to Use
Use this skill when asked to:
- "Generate tickets from the PRD"
- "Create tasks from the TDD"
- "Break down [feature/epic] into implementable tasks"
- "Create a sprint backlog"

## Before You Start
Always read:
1. The Product Requirements Document (PRD)
2. The Technical Design Document (TDD)
3. Any existing task tracking files

## Ticket Structure

Each ticket MUST include:

```markdown
# TASK-X.Y: [Short descriptive title]

## Type
[Feature | Bug | Chore | Documentation]

## Priority
[P0-Critical | P1-High | P2-Medium | P3-Low]

## Estimated Effort
[Hours: 1-8h per task, break larger tasks down]

## Dependencies
**Prerequisites:** [TASK-IDs that must complete first]
**Blocks:** [TASK-IDs that depend on this]

## Description
[2-3 sentences: what needs to be done and why]

## Acceptance Criteria
- [ ] [Specific, testable criterion]
- [ ] [Specific, testable criterion]
- [ ] [Specific, testable criterion]

## Implementation Details

### Files to Create/Modify
- `path/to/file.ext` - [Purpose]

### Key Components
- Function/component name - [What it does]

### Technical Notes
- [Frameworks/patterns to use]
- [API endpoints if applicable]
- [Data structures if applicable]

## Verification Steps
1. [Concrete step to verify completion]
2. [Concrete step to verify completion]

## Definition of Done
- [ ] Code written following style guide
- [ ] Unit tests written and passing (>80% coverage)
- [ ] Integration tests if applicable
- [ ] Documentation updated
- [ ] Examples provided
- [ ] CHANGELOG.md updated if user-facing
- [ ] Task marked complete in tracking file
```

## Decomposition Rules

### Epic → User Stories → Tasks

**Epic:** Large feature area (e.g., "Authentication System")
- Contains 2-5 user stories
- Maps to PRD epics

**User Story:** User-facing capability (e.g., "User can log in via OAuth")
- Contains 2-8 implementation tasks
- Maps to PRD user stories

**Task:** Single implementable unit (1-8 hours)
- One task = one pull request
- Must be independently testable

### Task Sizing
- **Ideal:** 2-4 hours
- **Maximum:** 8 hours (1 day)
- **If larger:** Split into sub-tasks
- **If smaller (< 1hr):** Combine with related work

## Mapping PRD/TDD to Tasks

### From PRD, Extract:
- Functional requirements (FR-*)
- User stories (US-*)
- Acceptance criteria
- Priority levels

### From TDD, Extract:
- Component architecture
- File/namespace structure
- API specifications
- Data models
- Technology stack

### Create Mapping:
```
User Story US-1.1 "Login with Last.fm"
  ↓
Functional Req FR-AUTH-001 "OAuth integration"
  ↓
Implementation Tasks:
  - TASK-3.1: OAuth initialization endpoint (Backend)
  - TASK-3.2: OAuth callback handler (Backend)
  - TASK-3.3: Session management (Backend)
  - TASK-5.1: Login button component (Frontend)
  - TASK-5.2: Auth state management (Frontend)
```

## Dependency Management

### Rules:
1. Infrastructure tasks before feature tasks
2. Backend before frontend (usually)
3. Data models before business logic
4. Core functions before integrations

### Example Dependency Chain:
```
TASK-2.4: Project structure
  └─→ TASK-2.5: Dependencies configured
       └─→ TASK-3.1: Auth namespace created
            └─→ TASK-3.2: OAuth endpoints
                 └─→ TASK-5.1: Frontend login
```

## Priority Assignment

**P0 (Critical):**
- Blocks all other work
- Infrastructure/setup
- Core authentication/security
- Deployment blockers

**P1 (High):**
- MVP features
- User-facing functionality
- Integration points

**P2 (Medium):**
- Nice-to-have features
- Optimizations
- Error handling enhancements

**P3 (Low):**
- Future enhancements
- Polish
- Non-critical improvements

## Output Format

### 1. Summary
```markdown
## Generated Tickets for [Feature/Sprint]

**Total:** X tasks, ~Y hours

### By Epic:
- Epic 1: Authentication (5 tasks, 12h)
- Epic 2: Scrobbling (6 tasks, 16h)
- Epic 3: UI (4 tasks, 8h)

### By Priority:
- P0: 3 tasks
- P1: 8 tasks
- P2: 4 tasks
```

### 2. Dependency Graph
```markdown
## Task Dependencies

Setup Tasks (No dependencies):
- TASK-2.1: Install tools
- TASK-2.2: Configure APIs

Backend Core (Depends on setup):
- TASK-3.1: Auth init (→ blocks 3.2, 5.1)
- TASK-3.2: Auth callback (→ blocks 3.3, 5.2)
- TASK-4.1: Scrobble polling (→ blocks 4.2)

Frontend (Depends on backend):
- TASK-5.1: Login UI (needs 3.1)
- TASK-5.2: Auth state (needs 3.2)
```

### 3. Sprint Allocation
```markdown
## Recommended Sprints

**Sprint 2: Setup** (6 tasks, 8h)
- TASK-2.1 through TASK-2.6

**Sprint 3: Auth Backend** (5 tasks, 12h)
- TASK-3.1 through TASK-3.5

**Sprint 4: Scrobbling** (6 tasks, 16h)
- TASK-4.1 through TASK-4.6
```

### 4. Full Ticket Details
Provide complete markdown for each ticket.

## Quality Checklist

Before delivering tickets:
- [ ] Every task has clear acceptance criteria
- [ ] All dependencies explicitly stated
- [ ] Each task is 1-8 hours
- [ ] Priority levels assigned correctly
- [ ] File paths reference TDD structure
- [ ] Technical details are specific (not vague)
- [ ] Each task is independently verifiable
- [ ] Definition of Done is complete

## Example Session

**Input:**
```
Generate implementation tickets for Sprint 3 (Authentication backend) 
based on PRD Epic 1 and TDD Section 5.1.
```

**Process:**
1. Read PRD Epic 1 user stories
2. Read TDD Section 5.1 components
3. Identify 5 core tasks needed
4. Map dependencies
5. Assign priorities
6. Generate full tickets

**Output:**
- Summary (5 tasks, ~12 hours)
- Dependency graph
- 5 complete ticket markdown documents

## Anti-Patterns to Avoid

❌ **Too vague:**
"Implement authentication"

✅ **Specific:**
"Create POST /api/auth/init endpoint that calls Last.fm auth.getToken and returns OAuth URL"

---

❌ **No acceptance criteria:**
"Add tests"

✅ **Measurable:**
"Unit tests with >80% coverage for: token validation, error handling, URL generation"

---

❌ **Missing dependencies:**
"Add login button" (but doesn't note it needs auth endpoint)

✅ **Dependencies noted:**
"Prerequisites: TASK-3.1 (provides auth URL endpoint)"
