#!/usr/bin/env bash
#
# prepare-release.sh
#
# Interactive script to help prepare a release following gitflow process.
# This script assists with version bumps, changelog updates, and provides
# documentation update reminders.
#
# Usage: ./scripts/prepare-release.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo -e "\n${BLUE}==== $1 ====${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

confirm() {
    read -p "$1 (y/n) " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

# Check if we're on develop branch
current_branch=$(git branch --show-current)
if [[ "$current_branch" != "develop" ]]; then
    print_error "You must be on the 'develop' branch to prepare a release."
    print_warning "Current branch: $current_branch"
    exit 1
fi

# Check if working directory is clean
if [[ -n $(git status --porcelain) ]]; then
    print_error "Working directory has uncommitted changes. Please commit or stash them first."
    git status --short
    exit 1
fi

# Pull latest changes
print_header "Pulling Latest Changes"
git pull origin develop
print_success "Up to date with origin/develop"

# Get current version
current_version=$(cat VERSION)
print_header "Current Version"
echo "Current version: $current_version"

# Prompt for new version
print_header "New Version"
echo "Enter the new version number (format: X.Y.Z)"
echo ""
echo "Semantic Versioning:"
echo "  MAJOR (X.0.0) - Breaking changes"
echo "  MINOR (0.X.0) - New features (backwards compatible)"
echo "  PATCH (0.0.X) - Bug fixes (backwards compatible)"
echo ""
read -p "New version: " new_version

# Validate version format
if ! [[ "$new_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    print_error "Invalid version format. Use X.Y.Z (e.g., 0.2.0)"
    exit 1
fi

# Confirm version bump
echo ""
echo "Version change: $current_version → $new_version"
if ! confirm "Is this correct?"; then
    print_warning "Aborting."
    exit 0
fi

# Create release branch
release_branch="release/$new_version"
print_header "Creating Release Branch"
echo "Creating branch: $release_branch"
git checkout -b "$release_branch"
print_success "Release branch created"

# Update VERSION file
print_header "Updating VERSION File"
echo "$new_version" > VERSION
print_success "VERSION file updated to $new_version"

# Prompt for CHANGELOG update
print_header "CHANGELOG.md Update Required"
print_warning "You need to manually update CHANGELOG.md:"
echo ""
echo "1. Move items from [Unreleased] to [${new_version}] - $(date +%Y-%m-%d)"
echo "2. Update comparison links at the bottom"
echo "3. Ensure [Unreleased] section remains for future changes"
echo ""
echo "Opening CHANGELOG.md in your editor..."
sleep 2

# Open CHANGELOG in editor (respects EDITOR env var, fallbacks to common editors)
if [[ -n "$EDITOR" ]]; then
    $EDITOR CHANGELOG.md
elif command -v code &> /dev/null; then
    code CHANGELOG.md
elif command -v vim &> /dev/null; then
    vim CHANGELOG.md
elif command -v nano &> /dev/null; then
    nano CHANGELOG.md
else
    print_warning "Could not detect editor. Please manually edit CHANGELOG.md"
    read -p "Press Enter when CHANGELOG.md is updated..."
fi

# Confirm CHANGELOG was updated
echo ""
if ! confirm "Have you updated CHANGELOG.md?"; then
    print_error "Please update CHANGELOG.md before continuing."
    print_warning "Run this script again when ready."
    exit 1
fi

# Show changes
print_header "Changes to be Committed"
git diff --stat VERSION CHANGELOG.md
echo ""

# Commit changes
if confirm "Commit these changes?"; then
    git add VERSION CHANGELOG.md
    git commit -m "chore(release): bump version to $new_version"
    print_success "Changes committed"
else
    print_warning "Changes not committed. You can commit manually:"
    echo "  git add VERSION CHANGELOG.md"
    echo "  git commit -m \"chore(release): bump version to $new_version\""
fi

# Push release branch
echo ""
if confirm "Push release branch to origin?"; then
    git push -u origin "$release_branch"
    print_success "Release branch pushed to origin"
else
    print_warning "Branch not pushed. You can push manually:"
    echo "  git push -u origin $release_branch"
fi

# Create PR to main
echo ""
if confirm "Create PR to main? (requires gh CLI)"; then
    if command -v gh &> /dev/null; then
        gh pr create --base main --title "Release v$new_version" --body "Release version $new_version

## Changes
See CHANGELOG.md for detailed changes.

## Checklist
- [x] VERSION file updated
- [x] CHANGELOG.md updated with release notes
- [ ] CI checks passing
- [ ] Ready to merge

After merge, this will trigger automated release creation."
        print_success "Pull request created"
    else
        print_error "GitHub CLI (gh) not installed"
        print_warning "Create PR manually at:"
        echo "  https://github.com/jackmoch/lasso/compare/main...$release_branch"
    fi
fi

# Post-release documentation reminder
print_header "📝 IMPORTANT: Post-Release Documentation Updates"
print_warning "After the PR merges to main, you MUST update documentation!"
echo ""
echo "Update these files on the develop branch:"
echo "  1. STATUS.md - Current state, version, completed work, metrics"
echo "  2. NEXT.md - Update immediate next task to upcoming sprint"
echo "  3. CLAUDE.md - Current sprint, version, completed section"
echo "  4. docs/sprints/sprint-X-summary.md - Create new sprint summary"
echo "  5. MEMORY.md - Add learnings and gotchas (.claude/projects/.../memory/)"
echo ""
echo "Then:"
echo "  1. Merge main back to develop: git checkout develop && git merge main"
echo "  2. Commit docs: git commit -am 'docs: update all project documentation after v$new_version release'"
echo "  3. Push: git push origin develop"
echo ""
print_warning "See CONTRIBUTING.md 'Post-Release Documentation Updates' section for details."

# Summary
print_header "Summary"
print_success "Release branch created: $release_branch"
print_success "VERSION updated: $current_version → $new_version"
print_success "CHANGELOG.md updated"
echo ""
echo "Next steps:"
echo "  1. Wait for PR to be approved and merged to main"
echo "  2. Automated release will be created (v$new_version)"
echo "  3. Update all documentation files (see reminder above)"
echo "  4. Merge main back to develop"
echo ""
print_success "Release preparation complete! 🚀"
