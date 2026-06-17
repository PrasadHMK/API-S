package com.hallmark.enterprise.integration.papi.hmk_purchases.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrderListByEmailFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrderListByPhoneFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrdersOmniManhFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.Order;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderList;
import com.hallmark.enterprise.integration.papi.hmk_purchases.exception.ErrorCodes;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderListResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.service.transformer.OrderTransformer;
import com.hallmark.integration.common.exception.IntegrationException;

/**
 * Unit tests for PurchasesService
 *
 * Tests all public methods defined in PurchasesService:
 * - searchByOrderNumber(String orderNumber)
 * - searchByPhoneNumber(String phoneNumber)
 * - searchByEmail(String email)
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchasesService Unit Tests")
class PurchasesServiceTest {

    @Mock
    private OrdersOmniManhFeignClient ordersClient;

    @Mock
    private OrderListByEmailFeignClient orderListByEmailClient;

    @Mock
    private OrderListByPhoneFeignClient orderListByPhoneClient;

    @Mock
    private OrderTransformer orderTransformer;

    @InjectMocks
    private PurchasesService purchasesService;

    private Order mockOrder;
    private OrderList mockOrderList;
    private OrderListResponse mockTransformedResponse;

    @BeforeEach
    void setUp() {
        // Setup mock Order (spec model for single order endpoint)
        mockOrder = new Order();
        
        // Setup mock OrderList (spec model for list endpoints)
        mockOrderList = new OrderList();
        mockOrderList.setCount(2);

        // Setup mock transformed response (server spec model)
        mockTransformedResponse = new OrderListResponse();
        mockTransformedResponse.setOrders(Collections.emptyList());
    }

    // ========================================
    // Tests for searchByOrderNumber() method
    // ========================================

    @Test
    @DisplayName("searchByOrderNumber - Should successfully search order by order number")
    void searchByOrderNumber_ValidOrderNumber_ReturnsOrderResponse() {
        // Given
        String orderNumber = "ORD123456";
        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById(orderNumber);
        when(orderTransformer.transformToOrderListResponse(mockOrder)).thenReturn(mockTransformedResponse);

        // When
        OrderListResponse result = purchasesService.searchByOrderNumber(orderNumber);

        // Then
        assertNotNull(result);
        assertEquals(mockTransformedResponse, result);
        verify(ordersClient, times(1)).getOrderById(orderNumber);
        verify(orderTransformer, times(1)).transformToOrderListResponse(mockOrder);
    }

    @Test
    @DisplayName("searchByOrderNumber - Should throw exception when response is null")
    void searchByOrderNumber_NullResponse_ThrowsIntegrationException() {
        // Given
        String orderNumber = "ORD123456";
        doReturn(ResponseEntity.ok(null)).when(ordersClient).getOrderById(orderNumber);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByOrderNumber(orderNumber);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(ordersClient, times(1)).getOrderById(orderNumber);
        verify(orderTransformer, never()).transformToOrderListResponse(any());
    }

    @Test
    @DisplayName("searchByOrderNumber - Should throw exception when order field is null")
    void searchByOrderNumber_NullOrderField_ThrowsIntegrationException() {
        // Given
        String orderNumber = "ORD123456";
        doReturn(ResponseEntity.ok(null)).when(ordersClient).getOrderById(orderNumber);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByOrderNumber(orderNumber);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(ordersClient, times(1)).getOrderById(orderNumber);
        verify(orderTransformer, never()).transformToOrderListResponse(any());
    }

    @Test
    @DisplayName("searchByOrderNumber - Should propagate generic exception")
    void searchByOrderNumber_GenericException_ThrowsRuntimeException() {
        // Given
        String orderNumber = "ORD123456";
        when(ordersClient.getOrderById(orderNumber)).thenThrow(new RuntimeException("API Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchasesService.searchByOrderNumber(orderNumber);
        });

        assertEquals("API Error", exception.getMessage());
        verify(ordersClient, times(1)).getOrderById(orderNumber);
    }

    @Test
    @DisplayName("searchByOrderNumber - Should propagate IntegrationException from client")
    void searchByOrderNumber_IntegrationExceptionFromClient_PropagatesException() {
        // Given
        String orderNumber = "ORD123456";
        IntegrationException clientException = new IntegrationException(
                ErrorCodes.ORDER_NOT_FOUND, "client", "Order not found in system");
        when(ordersClient.getOrderById(orderNumber)).thenThrow(clientException);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByOrderNumber(orderNumber);
        });

        assertEquals(clientException, exception);
        verify(ordersClient, times(1)).getOrderById(orderNumber);
    }

    // ========================================
    // Tests for searchByPhoneNumber() method
    // ========================================

    @Test
    @DisplayName("searchByPhoneNumber - Should successfully search orders by phone number")
    void searchByPhoneNumber_ValidPhoneNumber_ReturnsOrderResponse() {
        // Given
        String phoneNumber = "123-456-7890";

        doReturn(ResponseEntity.ok(mockOrderList)).when(orderListByPhoneClient).getOrderListByPhone(phoneNumber);
        when(orderTransformer.transformToOrderListResponseFromList(mockOrderList))
                .thenReturn(mockTransformedResponse);

        // When
        OrderListResponse result = purchasesService.searchByPhoneNumber(phoneNumber);

        // Then
        assertNotNull(result);
        assertEquals(mockTransformedResponse, result);
        verify(orderListByPhoneClient, times(1)).getOrderListByPhone(phoneNumber);
        verify(orderTransformer, times(1)).transformToOrderListResponseFromList(mockOrderList);
    }

    @Test
    @DisplayName("searchByPhoneNumber - Should throw exception when response is null")
    void searchByPhoneNumber_NullResponse_ThrowsIntegrationException() {
        // Given
        String phoneNumber = "123-456-7890";
        doReturn(ResponseEntity.ok(null)).when(orderListByPhoneClient).getOrderListByPhone(phoneNumber);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByPhoneNumber(phoneNumber);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(orderListByPhoneClient, times(1)).getOrderListByPhone(phoneNumber);
    }

    @Test
    @DisplayName("searchByPhoneNumber - Should throw exception when count is zero")
    void searchByPhoneNumber_ZeroCount_ThrowsIntegrationException() {
        // Given
        String phoneNumber = "123-456-7890";
        OrderList emptyOrderList = new OrderList();
        emptyOrderList.setCount(0);

        doReturn(ResponseEntity.ok(emptyOrderList)).when(orderListByPhoneClient).getOrderListByPhone(phoneNumber);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByPhoneNumber(phoneNumber);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(orderListByPhoneClient, times(1)).getOrderListByPhone(phoneNumber);
    }

    @Test
    @DisplayName("searchByPhoneNumber - Should throw exception when count is null")
    void searchByPhoneNumber_NullCount_ThrowsIntegrationException() {
        // Given
        String phoneNumber = "123-456-7890";
        OrderList nullCountOrderList = new OrderList();
        nullCountOrderList.setCount(null);

        doReturn(ResponseEntity.ok(nullCountOrderList)).when(orderListByPhoneClient).getOrderListByPhone(phoneNumber);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByPhoneNumber(phoneNumber);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(orderListByPhoneClient, times(1)).getOrderListByPhone(phoneNumber);
    }

    @Test
    @DisplayName("searchByPhoneNumber - Should propagate generic exception")
    void searchByPhoneNumber_GenericException_ThrowsRuntimeException() {
        // Given
        String phoneNumber = "123-456-7890";
        when(orderListByPhoneClient.getOrderListByPhone(phoneNumber))
                .thenThrow(new RuntimeException("API Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchasesService.searchByPhoneNumber(phoneNumber);
        });

        assertEquals("API Error", exception.getMessage());
        verify(orderListByPhoneClient, times(1)).getOrderListByPhone(phoneNumber);
    }

    // ========================================
    // Tests for searchByEmail() method
    // ========================================

    @Test
    @DisplayName("searchByEmail - Should successfully search orders by email")
    void searchByEmail_ValidEmail_ReturnsOrderResponse() {
        // Given
        String email = "test@example.com";

        doReturn(ResponseEntity.ok(mockOrderList)).when(orderListByEmailClient).getOrderListByEmail(email);
        when(orderTransformer.transformToOrderListResponseFromList(mockOrderList))
                .thenReturn(mockTransformedResponse);

        // When
        OrderListResponse result = purchasesService.searchByEmail(email);

        // Then
        assertNotNull(result);
        assertEquals(mockTransformedResponse, result);
        verify(orderListByEmailClient, times(1)).getOrderListByEmail(email);
        verify(orderTransformer, times(1)).transformToOrderListResponseFromList(mockOrderList);
    }

    @Test
    @DisplayName("searchByEmail - Should throw exception when response is null")
    void searchByEmail_NullResponse_ThrowsIntegrationException() {
        // Given
        String email = "test@example.com";
        doReturn(ResponseEntity.ok(null)).when(orderListByEmailClient).getOrderListByEmail(email);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByEmail(email);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(orderListByEmailClient, times(1)).getOrderListByEmail(email);
    }

    @Test
    @DisplayName("searchByEmail - Should throw exception when count is zero")
    void searchByEmail_ZeroCount_ThrowsIntegrationException() {
        // Given
        String email = "test@example.com";
        OrderList emptyOrderList = new OrderList();
        emptyOrderList.setCount(0);

        doReturn(ResponseEntity.ok(emptyOrderList)).when(orderListByEmailClient).getOrderListByEmail(email);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByEmail(email);
        });

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, exception.getCode());
        verify(orderListByEmailClient, times(1)).getOrderListByEmail(email);
    }

    @Test
    @DisplayName("searchByEmail - Should propagate generic exception")
    void searchByEmail_GenericException_ThrowsRuntimeException() {
        // Given
        String email = "test@example.com";
        when(orderListByEmailClient.getOrderListByEmail(email))
                .thenThrow(new RuntimeException("API Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            purchasesService.searchByEmail(email);
        });

        assertEquals("API Error", exception.getMessage());
        verify(orderListByEmailClient, times(1)).getOrderListByEmail(email);
    }

    @Test
    @DisplayName("searchByEmail - Should propagate IntegrationException from client")
    void searchByEmail_IntegrationExceptionFromClient_PropagatesException() {
        // Given
        String email = "test@example.com";
        IntegrationException clientException = new IntegrationException(
                ErrorCodes.ORDER_NOT_FOUND, "client", "No orders found");
        when(orderListByEmailClient.getOrderListByEmail(email)).thenThrow(clientException);

        // When & Then
        IntegrationException exception = assertThrows(IntegrationException.class, () -> {
            purchasesService.searchByEmail(email);
        });

        assertEquals(clientException, exception);
        verify(orderListByEmailClient, times(1)).getOrderListByEmail(email);
    }
}
