package com.bugbounty.webhook.controller;

import com.bugbounty.webhook.dto.GitHubIssueEvent;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import com.bugbounty.webhook.service.GitHubWebhookService;
import com.bugbounty.webhook.service.WebhookSignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling GitHub webhook events.
 * Receives webhook notifications from GitHub and processes them.
 */
@RestController
@RequestMapping("/api/webhooks/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookController {
    
    private static final String GITHUB_EVENT_HEADER = "X-GitHub-Event";
    private static final String GITHUB_SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String GITHUB_DELIVERY_HEADER = "X-GitHub-Delivery";
    
    private final WebhookSignatureService signatureService;
    private final GitHubWebhookService webhookService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handle GitHub push events.
     * 
     * @param eventType The event type from X-GitHub-Event header
     * @param signature The signature from X-Hub-Signature-256 header
     * @param deliveryId The delivery ID from X-GitHub-Delivery header
     * @param payload The raw JSON payload as string
     * @return HTTP response
     */
    @PostMapping("/push")
    public ResponseEntity<String> handlePushEvent(
            @RequestHeader(value = GITHUB_EVENT_HEADER, required = false) String eventType,
            @RequestHeader(value = GITHUB_SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = GITHUB_DELIVERY_HEADER, required = false) String deliveryId,
            @RequestBody String payload) {
        
        log.debug("Received webhook event - Type: {}, Delivery: {}", eventType, deliveryId);
        
        // Verify event type
        if (!"push".equals(eventType)) {
            log.warn("Received non-push event: {}", eventType);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Expected 'push' event, received: " + eventType);
        }
        
        // Verify signature
        if (!signatureService.verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature. Delivery ID: {}", deliveryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        }
        
        try {
            // Parse payload
            GitHubPushEvent pushEvent = objectMapper.readValue(payload, GitHubPushEvent.class);
            
            log.info("Processing push event for repository: {} (Delivery: {})",
                    pushEvent.getRepository() != null ? pushEvent.getRepository().getFullName() : "unknown",
                    deliveryId);
            
            // Process the push event
            boolean processed = webhookService.processPushEvent(pushEvent);
            
            if (processed) {
                return ResponseEntity.ok("Webhook processed successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to process webhook");
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload. Delivery ID: {}", deliveryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for webhook configuration.
     * GitHub can use this to verify the webhook endpoint is reachable.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GitHub webhook endpoint is active");
    }
    
    /**
     * Handle GitHub issue events (opened, closed, etc.).
     * This is the primary endpoint for real-time bounty detection.
     * 
     * @param eventType The event type from X-GitHub-Event header
     * @param signature The signature from X-Hub-Signature-256 header
     * @param deliveryId The delivery ID from X-GitHub-Delivery header
     * @param payload The raw JSON payload as string
     * @return HTTP response
     */
    @PostMapping("/issues")
    public ResponseEntity<String> handleIssueEvent(
            @RequestHeader(value = GITHUB_EVENT_HEADER, required = false) String eventType,
            @RequestHeader(value = GITHUB_SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = GITHUB_DELIVERY_HEADER, required = false) String deliveryId,
            @RequestBody String payload) {
        
        log.debug("Received webhook event - Type: {}, Delivery: {}", eventType, deliveryId);
        
        // Verify event type
        if (!"issues".equals(eventType)) {
            log.warn("Received non-issues event: {}", eventType);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Expected 'issues' event, received: " + eventType);
        }
        
        // Verify signature
        if (!signatureService.verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature. Delivery ID: {}", deliveryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        }
        
        try {
            // Parse payload
            GitHubIssueEvent issueEvent = objectMapper.readValue(payload, GitHubIssueEvent.class);
            
            log.info("Processing issue event - Action: {}, Repository: {}, Issue #{} (Delivery: {})",
                    issueEvent.getAction(),
                    issueEvent.getRepository() != null ? issueEvent.getRepository().getFullName() : "unknown",
                    issueEvent.getIssue() != null ? issueEvent.getIssue().getNumber() : "unknown",
                    deliveryId);
            
            // Process the issue event
            boolean processed = webhookService.processIssueEvent(issueEvent);
            
            if (processed) {
                return ResponseEntity.ok("Webhook processed successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to process webhook");
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload. Delivery ID: {}", deliveryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
    
    /**
     * Unified webhook endpoint that handles multiple event types.
     * GitHub can be configured to send all events to this endpoint.
     * 
     * @param eventType The event type from X-GitHub-Event header
     * @param signature The signature from X-Hub-Signature-256 header
     * @param deliveryId The delivery ID from X-GitHub-Delivery header
     * @param payload The raw JSON payload as string
     * @return HTTP response
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = GITHUB_EVENT_HEADER, required = false) String eventType,
            @RequestHeader(value = GITHUB_SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = GITHUB_DELIVERY_HEADER, required = false) String deliveryId,
            @RequestBody String payload) {
        
        log.debug("Received webhook event - Type: {}, Delivery: {}", eventType, deliveryId);
        
        // Verify signature
        if (!signatureService.verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature. Delivery ID: {}", deliveryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        }
        
        // Route to appropriate handler based on event type
        if ("issues".equals(eventType)) {
            return handleIssueEvent(eventType, signature, deliveryId, payload);
        } else if ("push".equals(eventType)) {
            return handlePushEvent(eventType, signature, deliveryId, payload);
        } else if ("ping".equals(eventType)) {
            log.info("Received ping event from GitHub. Delivery ID: {}", deliveryId);
            return ResponseEntity.ok("Pong");
        } else {
            log.debug("Unhandled event type: {}. Delivery ID: {}", eventType, deliveryId);
            return ResponseEntity.ok("Event received but not processed");
        }
    }
    
    /**
     * Handle ping events from GitHub (webhook test).
     */
    @PostMapping("/ping")
    public ResponseEntity<String> handlePing(
            @RequestHeader(value = GITHUB_EVENT_HEADER, required = false) String eventType,
            @RequestHeader(value = GITHUB_SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = GITHUB_DELIVERY_HEADER, required = false) String deliveryId,
            @RequestBody String payload) {
        
        log.info("Received ping event from GitHub. Delivery ID: {}", deliveryId);
        
        // Verify signature even for ping events
        if (!signatureService.verifySignature(payload, signature)) {
            log.warn("Invalid signature for ping event. Delivery ID: {}", deliveryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        }
        
        return ResponseEntity.ok("Pong");
    }
}

