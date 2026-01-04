# GitHub Issue Webhook Setup

## Overview

The system supports **real-time bounty detection** via GitHub webhooks. When an issue is created or reopened in a tracked repository, GitHub sends a webhook notification immediately, allowing the system to detect bounties in real-time (no polling delay).

## Benefits

- **Real-time detection**: No 5-minute polling delay
- **More efficient**: Only processes new issues, not all issues
- **Better user experience**: Immediate notification when bounties appear
- **Reduced API calls**: Webhooks are push-based, not pull-based

## Webhook Endpoints

The system provides multiple webhook endpoints:

### 1. Unified Endpoint (Recommended)
```
POST /api/webhooks/github
```
Handles all event types (issues, push, ping) and routes them appropriately.

### 2. Issue-Specific Endpoint
```
POST /api/webhooks/github/issues
```
Specifically for issue events.

### 3. Push Event Endpoint
```
POST /api/webhooks/github/push
```
For push events (existing functionality).

## GitHub Webhook Configuration

### Step 1: Access Repository Settings

1. Go to your GitHub repository
2. Click **Settings** → **Webhooks**
3. Click **Add webhook**

### Step 2: Configure Webhook

**Payload URL:**
```
https://your-domain.com/api/webhooks/github
```
Or if using the issue-specific endpoint:
```
https://your-domain.com/api/webhooks/github/issues
```

**Content type:** `application/json`

**Secret:** Generate a secure random string and set it in `application.yml`:
```yaml
app:
  webhooks:
    github:
      secret: ${GITHUB_WEBHOOK_SECRET:your-secret-here}
```

**Which events would you like to trigger this webhook?**
- Select **"Let me select individual events"**
- Check **"Issues"** (required for bounty detection)
- Optionally check **"Pushes"** (for existing CVE commit analysis)
- Click **"Add webhook"**

### Step 3: Verify Webhook

GitHub will send a `ping` event to verify the webhook is working. Check your application logs:

```
Received ping event from GitHub. Delivery ID: <id>
```

## Webhook Security

The system verifies webhook signatures using HMAC-SHA256:

1. GitHub sends a signature in the `X-Hub-Signature-256` header
2. The system computes the expected signature using the secret
3. If signatures don't match, the request is rejected (401 Unauthorized)

**Important:** Always use HTTPS for webhook endpoints in production!

## Event Processing

### Issue Events

The system processes the following issue events:

- **`opened`**: New issue created → Check for bounty amount
- **`reopened`**: Closed issue reopened → Check for bounty amount
- **`closed`**: Issue closed → Ignored (no processing needed)
- **`edited`**: Issue edited → Currently ignored (could be enhanced in future)

### Processing Flow

```
GitHub Issue Created/Reopened
    ↓
Webhook → GitHubWebhookController
    ↓
Verify Signature
    ↓
GitHubWebhookService.processIssueEvent()
    ↓
GitHubIssueScannerService.processIssueFromWebhook()
    ↓
Extract Bounty Amount (regex pattern matching)
    ↓
If amount >= minimum ($50 default):
    ↓
Save to Database
    ↓
LLM Triage (BountyFilteringService)
    ↓
Enqueue for Processing (TriageQueueService)
```

## Testing Webhooks Locally

### Using ngrok (Recommended)

1. Install ngrok: https://ngrok.com/
2. Start your application locally
3. Expose local port:
   ```bash
   ngrok http 8080
   ```
4. Use the ngrok URL in GitHub webhook configuration:
   ```
   https://<ngrok-id>.ngrok.io/api/webhooks/github
   ```

### Manual Testing

You can test the webhook endpoint manually:

```bash
curl -X POST http://localhost:8080/api/webhooks/github/issues \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: issues" \
  -H "X-Hub-Signature-256: sha256=<signature>" \
  -d '{
    "action": "opened",
    "issue": {
      "number": 123,
      "title": "Fix bug - $100 bounty",
      "body": "This issue needs to be fixed. Offering $100 for the fix.",
      "state": "open"
    },
    "repository": {
      "full_name": "owner/repo",
      "html_url": "https://github.com/owner/repo"
    }
  }'
```

## Configuration

### application.yml

```yaml
app:
  webhooks:
    github:
      enabled: true
      secret: ${GITHUB_WEBHOOK_SECRET:}  # Required for production
      verify-signature: true
  bounty:
    github:
      enabled: true
      minimum-amount: 50.00
```

## Monitoring

### Logs

The system logs all webhook events:

- **INFO**: Successful processing
- **DEBUG**: Skipped events (closed issues, PRs, etc.)
- **WARN**: Invalid signatures or malformed events
- **ERROR**: Processing failures

### Health Check

The webhook endpoint has a health check:

```
GET /api/webhooks/github/health
```

Returns: `"GitHub webhook endpoint is active"`

## Troubleshooting

### Webhook Not Receiving Events

1. Check GitHub webhook delivery logs (Settings → Webhooks → Recent Deliveries)
2. Verify webhook URL is accessible
3. Check application logs for incoming requests
4. Verify signature secret matches

### Signature Verification Failing

1. Ensure `GITHUB_WEBHOOK_SECRET` environment variable is set
2. Verify secret in GitHub webhook settings matches application secret
3. Check logs for signature verification errors

### Issues Not Being Processed

1. Verify issue is open (not closed)
2. Check if issue contains dollar amount (e.g., "$50", "$100")
3. Verify amount meets minimum threshold (default: $50)
4. Check if issue is a pull request (PRs are ignored)
5. Review application logs for processing errors

## Best Practices

1. **Use HTTPS**: Always use HTTPS in production for webhook endpoints
2. **Rotate Secrets**: Periodically rotate webhook secrets
3. **Monitor Logs**: Set up log monitoring for webhook events
4. **Rate Limiting**: Consider rate limiting for webhook endpoints
5. **Idempotency**: The system checks for duplicate bounties before processing

## Fallback to Polling

Even with webhooks configured, the system still runs scheduled polling every 5 minutes as a fallback:

- Catches issues if webhook delivery fails
- Handles repositories without webhooks configured
- Provides redundancy for critical bounty detection

Both webhooks and polling work together for maximum coverage.

