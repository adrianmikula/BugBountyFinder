# Recommended Repositories for Testing

This document lists GitHub repositories with high commit activity and active bug bounty programs, ideal for testing the Bug Bounty Finder system.

## Top Recommendations

### 1. **microsoft/vscode** ⭐ Highly Recommended
- **URL**: `https://github.com/microsoft/vscode`
- **Language**: TypeScript
- **Commit Activity**: Very high (typically 20-50+ commits per day)
- **Bug Bounty**: Microsoft Security Response Center (MSRC)
- **Why**: Extremely active development, large codebase, Microsoft's comprehensive bug bounty program
- **Default Branch**: `main`

**Add to system:**
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/microsoft/vscode",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'
```

### 2. **facebook/react**
- **URL**: `https://github.com/facebook/react`
- **Language**: JavaScript
- **Commit Activity**: High (typically 10-30 commits per day)
- **Bug Bounty**: Meta Bug Bounty Program
- **Why**: One of the most popular JavaScript frameworks, active development, Meta's bug bounty program
- **Default Branch**: `main`

**Add to system:**
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/facebook/react",
    "language": "JavaScript",
    "defaultBranch": "main"
  }'
```

### 3. **vercel/next.js**
- **URL**: `https://github.com/vercel/next.js`
- **Language**: TypeScript/JavaScript
- **Commit Activity**: High (typically 15-40 commits per day)
- **Bug Bounty**: Vercel Security Program
- **Why**: Modern web framework, very active development, growing ecosystem
- **Default Branch**: `canary` (main development branch)

**Add to system:**
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/vercel/next.js",
    "language": "TypeScript",
    "defaultBranch": "canary"
  }'
```

### 4. **microsoft/TypeScript**
- **URL**: `https://github.com/microsoft/TypeScript`
- **Language**: TypeScript
- **Commit Activity**: High (typically 10-25 commits per day)
- **Bug Bounty**: Microsoft Security Response Center (MSRC)
- **Why**: Core language compiler, active development, Microsoft's bug bounty program
- **Default Branch**: `main`

**Add to system:**
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/microsoft/TypeScript",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'
```

### 5. **kubernetes/kubernetes**
- **URL**: `https://github.com/kubernetes/kubernetes`
- **Language**: Go
- **Commit Activity**: Very high (typically 30-60+ commits per day)
- **Bug Bounty**: CNCF Security Response Team
- **Why**: Large-scale project, extremely active, critical infrastructure
- **Default Branch**: `master`

**Add to system:**
```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/kubernetes/kubernetes",
    "language": "Go",
    "defaultBranch": "master"
  }'
```

## Other Good Options

### 6. **tensorflow/tensorflow**
- **URL**: `https://github.com/tensorflow/tensorflow`
- **Language**: Python/C++
- **Commit Activity**: High (typically 20-50 commits per day)
- **Bug Bounty**: Google Bug Bounty Program
- **Default Branch**: `master`

### 7. **pytorch/pytorch**
- **URL**: `https://github.com/pytorch/pytorch`
- **Language**: Python/C++
- **Commit Activity**: High (typically 15-40 commits per day)
- **Bug Bounty**: Meta Bug Bounty Program
- **Default Branch**: `main`

### 8. **nodejs/node**
- **URL**: `https://github.com/nodejs/node`
- **Language**: JavaScript/C++
- **Commit Activity**: Very high (typically 20-50+ commits per day)
- **Bug Bounty**: Node.js Security Working Group
- **Default Branch**: `main`

### 9. **spring-projects/spring-boot**
- **URL**: `https://github.com/spring-projects/spring-boot`
- **Language**: Java
- **Commit Activity**: High (typically 10-30 commits per day)
- **Bug Bounty**: Pivotal/VMware Security Response
- **Default Branch**: `main`

### 10. **golang/go**
- **URL**: `https://github.com/golang/go`
- **Language**: Go
- **Commit Activity**: High (typically 15-35 commits per day)
- **Bug Bounty**: Google Security Team
- **Default Branch**: `master`

## How to Add Repositories

### Using cURL

```bash
# Add a repository
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/owner/repo",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'
```

### Using the API

The repository management API provides the following endpoints:

- `POST /api/repositories` - Add a new repository
- `GET /api/repositories` - List all repositories
- `GET /api/repositories/{id}` - Get repository by ID
- `GET /api/repositories/by-url?url={url}` - Get repository by URL
- `DELETE /api/repositories?url={url}` - Delete a repository

### Example: Adding VS Code

```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/microsoft/vscode",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'
```

## Testing Your Setup

After adding a repository:

1. **Verify it was added:**
   ```bash
   curl http://localhost:8080/api/repositories
   ```

2. **Check webhook setup:**
   - The system will automatically start monitoring the repository
   - Webhooks should be configured on GitHub to send push events

3. **Monitor activity:**
   - Check logs for repository cloning and updates
   - Monitor the triage queue for new bounties
   - Watch for CVE scanning when relevant vulnerabilities are detected

## Notes

- **Language is important**: The system uses the language field to match CVEs and bounties. Make sure to specify the primary language.
- **Default branch**: If not specified, the system will attempt to detect it, but it's better to provide it explicitly.
- **High activity repositories**: Repositories with high commit frequency will generate more webhook events, providing more opportunities to test the system.
- **Bug bounty programs**: All recommended repositories have active bug bounty or security reporting programs, making them ideal for testing the bounty hunting features.

## Quick Start: Add VS Code

For the best testing experience, start with **microsoft/vscode**:

```bash
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/microsoft/vscode",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'
```

This repository has:
- ✅ Very high commit activity (20-50+ commits/day)
- ✅ Active bug bounty program (Microsoft MSRC)
- ✅ Large, active codebase
- ✅ TypeScript (good for code analysis)
- ✅ Well-maintained security policies

