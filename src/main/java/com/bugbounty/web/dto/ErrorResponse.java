package com.bugbounty.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response DTO for REST API errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String error;
    
    public ErrorResponse(String message) {
        this.message = message;
        this.error = message;
    }
}

