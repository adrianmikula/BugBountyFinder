# GitHub Webhooks Research: Real-Time Commit Notifications

## Executive Summary

**Yes, we can create hooks to listen for GitHub push notifications when commits are pushed to repositories.** GitHub provides webhooks that allow real-time event notifications, which is more efficient than polling and enables immediate code scanning.

## Overview

### What are GitHub Webhooks?

GitHub webhooks are HTTP callbacks that GitHub sends to your application when specific events occur in a repository. For commit monitoring, the `push` event is the most relevant.

**Key Benefits:**
- **Real-time notifications**: Immediate notification when commits are pushed
- **Efficient**: No need to poll APIs repeatedly
- **Event-driven**: Only processes actual changes
- **Scalable**: Can handle multiple repositories simultaneously

### Current Architecture

Our application currently uses:
- **Polling-based approach**: `BountyPollingService` polls APIs at intervals (5 minutes)
- **Repository management**: `RepositoryService` can clone and update repositories
- **Spring Boot**: Web framework with REST capabilities already in place

## Implementation Approach

### 1. Webhook Endpoint Structure

GitHub webhooks require:
- **Publicly accessible URL**: Your application must be reachable from the internet
- **POST endpoint**: Receives JSON payloads from GitHub
- **Secret verification**: HMAC-SHA256 signature verification for security
- **Event handling**: Process different event types (push, pull_request, etc.)

### 2. Spring Boot Implementation

#### Required Dependencies

Already available in `build.gradle.kts`:
- `spring-boot-starter-web` ✅ (REST controllers)
- `spring-boot-starter-validation` ✅ (request validation)

No additional dependencies needed!

#### Webhook Controller Structure

```java
@RestController
@RequestMapping("/api/webhooks/github")
public class GitHubWebhookController {
    
    @PostMapping("/push")
    public ResponseEntity<?> handlePushEvent(
        @RequestHeader("X-GitHub-Event") String eventType,
        @RequestHeader("X-Hub-Signature-256") String signature,
        @RequestBody String payload
    ) {
        // 1. Verify webhook secret
        // 2. Parse payload
        // 3. Process push event
        // 4. Trigger code scanning
    }
}
```

### 3. Security: Webhook Secret Verification

**Critical**: Always verify webhook signatures to prevent unauthorized requests.

```java
private boolean verifySignature(String payload, String signature, String secret) {
    String expectedSignature = "sha256=" + 
        HmacUtils.hmacSha256Hex(secret, payload);
    return MessageDigest.isEqual(
        expectedSignature.getBytes(),
        signature.getBytes()
    );
}
```

**Configuration:**
- Store webhook secret in environment variable: `GITHUB_WEBHOOK_SECRET`
- Configure in GitHub repository settings
- Never commit secrets to repository

### 4. Push Event Payload Structure

GitHub push event payload includes:
```json
{
  "ref": "refs/heads/main",
  "repository": {
    "id": 123456,
    "name": "repo-name",
    "full_name": "owner/repo-name",
    "clone_url": "https://github.com/owner/repo-name.git",
    "html_url": "https://github.com/owner/repo-name"
  },
  "commits": [
    {
      "id": "abc123",
      "message": "Fix bug",
      "added": ["file1.java"],
      "modified": ["file2.java"],
      "removed": ["file3.java"],
      "timestamp": "2024-01-01T12:00:00Z"
    }
  ],
  "pusher": {
    "name": "username"
  }
}
```

### 5. Integration with Existing Services

**Workflow:**
1. **Webhook receives push event** → `GitHubWebhookController`
2. **Verify signature** → Security check
3. **Parse payload** → Extract repository info and commits
4. **Check if repository is tracked** → Query `Repository` domain
5. **Update repository** → Call `RepositoryService.updateRepository()`
6. **Scan new code** → Trigger code analysis service (future)
7. **Process changes** → Analyze diffs, detect issues

**Integration Points:**
- `RepositoryService`: Already has `updateRepository()` method
- `GitOperations`: Can pull latest changes
- Future: Code scanning service to analyze new commits

## Comparison: Webhooks vs Polling

### Current Polling Approach
- ✅ Simple to implement
- ✅ No external dependencies
- ❌ Delayed detection (5-minute intervals)
- ❌ Wastes API rate limits
- ❌ Doesn't scale well

### Webhook Approach
- ✅ Real-time notifications
- ✅ Efficient (only processes actual events)
- ✅ Better API rate limit usage
- ✅ Scales to many repositories
- ❌ Requires public endpoint
- ❌ More complex security setup
- ❌ Requires webhook configuration per repository

### Recommendation: Hybrid Approach

