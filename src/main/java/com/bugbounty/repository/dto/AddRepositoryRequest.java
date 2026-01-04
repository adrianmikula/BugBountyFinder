package com.bugbounty.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO for adding a new repository to monitor.
 */
@Data
public class AddRepositoryRequest {
    
    @NotBlank(message = "Repository URL is required")
    @Pattern(regexp = "https://github\\.com/[^/]+/[^/]+", 
             message = "URL must be a valid GitHub repository URL (e.g., https://github.com/owner/repo)")
    private String url;
    
    private String language; // Optional: will be auto-detected if not provided
    
    private String defaultBranch; // Optional: will be auto-detected if not provided
}

