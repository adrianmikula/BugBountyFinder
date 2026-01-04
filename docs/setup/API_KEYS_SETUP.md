# API Keys Setup Guide

This guide explains how to obtain and configure API keys for all external services used by Bug Bounty Finder.

## Required API Keys

### 1. GitHub Personal Access Token (PAT) ⚠️ Recommended

**Purpose**: Authenticated GitHub API requests for higher rate limits and repository access

**Why needed**:
- Higher rate limits (5,000 requests/hour authenticated vs 60/hour unauthenticated)
- Access to private repositories (if needed)
- Better reliability for repository operations

**How to get**:
1. Go to GitHub: https://github.com/settings/tokens
2. Click **"Generate new token"** → **"Generate new token (classic)"**
3. Give it a descriptive name: `BugBountyFinder`
4. Set expiration (recommend 90 days or custom)
5. Select scopes:
   - ✅ `repo` - Full control of private repositories (if monitoring private repos)
   - ✅ `read:org` - Read org and team membership (if monitoring org repos)
   - ✅ `read:user` - Read user profile data
6. Click **"Generate token"**
7. **Copy the token immediately** - you won't be able to see it again!

**Set environment variable**:
```bash
export GITHUB_API_TOKEN="ghp_your_token_here"
```

**Or add to `.env` file**:
```
GITHUB_API_TOKEN=ghp_your_token_here
```

---

### 2. GitHub Webhook Secret 🔐 Required

**Purpose**: Verify webhook signatures to ensure requests are from GitHub

**How to generate**:
```bash
# Using OpenSSL
openssl rand -hex 32

# Or using Python
python -c "import secrets; print(secrets.token_hex(32))"

# Or using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

**Set environment variable**:
```bash
export GITHUB_WEBHOOK_SECRET="your-generated-secret-here"
```

**Or add to `.env` file**:
```
GITHUB_WEBHOOK_SECRET=your-generated-secret-here
```

**Important**: Use the same secret when configuring webhooks in GitHub repository settings.

---

### 3. Algora API Key 🔑 Optional (if required)

**Purpose**: Authenticate requests to Algora bug bounty platform

**Status**: Currently, Algora API may work without authentication, but API keys may be required for:
- Higher rate limits
- Access to premium features
- Better reliability

**How to get** (if available):
1. Visit: https://algora.io
2. Sign up or log in
3. Navigate to **Settings** → **API Keys**
4. Generate a new API key
5. Copy the key

**Set environment variable**:
```bash
export ALGORA_API_KEY="your-algora-api-key"
```

**Or add to `.env` file**:
```
ALGORA_API_KEY=your-algora-api-key
```

**Note**: If Algora doesn't require authentication, you can leave this empty.

---

### 4. Polar.sh API Key 🔑 Optional (if required)

**Purpose**: Authenticate requests to Polar.sh PR bounty platform

**Status**: Currently, Polar.sh API may work without authentication, but API keys may be required for:
- Higher rate limits
- Access to premium features
- Better reliability

**How to get** (if available):
1. Visit: https://polar.sh
2. Sign up or log in
3. Navigate to **Settings** → **API** or **Developer Settings**
4. Generate a new API key or access token
5. Copy the key

**Set environment variable**:
```bash
export POLAR_API_KEY="your-polar-api-key"
```

**Or add to `.env` file**:
```
POLAR_API_KEY=your-polar-api-key
```

**Note**: If Polar.sh doesn't require authentication, you can leave this empty.

---

### 5. NVD API Key 🔑 Optional (Recommended)

**Purpose**: Higher rate limits for NVD (National Vulnerability Database) API

**Why recommended**:
- Without API key: 5 requests per 30 seconds
- With API key: 50 requests per 30 seconds (10x improvement)

**How to get**:
1. Visit: https://nvd.nist.gov/developers/request-an-api-key
2. Fill out the form:
   - **First Name**: Your first name
   - **Last Name**: Your last name
   - **Email**: Your email address
   - **Organization**: Your organization (or "Personal")
   - **Use Case**: Select "Research" or "Development"
3. Submit the form
4. Check your email for the API key
5. Copy the key (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

**Set environment variable**:
```bash
export NVD_API_KEY="your-nvd-api-key"
```

**Or add to `.env` file**:
```
NVD_API_KEY=your-nvd-api-key
```

**Note**: This is optional but highly recommended for better CVE monitoring performance.

---

### 6. Ollama Configuration 🦙 No API Key Needed

**Purpose**: Local LLM inference (no external API calls)

**Setup**:
1. Install Ollama: https://ollama.ai
2. Pull the recommended model:
   ```bash
   ollama pull deepseek-coder:6.7b
   ```
3. Start Ollama (usually runs automatically):
   ```bash
   ollama serve
   ```

**Configuration** (already in `application.yml`):
- Base URL: `http://localhost:11434` (default)
- Model: `deepseek-coder:6.7b` (default)

