package com.hallmark.enterprise.integration.papi.hmk_purchases.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hallmark.enterprise.integration.papi.hmk_purchases.exception.ErrorCodes;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderListResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.service.PurchasesService;
import com.hallmark.integration.common.exception.IntegrationException;

/**
 * Unit tests for PurchasesController
 *
 * Tests all public methods defined in PurchasesController:
 * - ordersSearch(String orderNumber, String phoneNumber, String email)
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchasesController Unit Tests")
class PurchasesControllerTest {

    @Mock
    private PurchasesService purchasesService;

    @InjectMocks
    private PurchasesController purchasesController;

    private OrderListResponse mockOrderListResponse;

    @BeforeEach
    void setUp() {
        // Create mock OrderListResponse for API responses
        mockOrderListResponse = new OrderListResponse(Collections.emptyList());
    }

    // ========================================
    // Tests for ordersSearch() method
    // ========================================

    @Test
    @DisplayName("Should successfully search by order number")
    void ordersSearch_WithOrderNumber_Success() {
        // Given
        String orderNumber = "ORD123456";
        when(purchasesService.searchByOrderNumber(orderNumber)).thenReturn(mockOrderListResponse);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(orderNumber, null, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockOrderListResponse, response.getBody());
        verify(purchasesService, times(1)).searchByOrderNumber(orderNumber);
        verify(purchasesService, never()).searchByPhoneNumber(anyString());
        verify(purchasesService, never()).searchByEmail(anyString());
    }

    @Test
    @DisplayName("Should successfully search by phone number")
    void ordersSearch_WithPhoneNumber_Success() {
        // Given
        String phoneNumber = "123-456-7890";
        when(purchasesService.searchByPhoneNumber(phoneNumber)).thenReturn(mockOrderListResponse);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(null, phoneNumber, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockOrderListResponse, response.getBody());
        verify(purchasesService, times(1)).searchByPhoneNumber(phoneNumber);
        verify(purchasesService, never()).searchByOrderNumber(anyString());
        verify(purchasesService, never()).searchByEmail(anyString());
    }

    @Test
    @DisplayName("Should successfully search by email")
    void ordersSearch_WithEmail_Success() {
        // Given
        String email = "test@example.com";
        when(purchasesService.searchByEmail(email)).thenReturn(mockOrderListResponse);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(null, null, email);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockOrderListResponse, response.getBody());
        verify(purchasesService, times(1)).searchByEmail(email);
        verify(purchasesService, never()).searchByOrderNumber(anyString());
        verify(purchasesService, never()).searchByPhoneNumber(anyString());
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when no parameters provided")
    void ordersSearch_NoParameters_ReturnsBadRequest() {
        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(null, null, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody()); // Body contains error map, not null
        verify(purchasesService, never()).searchByOrderNumber(anyString());
        verify(purchasesService, never()).searchByPhoneNumber(anyString());
        verify(purchasesService, never()).searchByEmail(anyString());
    }

    @Test
    @DisplayName("Should prioritize phone number when multiple parameters provided")
    void ordersSearch_MultipleParameters_PrioritizesPhoneNumber() {
        // Given
        String orderNumber = "ORD123456";
        String phoneNumber = "123-456-7890";
        String email = "test@example.com";
        when(purchasesService.searchByPhoneNumber(phoneNumber)).thenReturn(mockOrderListResponse);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(orderNumber, phoneNumber, email);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(purchasesService, times(1)).searchByPhoneNumber(phoneNumber);
        verify(purchasesService, never()).searchByOrderNumber(anyString());
        verify(purchasesService, never()).searchByEmail(anyString());
    }

    @Test
    @DisplayName("Should prioritize order number over email when both provided")
    void ordersSearch_OrderNumberAndEmail_PrioritizesOrderNumber() {
        // Given
        String orderNumber = "ORD123456";
        String email = "test@example.com";
        when(purchasesService.searchByOrderNumber(orderNumber)).thenReturn(mockOrderListResponse);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(orderNumber, null, email);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(purchasesService, times(1)).searchByOrderNumber(orderNumber);
        verify(purchasesService, never()).searchByPhoneNumber(anyString());
        verify(purchasesService, never()).searchByEmail(anyString());
    }

    @Test
    @DisplayName("Should return 404 when IntegrationException with ORDER_NOT_FOUND is thrown")
    void ordersSearch_ServiceThrowsIntegrationException_Returns404() {
        // Given
        String orderNumber = "ORD123456";
        IntegrationException exception = new IntegrationException(
                ErrorCodes.ORDER_NOT_FOUND, "service", "Order not found");
        when(purchasesService.searchByOrderNumber(orderNumber)).thenThrow(exception);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(orderNumber, null, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(purchasesService, times(1)).searchByOrderNumber(orderNumber);
    }

    @Test
    @DisplayName("Should return 500 when generic exception is thrown")
    void ordersSearch_ServiceThrowsGenericException_Returns500() {
        // Given
        String orderNumber = "ORD123456";
        RuntimeException exception = new RuntimeException("Unexpected error");
        when(purchasesService.searchByOrderNumber(orderNumber)).thenThrow(exception);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(orderNumber, null, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(purchasesService, times(1)).searchByOrderNumber(orderNumber);
    }

    @Test
    @DisplayName("Should prioritize phone number even when order number is empty string")
    void ordersSearch_EmptyStringOrderNumberWithPhone_PrioritizesPhone() {
        // Given
        String emptyOrderNumber = "";
        String phoneNumber = "123-456-7890";
        when(purchasesService.searchByPhoneNumber(phoneNumber)).thenReturn(mockOrderListResponse);

        // When
        ResponseEntity<OrderListResponse> response = purchasesController.ordersSearch(emptyOrderNumber, phoneNumber, null);

        // Then
        // Phone number has higher priority than order number, so searchByPhoneNumber is called
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockOrderListResponse, response.getBody());
        verify(purchasesService, times(1)).searchByPhoneNumber(phoneNumber);
        verify(purchasesService, never()).searchByOrderNumber(anyString());
        verify(purchasesService, never()).searchByEmail(anyString());
    }
}
