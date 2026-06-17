package com.hallmark.enterprise.integration.papi.hmk_purchases.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hallmark.enterprise.integration.papi.hmk_purchases.exception.ErrorCodes;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.OrdersApi;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderListResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.service.PurchasesService;
import com.hallmark.integration.common.exception.IntegrationException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PurchasesController – Handling requests related to order searches
 *
 * <p>
 * This controller is responsible for managing order-related operations in the
 * application. It provides endpoints to retrieve orders by order number, phone number, or email address.
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@RestController
@RequestMapping("/purchases-papi/v1")
@Slf4j
@AllArgsConstructor
public class PurchasesController implements OrdersApi {

    private final PurchasesService purchasesService;
    
    @Autowired
    private ObjectMapper defaultObjectMapper;


    /**
     * Handles the request to retrieve orders by order number, phone number, or email address.
     *
     * @param orderNumber The order number to search for
     * @param phoneNumber The phone number associated with the order
     * @param email The email address associated with the order
     * @return A ResponseEntity containing the list of orders or error response
     */
    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<OrderListResponse> ordersSearch(String orderNumber, String phoneNumber, String emailAddress) {

        log.info("Received request to search orders - orderNumber: {}, phoneNumber: {}, email: {}",
                orderNumber, phoneNumber != null ? "***" : null, emailAddress);

        try {    
                // Validate that at least one parameter is provided
            if (orderNumber == null && phoneNumber == null && emailAddress == null) {
                log.error("Bad request: No search parameters provided");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Bad request");
                errorResponse.put("payload", null);
                ObjectMapper customMapper = defaultObjectMapper.copy();
                customMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

                String jsonResponse = customMapper.writeValueAsString(errorResponse);

                return (ResponseEntity<OrderListResponse>) (Object) ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jsonResponse);
             }

        
            OrderListResponse apiResponse;
            
            // Priority order matches MuleSoft implementation: phoneNumber > orderNumber > email
            if (phoneNumber != null) {
                log.debug("Searching by phone number");
                apiResponse = purchasesService.searchByPhoneNumber(phoneNumber);
            } else if (orderNumber != null) {
                log.debug("Searching by order number");
                apiResponse = purchasesService.searchByOrderNumber(orderNumber);
            } else {
                log.debug("Searching by email");
                apiResponse = purchasesService.searchByEmail(emailAddress);
            }

            log.info("Successfully retrieved order information");
            return ResponseEntity.ok(apiResponse);
            
        } catch (IntegrationException ex) {
            log.error("Business exception occurred: {} - {}", ex.getCode(), ex.getMessage(), ex);
            
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
            errorResponse.put("error", ex.getCode().getCode());
            errorResponse.put("payload", ex.getMessage());

            // Cast to Object first, then to OrderListResponse to bypass type checking
            return (ResponseEntity<OrderListResponse>) (Object) ResponseEntity
                    .status(status)
                    .body(errorResponse);
            
        } catch (Exception e) {
            log.error("Error processing order search request: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Error");
            errorResponse.put("payload", "Internal Error");
            
            // Cast to Object first, then to OrderListResponse to bypass type checking
            // This allows us to return the error Map in the ResponseEntity
            return (ResponseEntity<OrderListResponse>) (Object) ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}