**Environment variables** (optional overrides):
```bash
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="deepseek-coder:6.7b"
```

---

## Quick Setup Checklist

- [ ] Generate GitHub Personal Access Token
- [ ] Generate GitHub Webhook Secret
- [ ] (Optional) Get Algora API Key (if required)
- [ ] (Optional) Get Polar.sh API Key (if required)
- [ ] (Recommended) Get NVD API Key
- [ ] Install and configure Ollama
- [ ] Create `.env` file with all keys
- [ ] Test configuration

---

## Environment Variables Summary

Create a `.env` file in your project root with:

```bash
# GitHub
GITHUB_API_TOKEN=ghp_your_github_pat_here
GITHUB_WEBHOOK_SECRET=your-generated-webhook-secret-here

# Bug Bounty Platforms (optional)
ALGORA_API_KEY=your-algora-api-key
POLAR_API_KEY=your-polar-api-key

# NVD (optional but recommended)
NVD_API_KEY=your-nvd-api-key

# Ollama (optional overrides)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=deepseek-coder:6.7b

# Database (if not using defaults)
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis (if not using defaults)
REDIS_HOST=localhost
REDIS_PORT=6379

# Repository clone path (optional)
REPO_CLONE_PATH=./repos
```

---

## Security Best Practices

1. **Never commit API keys to Git**
   - Add `.env` to `.gitignore`
   - Use `.env.example` for documentation

2. **Use environment variables**
   - Don't hardcode keys in source code
   - Use Spring's `${VARIABLE_NAME}` syntax

3. **Rotate keys regularly**
   - GitHub tokens: Every 90 days (or as configured)
   - Other keys: Follow provider recommendations

4. **Use least privilege**
   - Only grant necessary permissions
   - GitHub PAT: Only select needed scopes

5. **Monitor usage**
   - Check GitHub API rate limits
   - Monitor for unauthorized access

---

## Testing Your Configuration

### Test GitHub API Token

```bash
curl -H "Authorization: token $GITHUB_API_TOKEN" \
     https://api.github.com/user
```

### Test NVD API Key

```bash
curl -H "apiKey: $NVD_API_KEY" \
     "https://services.nvd.nist.gov/rest/json/cves/2.0?resultsPerPage=1"
```

### Test Ollama

```bash
curl http://localhost:11434/api/tags
```

---

## Troubleshooting

### GitHub API Rate Limit Exceeded

**Problem**: Getting 403 errors with "API rate limit exceeded"

**Solution**:
1. Add `GITHUB_API_TOKEN` environment variable
2. Wait for rate limit reset (check headers: `X-RateLimit-Reset`)
3. Reduce polling frequency in `application.yml`

### Ollama Connection Failed

**Problem**: Cannot connect to Ollama at `http://localhost:11434`

**Solution**:
1. Ensure Ollama is running: `ollama serve`
2. Check if port 11434 is accessible
3. Verify `OLLAMA_BASE_URL` environment variable

### NVD API Slow

**Problem**: CVE monitoring is very slow

**Solution**:
1. Get NVD API key (10x rate limit improvement)
2. Set `NVD_API_KEY` environment variable
3. Adjust `app.cve.nvd.rate-limit-delay-ms` if needed

---

## Next Steps

After setting up API keys:

1. **Configure GitHub Webhooks**: See [GitHub Webhook Setup](GITHUB_WEBHOOK_SETUP.md)
2. **Add Repositories**: See [Recommended Repositories](../recommended-repositories.md)
3. **Start the Application**: Run `./gradlew bootRun`
4. **Monitor Logs**: Check for authentication errors

---

## Support

- **GitHub API**: https://docs.github.com/en/rest
- **NVD API**: https://nvd.nist.gov/developers/vulnerabilities
- **Ollama**: https://ollama.ai/docs
- **Algora**: https://algora.io (check their documentation)
- **Polar.sh**: https://polar.sh (check their documentation)