1. **Use webhooks for tracked repositories** (repositories we're actively monitoring)
2. **Use polling as fallback** (if webhook fails or repository doesn't have webhook configured)
3. **Health checks**: Monitor webhook delivery status

## Implementation Steps

### Phase 1: Basic Webhook Endpoint
1. Create `GitHubWebhookController`
2. Implement signature verification
3. Parse push event payload
4. Log events for debugging

### Phase 2: Repository Integration
1. Match webhook events to tracked repositories
2. Call `RepositoryService.updateRepository()`
3. Handle repository not found scenarios

### Phase 3: Code Scanning Trigger
1. Extract changed files from commit
2. Trigger code analysis for new/modified files
3. Store scan results

### Phase 4: Production Readiness
1. Add webhook secret management
2. Implement retry logic for failed updates
3. Add monitoring and alerting
4. Handle webhook delivery failures

## Configuration Requirements

### Application Configuration

Add to `application.yml`:
```yaml
app:
  webhooks:
    github:
      enabled: true
      secret: ${GITHUB_WEBHOOK_SECRET:}
      path: /api/webhooks/github
      verify-signature: true
```

### GitHub Repository Setup

For each repository to monitor:
1. Go to **Settings** → **Webhooks**
2. Click **Add webhook**
3. **Payload URL**: `https://your-domain.com/api/webhooks/github/push`
4. **Content type**: `application/json`
5. **Secret**: Generate and store securely
6. **Events**: Select "Just the push event"
7. **Active**: ✅ Enabled

### Public Endpoint Requirements

**Options for exposing webhook endpoint:**

1. **Direct Public IP** (Development)
   - Use ngrok or similar tunnel: `ngrok http 8080`
   - Good for testing, not production

2. **Cloud Deployment** (Production)
   - Deploy to AWS, Azure, GCP
   - Use load balancer with public IP
   - Configure HTTPS (required by GitHub)

3. **Reverse Proxy** (Production)
   - Use nginx/traefik as reverse proxy
   - Handle SSL termination
   - Route to Spring Boot app

## Security Considerations

### 1. Webhook Secret Verification
- **Always verify signatures** before processing
- Use constant-time comparison to prevent timing attacks
- Store secret securely (environment variable, secrets manager)

### 2. Rate Limiting
- Implement rate limiting on webhook endpoint
- Prevent abuse from unauthorized sources
- Use Spring Security or Resilience4j

### 3. Input Validation
- Validate payload structure
- Sanitize repository URLs
- Check repository ownership/permissions

### 4. Error Handling
- Don't expose internal errors to GitHub
- Log errors securely
- Return appropriate HTTP status codes

## Testing Strategy

### Unit Tests
- Test signature verification logic
- Test payload parsing
- Test error handling

### Integration Tests
- Test webhook endpoint with mock GitHub payloads
- Verify repository update integration
- Test security scenarios

### Manual Testing
- Use GitHub's webhook testing feature
- Use tools like `ngrok` for local testing
- Verify webhook delivery in GitHub UI

## Monitoring and Observability

### Metrics to Track
- Webhook delivery success rate
- Processing latency
- Repository update failures
- Signature verification failures

### Logging
- Log all webhook events (sanitized)
- Log processing results
- Alert on repeated failures

## Limitations and Considerations

### GitHub Webhook Limitations
- **Delivery guarantees**: GitHub attempts delivery but doesn't guarantee
- **Retry logic**: GitHub retries failed deliveries (exponential backoff)
- **Rate limits**: Webhook delivery has rate limits
- **Payload size**: Large commits may have truncated payloads

### Our Application Limitations
- **Public endpoint required**: Application must be publicly accessible
- **HTTPS required**: GitHub requires HTTPS for webhooks
- **Per-repository setup**: Each repository needs webhook configuration
- **Secret management**: Need secure secret storage

## Alternative Approaches

### 1. GitHub App (More Advanced)
- OAuth-based authentication
- More granular permissions
- Better for multi-repository scenarios
- More complex setup

### 2. GitHub Actions (Repository-Side)
- Trigger actions on push
- Can call external APIs
- Requires repository access
- Good for repository owners

### 3. Polling with Webhooks (Hybrid)
- Use webhooks when available
- Fall back to polling
- Best of both worlds

## Recommendations

### Immediate Actions
1. ✅ **Research complete** - Webhooks are feasible
2. ⏭️ **Create webhook controller** - Basic endpoint structure
3. ⏭️ **Implement signature verification** - Security first
4. ⏭️ **Integrate with RepositoryService** - Connect to existing code

### Future Enhancements
1. Add webhook management UI (register/unregister repositories)
2. Implement webhook delivery monitoring
3. Add retry logic for failed repository updates
4. Create code scanning service triggered by webhooks

## References

- [GitHub Webhooks Documentation](https://docs.github.com/en/developers/webhooks-and-events/webhooks/about-webhooks)
- [GitHub Push Event Payload](https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#push)
- [Securing Your Webhooks](https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks)
- [Spring Boot REST Controllers](https://spring.io/guides/gs/rest-service/)

## Conclusion

**GitHub webhooks are a viable solution** for real-time commit monitoring. They provide immediate notifications when code is pushed, enabling faster response times for code scanning compared to polling.

**Key Advantages:**
- Real-time event notifications
- Efficient resource usage
- Better scalability
- Industry-standard approach

**Implementation is straightforward** with Spring Boot, and we already have the necessary dependencies. The main requirements are:
1. Public endpoint (for production)
2. Webhook secret management
3. Signature verification
4. Integration with existing repository services

**Next Steps:** Proceed with Phase 1 implementation - create basic webhook endpoint with signature verification.

