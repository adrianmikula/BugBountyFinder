# Bounty Discovery Flow

## Overview

The system discovers bounties by **polling Algora and Polar.sh platforms**, not by scanning GitHub issues directly. These platforms link bounties to GitHub issues, which is how we identify which issues have monetary rewards.

## Discovery Flow

```
┌─────────────────┐
│  Algora API     │  ← Poll for bounties (requires login/API key)
└────────┬────────┘
         │
         │ Returns bounties with:
         │ - repositoryUrl (GitHub repo)
         │ - issueId (GitHub issue number)
         │ - amount (bounty amount)
         │ - title, description
         │
         ▼
┌─────────────────┐
│  Polar.sh API   │  ← Poll for bounties (requires login/API key)
└────────┬────────┘
         │
         │ Returns bounties with:
         │ - repositoryUrl (GitHub repo)
         │ - issueId (GitHub issue number)
         │ - amount (bounty amount)
         │ - title, description
         │
         ▼
┌─────────────────────────┐
│  BountyPollingService   │
│  - Filters by min amount│
│  - Saves to database    │
│  - Enqueues for triage  │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  IssueAnalysisService   │
│  - Analyzes GitHub issue│
│  - Extracts root cause  │
│  - Identifies code      │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  CVEVerificationService │
│  - Verifies root cause  │
│  - Generates fix        │
│  - Verifies fix         │
└─────────────────────────┘
```

## Key Points

### 1. Bounties Come from Platforms, Not GitHub

**❌ Incorrect Approach:**
- Scanning GitHub issues for dollar amounts
- Looking for "$50" or "$100" in issue titles/bodies

**✅ Correct Approach:**
- Poll Algora API → Get bounties → Each bounty links to a GitHub issue
- Poll Polar.sh API → Get bounties → Each bounty links to a GitHub issue
- The platforms are the source of truth for which issues have bounties

### 2. Authentication Required

Both platforms require authentication:

- **Algora**: 
  - Sign up at https://algora.io
  - Log in to your account
  - Generate API key from settings
  - Set `ALGORA_API_KEY` environment variable

- **Polar.sh**:
  - Sign up at https://polar.sh
  - Log in to your account
  - Generate API key from settings
  - Set `POLAR_API_KEY` environment variable

### 3. Bounty Data Structure

When platforms return bounties, they include:

```json
{
  "issueId": "123",
  "repositoryUrl": "https://github.com/owner/repo",
  "amount": 100.00,
  "currency": "USD",
  "title": "Fix bug in authentication",
  "description": "Issue description...",
  "platform": "algora" // or "polar"
}
```

### 4. Issue Analysis

Once a bounty is discovered:

1. **Extract GitHub Issue**: Use `repositoryUrl` and `issueId` to fetch the GitHub issue
2. **Analyze Issue**: Use `IssueAnalysisService` to understand the bug
3. **Generate Fix**: Use `CVEVerificationService` to generate and verify fixes

## Configuration

### application.yml

```yaml
app:
  bounty:
    polling:
      interval-seconds: 300  # Poll every 5 minutes
      enabled: true
    platforms:
      algora:
        api-url: https://api.algora.io/v1
        api-key: ${ALGORA_API_KEY:}  # Required - from login
        rate-limit-per-minute: 60
      polar:
        api-url: https://api.polar.sh
        api-key: ${POLAR_API_KEY:}  # Required - from login
        rate-limit-per-minute: 60
```

### Environment Variables

```bash
# Required - Get these by logging into the platforms
ALGORA_API_KEY=your-algora-api-key-from-login
POLAR_API_KEY=your-polar-api-key-from-login

# Optional but recommended
GITHUB_API_TOKEN=your-github-pat
```

## Why This Approach?

1. **Platforms are Source of Truth**: Algora and Polar.sh maintain the authoritative list of which issues have bounties
2. **Centralized Management**: Bounty amounts, status, and details are managed on the platforms
3. **Reliability**: Platforms handle payment processing, verification, and bounty lifecycle
4. **No False Positives**: We only process issues that actually have bounties attached

## GitHub Issue Scanning (Deprecated)

The `GitHubIssueScannerService` that scans GitHub issues for dollar amounts is **deprecated** and should not be used. The correct flow is:

1. Poll Algora/Polar.sh → Get bounties
2. Each bounty links to a GitHub issue
3. Analyze that specific GitHub issue

## Troubleshooting

### No Bounties Found

**Check:**
1. Are API keys set? (`ALGORA_API_KEY`, `POLAR_API_KEY`)
2. Did you log in to the platforms to get the keys?
3. Are the API keys valid?
4. Check logs for authentication errors

### Authentication Errors

**Symptoms**: 401 Unauthorized errors

**Solution**:
1. Verify you're logged into Algora/Polar.sh
2. Regenerate API keys if expired
3. Check that keys are correctly set in environment variables

### Rate Limiting

**Symptoms**: 429 Too Many Requests errors

**Solution**:
1. Reduce polling frequency in `application.yml`
2. Check platform-specific rate limits
3. Implement exponential backoff (already done via Resilience4j)

