package com.bugbounty.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookSignatureService Tests")
class WebhookSignatureServiceTest {

    private WebhookSignatureService signatureService;
    private static final String TEST_SECRET = "test-secret-key";
    private static final String TEST_PAYLOAD = "{\"test\": \"data\"}";

    @BeforeEach
    void setUp() {
        // Create service with test secret
        signatureService = new WebhookSignatureService(TEST_SECRET);
    }

    @Test
    @DisplayName("Should verify valid signature")
    void shouldVerifyValidSignature() {
        // Given - We need to calculate a valid signature first
        // For this test, we'll use a known good signature
        // In practice, we'd calculate this using the same HMAC algorithm
        String validSignature = "sha256=" + calculateTestSignature(TEST_PAYLOAD, TEST_SECRET);
        
        // When
        boolean isValid = signatureService.verifySignature(TEST_PAYLOAD, validSignature);
        
        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid signature")
    void shouldRejectInvalidSignature() {
        // Given
        String invalidSignature = "sha256=invalid_signature_hash";
        
        // When
        boolean isValid = signatureService.verifySignature(TEST_PAYLOAD, invalidSignature);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject signature with wrong prefix")
    void shouldRejectSignatureWithWrongPrefix() {
        // Given
        String wrongPrefix = "sha1=some_hash";
        
        // When
        boolean isValid = signatureService.verifySignature(TEST_PAYLOAD, wrongPrefix);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject null signature")
    void shouldRejectNullSignature() {
        // When
        boolean isValid = signatureService.verifySignature(TEST_PAYLOAD, null);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject empty signature")
    void shouldRejectEmptySignature() {
        // When
        boolean isValid = signatureService.verifySignature(TEST_PAYLOAD, "");
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should allow when secret is not configured")
    void shouldAllowWhenSecretNotConfigured() {
        // Given
        WebhookSignatureService serviceWithoutSecret = new WebhookSignatureService("");
        
        // When
        boolean isValid = serviceWithoutSecret.verifySignature(TEST_PAYLOAD, "any-signature");
        
        // Then
        assertTrue(isValid); // Should allow when secret not configured (development mode)
    }

    @Test
    @DisplayName("Should handle different payloads correctly")
    void shouldHandleDifferentPayloads() {
        // Given
        String payload1 = "{\"event\": \"push\"}";
        String payload2 = "{\"event\": \"pull_request\"}";
        
        String signature1 = "sha256=" + calculateTestSignature(payload1, TEST_SECRET);
        String signature2 = "sha256=" + calculateTestSignature(payload2, TEST_SECRET);
        
        // When & Then
        assertTrue(signatureService.verifySignature(payload1, signature1));
        assertTrue(signatureService.verifySignature(payload2, signature2));
        assertFalse(signatureService.verifySignature(payload1, signature2)); // Wrong signature for payload1
    }

    @Test
    @DisplayName("Should handle signature with different length")
    void shouldHandleSignatureWithDifferentLength() {
        // Given
        String shortSignature = "sha256=abc";
        String longSignature = "sha256=" + "a".repeat(100);
        
        // When & Then
        assertFalse(signatureService.verifySignature(TEST_PAYLOAD, shortSignature));
        assertFalse(signatureService.verifySignature(TEST_PAYLOAD, longSignature));
    }

    @Test
    @DisplayName("Should handle signature without sha256 prefix")
    void shouldHandleSignatureWithoutPrefix() {
        // Given
        String signatureWithoutPrefix = calculateTestSignature(TEST_PAYLOAD, TEST_SECRET);
        
        // When
        boolean isValid = signatureService.verifySignature(TEST_PAYLOAD, signatureWithoutPrefix);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty payload")
    void shouldHandleEmptyPayload() {
        // Given
        String emptyPayload = "";
        String signature = "sha256=" + calculateTestSignature(emptyPayload, TEST_SECRET);
        
        // When
        boolean isValid = signatureService.verifySignature(emptyPayload, signature);
        
        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle large payload")
    void shouldHandleLargePayload() {
        // Given
        String largePayload = "{\"data\": \"" + "x".repeat(10000) + "\"}";
        String signature = "sha256=" + calculateTestSignature(largePayload, TEST_SECRET);
        
        // When
        boolean isValid = signatureService.verifySignature(largePayload, signature);
        
        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should handle null secret")
    void shouldHandleNullSecret() {
        // Given
        WebhookSignatureService serviceWithNullSecret = new WebhookSignatureService(null);
        
        // When
        boolean isValid = serviceWithNullSecret.verifySignature(TEST_PAYLOAD, "any-signature");
        
        // Then
        assertTrue(isValid); // Should allow when secret is null (development mode)
    }

    /**
     * Helper method to calculate HMAC-SHA256 signature for testing.
     * This mirrors the implementation in WebhookSignatureService.
     */
    private String calculateTestSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }
}

