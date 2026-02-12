# AI Collaboration System

This project uses a **universal session handoff system** that works with any AI coding assistant.

## 🎯 Core Concept

Three simple files enable seamless AI collaboration:

1. **`STATUS.md`** - "Where are we?" (current project state)
2. **`NEXT.md`** - "What should I do?" (immediate next task)
3. **MEMORY.md`** - "What should I know?" (gotchas and patterns)

Plus:
4. **`CONTEXT.md`** - "What is this project?" (full overview)

## 🚀 Quick Start for Any AI Tool

**New session? Read 3 files:**
```
1. STATUS.md  → Current state (2 min read)
2. NEXT.md    → Next task (1 min read)
3. MEMORY.md  → Gotchas (2 min scan)
```

**Then start coding!** Everything you need to know is in those files.

## 📁 File Structure

```
lasso/
├── STATUS.md              ✅ Current project snapshot
├── NEXT.md                ✅ Immediate next task
├── MEMORY.md              ✅ Persistent learnings (symlink)
├── CONTEXT.md             ✅ Universal project context
│
├── CLAUDE.md              🤖 Claude Code specific
├── .ai/
│   ├── README.md          📖 AI tool docs
│   ├── .cursorrules       🤖 Cursor IDE specific
│   └── [other tools...]
│
├── .github/
│   └── copilot-instructions.md  🤖 GitHub Copilot specific
│
└── .claude/               🤖 Claude Code auto memory
    └── projects/.../memory/
        └── MEMORY.md      (source of symlink)
```

## 🔄 Universal vs Tool-Specific

### Universal Files (Work Everywhere)
- `STATUS.md` - Pure markdown, any tool can read
- `NEXT.md` - Pure markdown, any tool can read
- `MEMORY.md` - Pure markdown, any tool can read
- `CONTEXT.md` - Pure markdown, any tool can read

### Tool-Specific Files
- `CLAUDE.md` - Claude Code
- `.ai/.cursorrules` - Cursor IDE
- `.github/copilot-instructions.md` - GitHub Copilot
- *(Add more as needed)*

**Strategy:** Tool-specific files **reference** the universal files instead of duplicating content.

## 🛠️ Supported AI Tools

### ✅ Claude Code
Reads: `CLAUDE.md`, `STATUS.md`, `NEXT.md`, `MEMORY.md`

### ✅ Cursor IDE
Reads: `.ai/.cursorrules`, falls back to `CONTEXT.md`, `STATUS.md`

### ✅ GitHub Copilot
Reads: `.github/copilot-instructions.md`, references `CONTEXT.md`

### ✅ Windsurf
Can read: `CONTEXT.md`, `STATUS.md`, `NEXT.md`

### ✅ Continue.dev
Can reference: `CONTEXT.md` in context providers

### ✅ Any Future Tool
Just read the markdown files in root!

## 💡 Why This Works

### Portability
- Pure markdown files work everywhere
- No lock-in to specific tools
- Human developers can use them too!

### Session Handoff
- New AI session reads 3 files (~5 min)
- Immediately understands project state
- Knows exactly what to do next
- Remembers past gotchas

### Maintainability
- Single source of truth (`STATUS.md`)
- Clear task tracking (`NEXT.md`)
- Institutional knowledge preserved (`MEMORY.md`)
- Self-documenting project state

## 📝 Keeping It Updated

### When You Finish a Task
```bash
# 1. Update NEXT.md - move task to "completed"
# 2. Update STATUS.md - mark milestone complete
# 3. Commit both together
git add NEXT.md STATUS.md
git commit -m "docs: update project status after completing Phase 4"
```

### When You Encounter a Gotcha
```bash
# Add to MEMORY.md
echo "## New Gotcha\n..." >> MEMORY.md
git commit MEMORY.md -m "docs: add gotcha about X"
```

### When Starting a New Sprint
```bash
# Update all three
# STATUS.md - New sprint, new milestone
# NEXT.md - New top task
# MEMORY.md - Any new patterns learned
```

## 🎁 Using This in Other Projects

This system is **highly portable**. To use in a new project:

### Option 1: Copy Files
```bash
cp STATUS.md NEXT.md CONTEXT.md ../new-project/
cp -r .ai ../new-project/
```

### Option 2: Template Repository
Create a template repo with these files and clone for new projects.

### Option 3: Script It
```bash
# init-ai-collab.sh
cat > STATUS.md << 'EOF'
# Project Status
...template...
EOF

cat > NEXT.md << 'EOF'
# What to Work On Next
...template...
EOF

# etc.
```

## 🔍 Example: New Session Workflow

**Scenario:** You're opening Claude Code (or Cursor, or Copilot) in a fresh session.

```bash
# Step 1: Read STATUS.md (2 min)
"Oh, we're on Sprint 3-4, backend development.
 Phase 1-3 complete, Phase 4-6 in progress.
 Tests passing, CI working."

# Step 2: Read NEXT.md (1 min)
"Next task: Implement OAuth routes in routes.clj.
 Here's the spec, here are the dependencies,
 here's how to test."

# Step 3: Scan MEMORY.md (2 min)
"Gotcha: PRs go to develop, not main!
 Gotcha: Use wait-for-ci.sh script.
 Pattern: Always mock external APIs in tests."

# Step 4: Start coding! (rest of session)
```

**Total onboarding time:** ~5 minutes
**Context preserved:** 100%

## 🎯 Benefits

### For AI Assistants
- Fast context loading (3 files vs. entire codebase)
- Clear direction (no guessing what to do next)
- Avoid repeated mistakes (MEMORY.md)

### For Developers
- Great onboarding docs for humans too
- Single source of truth for project state
- Easy to see what's done vs. what's next

### For Teams
- Any team member can see current state
- Clear task tracking without heavy tooling
- Works with any AI tool team members prefer

## 📚 Further Reading

- `STATUS.md` - See current project state
- `NEXT.md` - See what's next
- `MEMORY.md` - See gotchas and patterns
- `CONTEXT.md` - See full project overview
- `.ai/README.md` - See tool-specific configs

---

**This system is tool-agnostic, portable, and scales to any project size.**

Use it with Claude Code, Cursor, Copilot, or any future AI coding assistant!
