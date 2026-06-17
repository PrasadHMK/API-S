package com.hallmark.enterprise.integration.papi.hmk_purchases.exception;

import com.hallmark.integration.common.exception.ErrorCode;

/**
 * Error codes for the Purchases Process API
 *
 * <p>
 * This class defines standardized error codes used throughout the Purchases Process API.
 * Each error code maps to a specific HTTP status code and business error scenario.
 * </p>
 *
 * <p>
 * Error Code Mapping:
 * <ul>
 *   <li>PURCHASES_001 (BAD_REQUEST) → HTTP 400: Invalid request parameters</li>
 *   <li>PURCHASES_002 (ORDER_NOT_FOUND) → HTTP 404: Order not found</li>
 *   <li>PURCHASES_003 (INTERNAL_ERROR) → HTTP 500: Internal server error</li>
 *   <li>PURCHASES_004 (SERVICE_UNAVAILABLE) → HTTP 503: Downstream service unavailable</li>
 *   <li>PURCHASES_005 (INVALID_PARAMETER) → HTTP 400: Parameter validation failed</li>
 * </ul>
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
public final class ErrorCodes {
    
    private ErrorCodes() {
        // Utility class - prevent instantiation
    }

    /**
     * BAD_REQUEST (PURCHASES_001) - Invalid request
     * Maps to HTTP 400 Bad Request
     * Used when the request is malformed or contains invalid data
     */
    public static final ErrorCode BAD_REQUEST = ErrorCode.of("PURCHASES_001", "Bad Request");
    
    /**
     * ORDER_NOT_FOUND (PURCHASES_002) - Order not found
     * Maps to HTTP 404 Not Found
     * Used when the requested order does not exist in the system
     */
    public static final ErrorCode ORDER_NOT_FOUND = ErrorCode.of("PURCHASES_002", "Order Not Found");
    
    /**
     * INTERNAL_ERROR (PURCHASES_003) - Internal server error
     * Maps to HTTP 500 Internal Server Error
     * Used when an unexpected error occurs during request processing
     */
    public static final ErrorCode INTERNAL_ERROR = ErrorCode.of("PURCHASES_003", "Internal Error");
    
    /**
     * SERVICE_UNAVAILABLE (PURCHASES_004) - Service unavailable
     * Maps to HTTP 503 Service Unavailable
     * Used when a downstream service (Orders SAPI, ShipTrack, StoreInfo) is unavailable
     */
    public static final ErrorCode SERVICE_UNAVAILABLE = ErrorCode.of("PURCHASES_004", "Service Unavailable");
    
    /**
     * INVALID_PARAMETER (PURCHASES_005) - Invalid parameter
     * Maps to HTTP 400 Bad Request
     * Used when request parameter validation fails
     */
    public static final ErrorCode INVALID_PARAMETER = ErrorCode.of("PURCHASES_005", "Invalid Parameter");
}
