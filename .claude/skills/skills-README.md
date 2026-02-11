# Lasso Project Skills

This directory contains Claude Skills for the Lasso project. Skills are instruction sets that codify best practices for specific types of work.

## Directory Structure

```
lasso-skills/
├── README.md (this file)
├── ticket-generation/
│   └── SKILL.md
└── task-implementation/
    └── SKILL.md
```

## Skills Overview

### 1. Ticket Generation (`ticket-generation/SKILL.md`)

**Purpose:** Generate implementation tickets from PRD and TDD documents.

**Use when:**
- "Generate tickets from the PRD"
- "Create tasks for Sprint X"
- "Break down [feature] into implementable tasks"

**What it teaches:**
- How to decompose PRD/TDD into tickets
- Ticket structure and format
- Dependency mapping
- Priority assignment
- Effort estimation

**Output:** Complete ticket markdown files with acceptance criteria, dependencies, and implementation details.

---

### 2. Task Implementation (`task-implementation/SKILL.md`)

**Purpose:** Implement tasks following complete development lifecycle.

**Use when:**
- "Implement TASK-X.Y"
- "Work on [ticket]"
- "Complete this task"

**What it teaches:**
- Code writing best practices
- Test writing (>80% coverage required)
- Documentation (docstrings, README, examples)
- CHANGELOG updates
- Verification procedures
- Task tracking updates

**Workflow:**
```
Read Ticket → Write Code → Write Tests → Write Docs → 
Create Examples → Update CHANGELOG → Verify → Commit → Update Tracking
```

## How to Use These Skills

### For Human Users

1. **Upload to Claude Project:**
   - Upload these skill files to your Claude Project
   - Claude will read them before performing relevant work

2. **Trigger Skills:**
   ```
   Human: "Generate tickets for Sprint 3 authentication epic"
   Claude: [Reads ticket-generation/SKILL.md]
   Claude: [Generates tickets following the skill]
   ```

3. **Reference in Prompts:**
   ```
   Human: "Implement TASK-3.1 following the task-implementation skill"
   Claude: [Reads task-implementation/SKILL.md]
   Claude: [Implements with code, tests, docs, examples, changelog, tracking]
   ```

### For AI Agents (Claude)

When you see these phrases, read the corresponding skill:

**Ticket Generation:**
- "generate tickets"
- "create tasks"
- "break down into tasks"
- "sprint backlog"

→ Read `/path/to/ticket-generation/SKILL.md`

**Task Implementation:**
- "implement TASK-"
- "work on ticket"
- "complete this task"

→ Read `/path/to/task-implementation/SKILL.md`

## Integration with Project Documents

These skills work with:

1. **Project Charter** - Defines sprints and milestones
2. **PRD** - Source of user stories and requirements
3. **TDD** - Source of technical specifications
4. **Sprint Plans** - Task lists to implement
5. **Progress Tracking** - Files to update after completion

## Workflow Example

### Sprint 3 Implementation

**Step 1: Generate Tickets**
```
Human: "Generate tickets for Sprint 3 based on PRD Epic 1 and TDD Section 5.1"
Claude: [Reads ticket-generation/SKILL.md]
Claude: [Generates TASK-3.1 through TASK-3.5]
```

**Step 2: Implement Tasks**
```
Human: "Implement TASK-3.1"
Claude: [Reads task-implementation/SKILL.md]
Claude: [Writes code]
Claude: [Writes tests]
Claude: [Writes documentation]
Claude: [Creates examples]
Claude: [Updates CHANGELOG]
Claude: [Verifies acceptance criteria]
Claude: [Commits]
Claude: [Updates progress tracking]
Claude: "TASK-3.1 complete ✅. Next: TASK-3.2?"
```

**Step 3: Continue**
```
Human: "Yes, proceed with TASK-3.2"
Claude: [Repeats implementation workflow]
```

## Skill Maintenance

### When to Update Skills

Update skills when:
- Project conventions change
- New best practices emerge
- Tools/frameworks change
- Feedback indicates improvements needed

### How to Update Skills

1. Edit the SKILL.md file
2. Test with example usage
3. Document changes
4. Re-upload to Claude Project

## Quality Standards

Both skills enforce:
- ✅ >80% test coverage
- ✅ Comprehensive documentation
- ✅ Working examples
- ✅ CHANGELOG updates
- ✅ Task tracking updates
- ✅ All acceptance criteria met
- ✅ No shortcuts or technical debt

## Expected Outcomes

### After Ticket Generation

You should have:
- Complete ticket markdown files
- Dependency graph
- Sprint allocation suggestions
- Clear acceptance criteria for each task
- Estimated effort for planning

### After Task Implementation

You should have:
- Working, tested code
- >80% test coverage
- Documentation (code + README)
- Usage examples
- Updated CHANGELOG (if applicable)
- Updated progress tracking
- Clean git commit
- Ready for next task

## Tips for Effective Use

1. **Read both skills before starting** - Understand the full workflow
2. **Follow the order** - Generate tickets first, then implement
3. **Don't skip steps** - Each phase builds on the previous
4. **Verify thoroughly** - Acceptance criteria must be met
5. **Update tracking** - Keep state current for continuity

## Troubleshooting

### Skill Not Being Applied?

Make sure to explicitly reference it:
```
"Generate tickets following the ticket-generation skill"
"Implement this task following the task-implementation skill"
```

### Incomplete Implementation?

Check that all phases were completed:
- Code ✅
- Tests ✅
- Documentation ✅
- Examples ✅
- CHANGELOG ✅
- Tracking Update ✅

### Can't Find Next Task?

Check progress tracking file:
- Look for tasks with "Dependencies: ✅ All met"
- Sort by Priority (P0 first)
- Check "Next Up" section

## License

These skills are part of the Lasso project documentation.
