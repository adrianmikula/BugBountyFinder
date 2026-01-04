package com.bugbounty.webhook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Service for verifying GitHub webhook signatures.
 * Uses HMAC-SHA256 to verify that webhook payloads are authentic.
 */
@Service
@Slf4j
public class WebhookSignatureService {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    
    private final String webhookSecret;
    
    public WebhookSignatureService(
            @Value("${app.webhooks.github.secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
    
    /**
     * Verify the GitHub webhook signature.
     * 
     * @param payload The raw request body as string
     * @param signature The X-Hub-Signature-256 header value
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook secret not configured. Signature verification disabled.");
            return true; // Allow if secret not configured (for development)
        }
        
        if (signature == null || signature.isEmpty()) {
            log.warn("Missing webhook signature header");
            return false;
        }
        
        if (!signature.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Invalid signature format. Expected 'sha256=' prefix");
            return false;
        }
        
        try {
            String expectedSignature = calculateSignature(payload);
            String receivedSignature = signature.substring(SIGNATURE_PREFIX.length());
            
            // Use constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedSignature, receivedSignature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Calculate HMAC-SHA256 signature for the payload.
     */
    private String calculateSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
}

