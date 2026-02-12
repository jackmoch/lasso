#!/usr/bin/env bash
#
# wait-for-ci.sh - Intelligently wait for GitHub Actions CI to complete
#
# Usage:
#   ./scripts/wait-for-ci.sh <pr-number>
#   ./scripts/wait-for-ci.sh 7
#
# Features:
# - Fetches historical average CI run time
# - Waits for expected duration before first check
# - Provides progress updates
# - Returns when CI completes (success or failure)
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PR_NUMBER="${1:-}"

if [ -z "$PR_NUMBER" ]; then
    echo -e "${RED}Error: PR number required${NC}"
    echo "Usage: $0 <pr-number>"
    exit 1
fi

echo -e "${BLUE}⏳ Waiting for CI to complete on PR #${PR_NUMBER}...${NC}"
echo ""

# Fetch historical average duration
echo -e "${YELLOW}📊 Fetching historical CI run times...${NC}"
HISTORICAL_AVG=$(gh run list --workflow=ci.yml --limit 10 --json conclusion,createdAt,updatedAt --jq '[.[] | select(.conclusion == "success") | (((.updatedAt | fromdateiso8601) - (.createdAt | fromdateiso8601)))] | add / length | floor' 2>/dev/null || echo "135")

if [ -z "$HISTORICAL_AVG" ] || [ "$HISTORICAL_AVG" = "null" ]; then
    HISTORICAL_AVG=135  # Default: 2min 15s
fi

HISTORICAL_MIN=$((HISTORICAL_AVG / 60))
HISTORICAL_SEC=$((HISTORICAL_AVG % 60))

echo -e "${GREEN}   Historical average: ${HISTORICAL_MIN}m ${HISTORICAL_SEC}s${NC}"
echo ""

# Add 15 second buffer for safety
WAIT_TIME=$((HISTORICAL_AVG + 15))
WAIT_MIN=$((WAIT_TIME / 60))
WAIT_SEC=$((WAIT_TIME % 60))

echo -e "${YELLOW}⏱️  Waiting ${WAIT_MIN}m ${WAIT_SEC}s before first check...${NC}"

# Wait with progress indicator
START_TIME=$(date +%s)
while true; do
    ELAPSED=$(($(date +%s) - START_TIME))

    if [ $ELAPSED -ge $WAIT_TIME ]; then
        break
    fi

    REMAINING=$((WAIT_TIME - ELAPSED))
    REMAINING_MIN=$((REMAINING / 60))
    REMAINING_SEC=$((REMAINING % 60))

    printf "\r   ${BLUE}Time remaining: ${REMAINING_MIN}m ${REMAINING_SEC}s ${NC}"
    sleep 5
done

echo ""
echo ""
echo -e "${GREEN}✓ Expected time elapsed, checking CI status...${NC}"
echo ""

# Check CI status
gh pr checks "$PR_NUMBER"
EXIT_CODE=$?

echo ""

if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ All CI checks passed!${NC}"
else
    echo -e "${YELLOW}⚠️  CI checks still running or failed. Run 'gh pr checks ${PR_NUMBER}' to see details.${NC}"
fi

exit $EXIT_CODE
