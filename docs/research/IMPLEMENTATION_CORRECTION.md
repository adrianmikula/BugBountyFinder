# Implementation Correction: Bounty Discovery

## Correction

The system was initially implemented to scan GitHub issues directly for dollar amounts. This was **incorrect**.

## Correct Approach

**Bounties are discovered by polling Algora and Polar.sh platforms**, not by scanning GitHub issues.

### How It Actually Works

1. **Poll Algora API** (requires login/API key)
   - Returns list of bounties
   - Each bounty includes:
     - `repositoryUrl` - Links to GitHub repository
     - `issueId` - Links to specific GitHub issue
     - `amount` - Bounty amount
     - `title`, `description` - Issue details

2. **Poll Polar.sh API** (requires login/API key)
   - Returns list of bounties
   - Same structure as Algora

3. **Process Bounties**
   - Save to database
   - Extract GitHub issue information
   - Analyze the GitHub issue (not scan for dollar amounts)
   - Generate fixes

### Why This Matters

- **Platforms are Source of Truth**: Algora and Polar.sh maintain which issues have bounties
- **Authentication Required**: You must log in to these platforms to get API keys
- **No False Positives**: Only process issues that actually have bounties attached
- **Centralized Management**: Bounty amounts and status managed on platforms

## What Was Changed

1. **Documentation Updated**:
   - Clarified that Algora/Polar.sh require logins
   - Updated API keys setup guide
   - Created bounty discovery flow documentation

2. **Configuration Updated**:
   - Marked Algora/Polar.sh API keys as required
   - Removed GitHub issue scanning configuration

3. **Code Comments Updated**:
   - Added notes that GitHub issue scanning is deprecated
   - Clarified that bounties come from platforms

## Current Flow

```
Algora/Polar.sh (with login/API key)
    ↓
Poll for bounties
    ↓
Get bounties with GitHub issue links
    ↓
BountyPollingService processes bounties
    ↓
IssueAnalysisService analyzes linked GitHub issues
    ↓
CVEVerificationService generates fixes
```

## Setup Requirements

**Required:**
1. Create account on Algora (https://algora.io)
2. Log in and generate API key
3. Create account on Polar.sh (https://polar.sh)
4. Log in and generate API key
5. Set `ALGORA_API_KEY` and `POLAR_API_KEY` environment variables

**Optional:**
- GitHub API token (for higher rate limits)
- NVD API key (for CVE monitoring)

## References

- [Bounty Discovery Flow](docs/architecture/BOUNTY_DISCOVERY_FLOW.md)
- [API Keys Setup](docs/setup/API_KEYS_SETUP.md)

