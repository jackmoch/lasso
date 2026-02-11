# Lasso Documentation

This directory contains all project documentation organized by category.

## Documentation Structure

```
docs/
├── README.md (this file)
├── project-planning/     # Initial planning and design documents
├── development/          # Development workflows and processes
├── deployment/           # Deployment guides and configuration
└── sprints/              # Sprint-specific documentation
```

## Directory Guide

### 📋 project-planning/
Strategic planning and technical design documents created before development.

- **lasso-project-charter.md** - Project vision, goals, and success criteria
- **lasso-prd.md** - Product Requirements Document (features, user stories)
- **lasso-tdd.md** - Technical Design Document (architecture, technology stack)

### 🛠️ development/
Development processes, workflows, and best practices.

- **AUTONOMOUS_PR_WORKFLOW.md** - Claude Code autonomous PR review process

### 🚀 deployment/
Deployment configuration, secrets management, and infrastructure guides.

- **DEPLOYMENT_SECRETS.md** - GitHub secrets and Google Cloud setup

### 📅 sprints/
Sprint-specific implementation plans and summaries.

- **sprint-2-plan.md** - Sprint 2 implementation plan (scaffolding)
- **sprint-2-summary.md** - Sprint 2 completion summary

## Quick Reference

### For New Contributors
1. Start with [project-planning/lasso-prd.md](project-planning/lasso-prd.md) for product overview
2. Read [../CONTRIBUTING.md](../CONTRIBUTING.md) for workflow and standards
3. See [development/AUTONOMOUS_PR_WORKFLOW.md](development/AUTONOMOUS_PR_WORKFLOW.md) for PR process

### For Deployment
1. Review [deployment/DEPLOYMENT_SECRETS.md](deployment/DEPLOYMENT_SECRETS.md)
2. Follow deployment steps in [../README.md](../README.md#deployment)

### For Sprint Planning
- Current sprint documentation in `sprints/` directory
- Sprint plans follow format: `sprint-N-plan.md`
- Sprint summaries follow format: `sprint-N-summary.md`

## Document Lifecycle

1. **Planning Phase** → Documents in `project-planning/`
2. **Development Phase** → Documents in `development/` and `sprints/`
3. **Deployment Phase** → Documents in `deployment/`
4. **Post-Sprint** → Summary added to `sprints/`

## Adding New Documentation

### New Sprint Documentation
```bash
# Create sprint plan
touch docs/sprints/sprint-N-plan.md

# After sprint completion
touch docs/sprints/sprint-N-summary.md
```

### New Development Guide
```bash
touch docs/development/guide-name.md
```

### New Deployment Guide
```bash
touch docs/deployment/guide-name.md
```

## Document Templates

### Sprint Plan Template
```markdown
# Sprint N Implementation Plan: [Sprint Name]

## Context
[Background and goals]

## Implementation Approach
[Phases and steps]

## Critical Files & Specifications
[Key files to create/modify]

## Verification & Testing
[How to verify success]

## Success Criteria
[Checklist of completion criteria]
```

### Sprint Summary Template
```markdown
# Sprint N Summary: [Sprint Name]

## Overview
[What was accomplished]

## What Was Accomplished
[Detailed list of deliverables]

## Verification Results
[Test results and quality checks]

## Next Steps
[What comes next]
```

## Maintenance

- Keep documentation up to date with code changes
- Archive outdated docs to `docs/archive/` if needed
- Link related documents with relative paths
- Use clear, descriptive filenames

## Related Documentation

- [../README.md](../README.md) - Setup and usage guide
- [../CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [../CHANGELOG.md](../CHANGELOG.md) - Version history
- [../CLAUDE.md](../CLAUDE.md) - Claude Code instructions

---

For questions about documentation, open a GitHub Discussion or Issue.
