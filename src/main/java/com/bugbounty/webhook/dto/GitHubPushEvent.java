package com.bugbounty.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for GitHub push event webhook payload.
 * Represents the structure of a push event from GitHub webhooks.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPushEvent {
    
    @JsonProperty("ref")
    private String ref; // e.g., "refs/heads/main"
    
    @JsonProperty("repository")
    private Repository repository;
    
    @JsonProperty("commits")
    private List<Commit> commits;
    
    @JsonProperty("pusher")
    private Pusher pusher;
    
    @JsonProperty("head_commit")
    private Commit headCommit;
    
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
    public static class Commit {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("added")
        private List<String> added;
        
        @JsonProperty("modified")
        private List<String> modified;
        
        @JsonProperty("removed")
        private List<String> removed;
        
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("author")
        private Author author;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pusher {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
    }
    
    /**
     * Extract branch name from ref (e.g., "refs/heads/main" -> "main")
     */
    public String getBranchName() {
        if (ref == null || !ref.startsWith("refs/heads/")) {
            return null;
        }
        return ref.substring("refs/heads/".length());
    }
    
    /**
     * Check if this is a push to the default branch
     */
    public boolean isDefaultBranch() {
        if (repository == null || ref == null) {
            return false;
        }
        String defaultBranch = repository.getDefaultBranch();
        if (defaultBranch == null) {
            return false;
        }
        return ref.equals("refs/heads/" + defaultBranch);
    }
}

