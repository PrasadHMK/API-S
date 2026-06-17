package com.hallmark.enterprise.integration.papi.hmk_purchases.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hallmark.integration.common.exception.IntegrationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the Purchases PAPI application.
 * 
 * <p>
 * This class handles exceptions and maps them to appropriate HTTP responses
 * matching the Mule API error format: {"error": "Internal Error", "payload": "Internal Error"}
 * </p>
 * 
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-09
 * @version 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle IntegrationException (business exceptions)
     * 
     * @param ex The exception instance
     * @return ResponseEntity containing error details in Mule format
     */
    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrationException(IntegrationException ex) {
        log.error("Integration exception: {} - {}", ex.getCode(), ex.getMessage(), ex);
        
        HttpStatus status;
        
        // Map error codes to HTTP status
        if (ErrorCodes.ORDER_NOT_FOUND.equals(ex.getCode())) {
            status = HttpStatus.NOT_FOUND;
        } else if (ErrorCodes.BAD_REQUEST.equals(ex.getCode()) || 
                   ErrorCodes.INVALID_PARAMETER.equals(ex.getCode())) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ErrorCodes.SERVICE_UNAVAILABLE.equals(ex.getCode())) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Error");
        errorResponse.put("payload", "Internal Error");
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle all other exceptions (system exceptions, Feign exceptions, etc.)
     * This includes FeignException when downstream order or store services fail.
     * 
     * @param ex The exception instance
     * @return ResponseEntity containing error details in Mule format
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Error");
        errorResponse.put("payload", "Internal Error");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
