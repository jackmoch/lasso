# AI Tool Configuration

This directory contains tool-specific configuration files for various AI coding assistants.

## Universal Files (Root Level)

These files work with **any** LLM tool:

- **`../STATUS.md`** - Current project state snapshot
- **`../NEXT.md`** - Immediate next task with implementation details
- **`../MEMORY.md`** - Persistent learnings and gotchas (symlinked from `.claude/`)
- **`../CONTEXT.md`** - Universal project context for all AI tools

## Tool-Specific Files

### Claude Code
- **`../CLAUDE.md`** - Claude Code-specific guidance
- **`../.claude/`** - Claude Code configuration and auto memory

### Cursor IDE
- **`.cursorrules`** - Cursor-specific rules (if needed)
- Falls back to reading `CONTEXT.md`

### GitHub Copilot
- **`../.github/copilot-instructions.md`** - Copilot context (if needed)
- Falls back to reading `CONTEXT.md`

### Windsurf
- **`.windsurfrules`** - Windsurf-specific rules (if needed)
- Falls back to reading `CONTEXT.md`

### Continue.dev
- **`config.json`** - Continue configuration (if needed)
- Can reference `CONTEXT.md` in context providers

## Quick Start for Any Tool

1. **Read** `../STATUS.md` - Where are we?
2. **Read** `../NEXT.md` - What's the immediate task?
3. **Scan** `../MEMORY.md` - What are the gotchas?
4. **Reference** `../CONTEXT.md` - Full project context

That's it! You're ready to code.

## Why This Structure?

**Portability:** The core documentation (`STATUS`, `NEXT`, `MEMORY`, `CONTEXT`) is pure Markdown that works everywhere.

**Tool Flexibility:** Switch between Claude Code, Cursor, Copilot, or any future tool without losing context.

**Human-Friendly:** These files are great onboarding docs for human developers too!

**Session Handoff:** New AI sessions can pick up exactly where the last one left off.

## Adding New Tool Configs

When adding support for a new AI tool:

1. Create tool-specific config in this directory (`.ai/`)
2. Reference the universal files (`STATUS.md`, `NEXT.md`, etc.)
3. Don't duplicate content - use references/symlinks
4. Update this README

## Template for Other Projects

Want to use this system in another project? Copy these files:

```bash
# Core files (required)
STATUS.md
NEXT.md
CONTEXT.md

# Optional (for Claude Code)
CLAUDE.md
.claude/

# Optional (tool-specific)
.ai/
```

Or use this as a template repository.
