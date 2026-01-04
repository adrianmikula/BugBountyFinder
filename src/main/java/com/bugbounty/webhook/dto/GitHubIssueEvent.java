package com.bugbounty.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for GitHub issue event webhook payload.
 * Represents the structure of an issue event from GitHub webhooks.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubIssueEvent {
    
    @JsonProperty("action")
    private String action; // "opened", "closed", "reopened", etc.
    
    @JsonProperty("issue")
    private Issue issue;
    
    @JsonProperty("repository")
    private Repository repository;
    
    @JsonProperty("sender")
    private Sender sender;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("number")
        private Integer number;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("body")
        private String body;
        
        @JsonProperty("state")
        private String state; // "open", "closed"
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("updated_at")
        private String updatedAt;
        
        @JsonProperty("user")
        private User user;
        
        @JsonProperty("labels")
        private java.util.List<Label> labels;
        
        @JsonProperty("pull_request")
        private PullRequest pullRequest; // Present if this is a PR, null if it's an issue
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("full_name")
        private String fullName; // e.g., "owner/repo-name"
        
        @JsonProperty("clone_url")
        private String cloneUrl;
        
        @JsonProperty("html_url")
        private String htmlUrl;
        
        @JsonProperty("default_branch")
        private String defaultBranch;
        
        @JsonProperty("private")
        private Boolean isPrivate;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty("login")
        private String login;
        
        @JsonProperty("id")
        private Long id;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Label {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("color")
        private String color;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("html_url")
        private String htmlUrl;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {
        @JsonProperty("login")
        private String login;
        
        @JsonProperty("id")
        private Long id;
    }
    
    /**
     * Check if this is an issue (not a pull request).
     */
    public boolean isIssue() {
        return issue != null && issue.getPullRequest() == null;
    }
    
    /**
     * Check if this is a pull request.
     */
    public boolean isPullRequest() {
        return issue != null && issue.getPullRequest() != null;
    }
    
    /**
     * Check if the issue is open.
     */
    public boolean isOpen() {
        return issue != null && "open".equals(issue.getState());
    }
    
    /**
     * Get the repository URL in standard format.
     */
    public String getRepositoryUrl() {
        if (repository == null || repository.getFullName() == null) {
            return null;
        }
        return "https://github.com/" + repository.getFullName();
    }
}

