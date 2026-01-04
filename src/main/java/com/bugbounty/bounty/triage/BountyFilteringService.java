package com.bugbounty.bounty.triage;

import com.bugbounty.bounty.domain.Bounty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BountyFilteringService {

    private static final double DEFAULT_MIN_CONFIDENCE = 0.6;
    private static final int DEFAULT_MAX_TIME_MINUTES = 60;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public FilterResult shouldProcess(Bounty bounty) {
        return shouldProcess(bounty, DEFAULT_MIN_CONFIDENCE, DEFAULT_MAX_TIME_MINUTES);
    }

    public FilterResult shouldProcess(Bounty bounty, double minConfidence) {
        return shouldProcess(bounty, minConfidence, DEFAULT_MAX_TIME_MINUTES);
    }

    public FilterResult shouldProcess(Bounty bounty, double minConfidence, int maxTimeMinutes) {
        try {
            log.debug("Filtering bounty: {} - {}", bounty.getIssueId(), bounty.getTitle());

            String promptText = buildPrompt(bounty);
            Prompt aiPrompt = new Prompt(new UserMessage(promptText));

            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();

            FilterResult result = parseResponse(content);

            // Apply thresholds
            if (result.confidence() < minConfidence) {
                log.debug("Bounty {} rejected: confidence {} below threshold {}", 
                        bounty.getIssueId(), result.confidence(), minConfidence);
                return new FilterResult(false, result.confidence(), result.estimatedTimeMinutes(), 
                        result.reason() + " (confidence too low)");
            }

            if (result.estimatedTimeMinutes() > maxTimeMinutes) {
                log.debug("Bounty {} rejected: estimated time {} exceeds threshold {}", 
                        bounty.getIssueId(), result.estimatedTimeMinutes(), maxTimeMinutes);
                return new FilterResult(false, result.confidence(), result.estimatedTimeMinutes(), 
                        result.reason() + " (time estimate too high)");
            }

            log.info("Bounty {} accepted: confidence={}, time={}min", 
                    bounty.getIssueId(), result.confidence(), result.estimatedTimeMinutes());
            return result;

        } catch (Exception e) {
            log.error("Error filtering bounty: {}", bounty.getIssueId(), e);
            // Fail-safe: reject on error
            return new FilterResult(false, 0.0, 0, "Error during filtering: " + e.getMessage());
        }
    }

    private String buildPrompt(Bounty bounty) {
        return """
                Analyze this bug bounty and determine if it should be processed.
                
                Bounty Details:
                - Issue ID: {issueId}
                - Repository: {repositoryUrl}
                - Platform: {platform}
                - Amount: {amount} {currency}
                - Title: {title}
                - Description: {description}
                
                Consider:
                1. Is the issue clearly described and fixable?
                2. Can it be fixed in under 60 minutes?
                3. Is the bounty amount worth the effort?
                4. Is there enough information to proceed?
                5. Is it a simple bug fix vs. a complex refactoring?
                
                Respond with a JSON object:
                {{
                  "shouldProcess": true/false,
                  "confidence": 0.0-1.0,
                  "estimatedTimeMinutes": number,
                  "reason": "brief explanation"
                }}
                """.replace("{issueId}", bounty.getIssueId())
                .replace("{repositoryUrl}", bounty.getRepositoryUrl() != null ? bounty.getRepositoryUrl() : "N/A")
                .replace("{platform}", bounty.getPlatform())
                .replace("{amount}", bounty.getAmount() != null ? bounty.getAmount().toString() : "N/A")
                .replace("{currency}", bounty.getCurrency() != null ? bounty.getCurrency() : "USD")
                .replace("{title}", bounty.getTitle() != null ? bounty.getTitle() : "N/A")
                .replace("{description}", bounty.getDescription() != null ? bounty.getDescription() : "N/A");
    }

    private FilterResult parseResponse(String content) {
        try {
            // Extract JSON from response (might have markdown code blocks)
            String jsonContent = content.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            }
            if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();

            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean shouldProcess = node.get("shouldProcess").asBoolean();
            double confidence = node.get("confidence").asDouble();
            int estimatedTime = node.get("estimatedTimeMinutes").asInt();
            String reason = node.has("reason") ? node.get("reason").asText() : "";

            return new FilterResult(shouldProcess, confidence, estimatedTime, reason);
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", content, e);
            return new FilterResult(false, 0.0, 0, "Failed to parse LLM response");
        }
    }
}

