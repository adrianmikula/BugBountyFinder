package com.bugbounty.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repository {
    
    private static final Pattern GITHUB_URL_PATTERN = 
        Pattern.compile("(?:https?://|git@)github\\.com[:/]([^/]+)/([^/\\.]+)(?:\\.git)?");
    
    @Builder.Default
    private UUID id = UUID.randomUUID();
    
    private String url;
    private String owner;
    private String name;
    private String defaultBranch;
    private String language;
    private String localPath;
    
    public String getOwner() {
        if (owner != null) {
            return owner;
        }
        extractFromUrl();
        return owner;
    }
    
    public String getName() {
        if (name != null) {
            return name;
        }
        extractFromUrl();
        return name;
    }
    
    private void extractFromUrl() {
        if (url == null) {
            return;
        }
        
        Matcher matcher = GITHUB_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            this.owner = matcher.group(1);
            this.name = matcher.group(2);
        }
    }
    
    public boolean isCloned() {
        return localPath != null && !localPath.isEmpty();
    }
    
    public void markAsCloned(String localPath) {
        this.localPath = localPath;
    }
    
    public String getFullPath() {
        String ownerName = getOwner();
        String repoName = getName();
        if (ownerName != null && repoName != null) {
            return ownerName + "/" + repoName;
        }
        return null;
    }
}

