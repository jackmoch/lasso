# Scripts

Helper scripts for development and CI automation.

## wait-for-ci.sh

Intelligently waits for GitHub Actions CI to complete on a pull request.

### Features

- 📊 Fetches historical average CI run time (last 10 successful runs)
- ⏱️ Waits for expected duration before first check (avoids unnecessary polling)
- 🔄 Provides live progress updates with countdown timer
- ✅ Returns success/failure status when CI completes

### Usage

```bash
# Wait for CI on PR #7
./scripts/wait-for-ci.sh 7

# Use in autonomous workflow
./scripts/wait-for-ci.sh $PR_NUMBER && echo "CI passed!"
```

### Example Output

```
⏳ Waiting for CI to complete on PR #7...

📊 Fetching historical CI run times...
   Historical average: 2m 15s

⏱️  Waiting 2m 30s before first check...
   Time remaining: 1m 45s

✓ Expected time elapsed, checking CI status...

lint-and-build  pass  1m24s  https://github.com/...
docker-build    pass  34s    https://github.com/...

✅ All CI checks passed!
```

### How It Works

1. Queries GitHub API for last 10 successful CI runs
2. Calculates average duration
3. Waits for average duration + 15s buffer
4. Shows countdown timer with 5s updates
5. Checks CI status after wait
6. Returns exit code 0 (success) or non-zero (failure/pending)

### Integration with Claude Code

Claude Code can use this script in the autonomous PR workflow to avoid repeated polling:

```bash
# Instead of:
sleep 15 && gh pr checks 7
sleep 60 && gh pr checks 7
sleep 90 && gh pr checks 7

# Use:
./scripts/wait-for-ci.sh 7
```

This reduces API calls and provides better user feedback.

## Requirements

- `gh` (GitHub CLI)
- `jq` (JSON processor)
- Bash 4.0+

Both are already installed in the development environment.
