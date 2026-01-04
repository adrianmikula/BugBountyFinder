# GitHub Webhook Setup Guide

This guide explains how to configure GitHub webhooks to receive real-time notifications when commits are pushed to repositories.

## Overview

GitHub webhooks allow your application to receive immediate notifications when events occur in repositories (such as push events). This enables real-time code scanning instead of polling.

## Prerequisites

1. **Public Endpoint**: Your application must be publicly accessible via HTTPS
2. **Webhook Secret**: Generate a secure secret for signature verification
3. **GitHub Repository Access**: Admin access to repositories you want to monitor

## Step 1: Generate Webhook Secret

Generate a secure random string to use as your webhook secret:

```bash
# Using OpenSSL
openssl rand -hex 32

# Or using Python
python -c "import secrets; print(secrets.token_hex(32))"
```

**Important**: Store this secret securely. You'll need it for both:
- Application configuration
- GitHub webhook configuration

## Step 2: Configure Application

### Environment Variable

Set the webhook secret as an environment variable:

```bash
export GITHUB_WEBHOOK_SECRET="your-generated-secret-here"
```

Or add to your `.env` file:
```
GITHUB_WEBHOOK_SECRET=your-generated-secret-here
```

### Application Configuration

The webhook is already configured in `application.yml`:

```yaml
app:
  webhooks:
    github:
      enabled: true
      secret: ${GITHUB_WEBHOOK_SECRET:}
      verify-signature: true
```

## Step 3: Deploy Application

Your application must be publicly accessible. Options:

### Option A: Local Development with ngrok

For testing locally:

```bash
# Install ngrok: https://ngrok.com/download
ngrok http 8080

# Use the HTTPS URL provided by ngrok
# Example: https://abc123.ngrok.io
```

### Option B: Cloud Deployment

Deploy to:
- **AWS**: EC2, ECS, or Lambda
- **Azure**: App Service
- **GCP**: Cloud Run or App Engine
- **Heroku**: Direct deployment

**Important**: GitHub requires HTTPS, so ensure SSL/TLS is configured.

## Step 4: Configure GitHub Webhook

For each repository you want to monitor:

1. **Navigate to Repository Settings**
   - Go to your GitHub repository
   - Click **Settings** → **Webhooks**
   - Click **Add webhook**

2. **Configure Webhook**
   - **Payload URL**: `https://your-domain.com/api/webhooks/github/push`
   - **Content type**: `application/json`
   - **Secret**: Paste the secret you generated in Step 1
   - **Which events**: Select "Just the push event"
   - **Active**: ✅ Checked

3. **Save Webhook**
   - Click **Add webhook**
   - GitHub will send a ping event to verify the endpoint

## Step 5: Verify Setup

### Test Webhook

1. **Check Health Endpoint**
   ```bash
   curl https://your-domain.com/api/webhooks/github/health
   ```
   Should return: `GitHub webhook endpoint is active`

2. **Test with GitHub Ping**
   - After adding webhook, GitHub sends a ping event
   - Check application logs for "Received ping event"
   - Verify response is "Pong"

3. **Test Push Event**
   - Make a commit to the repository
   - Push to GitHub
   - Check application logs for push event processing

### Verify in GitHub

1. Go to **Settings** → **Webhooks**
2. Click on your webhook
3. Check **Recent Deliveries** tab
4. Verify deliveries show **200 OK** status

## Webhook Endpoints

### Push Event Endpoint
- **URL**: `/api/webhooks/github/push`
- **Method**: POST
- **Headers Required**:
  - `X-GitHub-Event: push`
  - `X-Hub-Signature-256: sha256=<signature>`
  - `X-GitHub-Delivery: <delivery-id>`

### Ping Event Endpoint
- **URL**: `/api/webhooks/github/ping`
- **Method**: POST
- **Purpose**: GitHub tests webhook connectivity

### Health Check Endpoint
- **URL**: `/api/webhooks/github/health`
- **Method**: GET
- **Purpose**: Verify endpoint is accessible

## Security Considerations

### Signature Verification

The application automatically verifies webhook signatures using HMAC-SHA256. This ensures:
- Requests are from GitHub
- Payload hasn't been tampered with
- Unauthorized requests are rejected

### Best Practices

1. **Never commit secrets** to version control
2. **Use environment variables** for webhook secret
3. **Enable HTTPS** (required by GitHub)
4. **Monitor webhook deliveries** in GitHub
5. **Implement rate limiting** (future enhancement)

## Troubleshooting

### Webhook Not Receiving Events

1. **Check Public Accessibility**
   ```bash
   curl https://your-domain.com/api/webhooks/github/health
   ```

2. **Verify GitHub Configuration**
   - Check webhook URL is correct
   - Verify secret matches
   - Check "Active" is enabled

3. **Check Application Logs**
   - Look for webhook delivery attempts
   - Check for signature verification errors
   - Verify event processing

### Signature Verification Failing

1. **Verify Secret Matches**
   - Check `GITHUB_WEBHOOK_SECRET` environment variable
   - Compare with GitHub webhook secret
   - Ensure no extra whitespace

2. **Check Payload Format**
   - GitHub sends raw JSON
   - Ensure application receives raw body
   - Don't parse before signature verification

### Repository Not Updating

1. **Check Repository Service**
   - Verify repository is tracked
   - Check if repository is already cloned
   - Review update repository logs

2. **Verify Git Operations**
   - Check Git credentials
   - Verify repository permissions
   - Review Git operation errors

## Monitoring

### Application Logs

Monitor these log messages:
- `Received webhook event` - Webhook received
- `Processing push event` - Event processing started
- `Invalid webhook signature` - Security issue
- `Error processing webhook` - Processing failure

### GitHub Webhook Dashboard

Monitor in GitHub:
- **Recent Deliveries**: See all webhook attempts
- **Response Status**: Verify 200 OK responses
- **Delivery Time**: Check latency
- **Request/Response**: Debug payload issues

## Next Steps

1. **Monitor Webhook Deliveries**: Set up alerts for failures
2. **Add Repository Tracking**: Implement database of tracked repositories
3. **Code Scanning**: Trigger code analysis on push events
4. **Retry Logic**: Handle failed repository updates
5. **Webhook Management UI**: Create interface to manage webhooks

## References

- [GitHub Webhooks Documentation](https://docs.github.com/en/developers/webhooks-and-events/webhooks/about-webhooks)
- [Securing Your Webhooks](https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks)
- [Webhook Research Document](../research/github-webhooks-research.md)

