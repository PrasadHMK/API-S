package com.hallmark.enterprise.integration.papi.hmk_purchases.service.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.ShippingMethodLabelFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.Order;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderBillTo;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderBillToAddress;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderItemsInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderItemsInnerShipmentsInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderList;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderListDataInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.storeinfo.StoreInfoFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderListResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderSearchResponse;

/**
 * Unit tests for OrderTransformer
 *
 * Tests all public methods defined in OrderTransformer:
 * - transformToOrderListResponse(Order order)
 * - transformToOrderListResponseFromList(OrderList orderList)
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTransformer Unit Tests")
class OrderTransformerTest {

    @Mock
    private StoreInfoFeignClient storeInfoClient;

    @Mock
    private ShippingMethodLabelFeignClient shippingMethodLabelClient;

    @InjectMocks
    private OrderTransformer orderTransformer;

    private Order mockOrder;
    private OrderBillTo mockBillTo;
    private OrderBillToAddress mockAddress;
    private OrderList mockOrderList;
    private List<OrderItemsInner> mockItems;

    @BeforeEach
    void setUp() {
        // Setup mock address
        mockAddress = new OrderBillToAddress();
        mockAddress.setAttention("John Doe");
        mockAddress.setAddress("123 Main St");
        mockAddress.setCity("Kansas City");
        mockAddress.setPostalCode("64101");
        mockAddress.setCountryRegion("MO");
        mockAddress.setCountry("USA");

        // Setup mock billTo
        mockBillTo = new OrderBillTo();
        mockBillTo.setPhone("816-123-4567");
        mockBillTo.setEmail("john.doe@example.com");
        mockBillTo.setAddress(mockAddress);

        // Setup mock items list
        mockItems = new ArrayList<>();

        // Setup mock Order (for single order endpoint)
        mockOrder = new Order();
        mockOrder.setOrderNumber("ORD-2026-001");
        mockOrder.setOrderDate("2026-01-05");
        mockOrder.setFulfillmentStatus("Shipped");
        mockOrder.setBillTo(mockBillTo);
        mockOrder.setItems(mockItems);
        
        // Setup mock OrderList (for list endpoints)
        mockOrderList = new OrderList();
        mockOrderList.setCount(2);
        mockOrderList.setData(new ArrayList<>());
    }

    // ========================================
    // Tests for transformToOrderListResponse() method
    // ========================================

    @Test
    @DisplayName("transformToOrderListResponse - Should transform order with no items")
    void transformToOrderResponse_NoItems_ReturnsOrderResponse() {
        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponse(mockOrder);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrders());
        assertEquals(1, result.getOrders().size());

        OrderSearchResponse orderDetails = result.getOrders().get(0);
        assertEquals("ORD-2026-001", orderDetails.getOrderNumber());
        assertEquals("01-05-2026", orderDetails.getOrderDate());
        assertEquals("816-123-4567", orderDetails.getOrderBillToPhone());
        assertEquals("john.doe@example.com", orderDetails.getOrderBillToEmail());
        assertEquals("John Doe", orderDetails.getOrderBillToAttention());
        assertNotNull(orderDetails.getShipToAddressShipments());
        assertEquals(0, orderDetails.getShipToAddressShipments().size(), "ShipToAddressShipments should be empty");
        assertNull(orderDetails.getBoPISItems());
    }

    @Test
    @DisplayName("transformToOrderListResponse - Should transform order with ship-to-address items")
    void transformToOrderResponse_WithShipToAddressItems_ReturnsOrderResponse() {
        // Given
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("ShipToAddress");
        item.setDescription("Test Product");
        item.setQuantityOrdered(2);
        item.setShipments(new ArrayList<>());
        mockItems.add(item);
        mockOrder.setItems(mockItems);

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponse(mockOrder);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrders().size());
        OrderSearchResponse orderDetails = result.getOrders().get(0);
        assertNotNull(orderDetails.getShipToAddressShipments());
        assertNull(orderDetails.getBoPISItems());
    }

    @Test
    @DisplayName("transformToOrderListResponse - Should transform order with BOPIS items")
    void transformToOrderResponse_WithBopisItems_ReturnsOrderResponse() {
        // Given
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("PickUpAtStore");
        item.setDescription("Test Product");
        item.setQuantityOrdered(1);
        item.setStoreID("STORE001");
        item.setStoreName("KC Main Store");
        item.setStorePhone("816-555-0100");
        item.setPickupStatus("ReadyForPickup");
        mockItems.add(item);
        mockOrder.setItems(mockItems);

        // Mock store info client
        when(storeInfoClient.getStoreInfo("STORE001")).thenReturn(ResponseEntity.ok("123 Store St, KC, MO 64101"));

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponse(mockOrder);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrders().size());
        OrderSearchResponse orderDetails = result.getOrders().get(0);
        assertNotNull(orderDetails.getShipToAddressShipments());
        assertEquals(0, orderDetails.getShipToAddressShipments().size(), "ShipToAddressShipments should be empty for BOPIS items");
        assertNotNull(orderDetails.getBoPISItems());
        assertEquals("Ready For Pickup", orderDetails.getBoPISItems().getPickupStatus());
        assertEquals(1, orderDetails.getBoPISItems().getStores().size());
    }

    @Test
    @DisplayName("transformToOrderListResponse - Should handle null items")
    void transformToOrderResponse_NullItems_ReturnsOrderResponse() {
        // Given
        mockOrder.setItems(null);

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponse(mockOrder);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrders().size());
        OrderSearchResponse orderDetails = result.getOrders().get(0);
        assertNotNull(orderDetails.getShipToAddressShipments());
        assertEquals(0, orderDetails.getShipToAddressShipments().size());
        assertNull(orderDetails.getBoPISItems());
    }

    @Test
    @DisplayName("transformToOrderListResponse - Should handle null billTo")
    void transformToOrderResponse_NullBillTo_ReturnsOrderResponse() {
        // Given
        mockOrder.setBillTo(null);

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponse(mockOrder);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrders().size());
        OrderSearchResponse orderDetails = result.getOrders().get(0);
        assertEquals("Not Available", orderDetails.getOrderBillToPhone());
        assertEquals("Not Available", orderDetails.getOrderBillToEmail());
    }

    @Test
    @DisplayName("transformToOrderListResponse - Should map shipment values from order payload")
    void transformToOrderResponse_WithShipmentInfo_MapsShipmentDetails() {
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("ShipToAddress");
        item.setDescription("Test Product");
        item.setQuantityOrdered(1);
        item.setLatestDeliveryDate("2026-01-10");

        OrderItemsInnerShipmentsInner shipment = new OrderItemsInnerShipmentsInner();
        shipment.setTrackingNumber("TRACK-123");
        shipment.setShippingMethodLabel("UPS Ground");
        shipment.setShippedDate("2026-01-06");
        shipment.setTrackURL("https://ups.com/track/TRACK-123");
        shipment.setDeliveryDate("2026-01-11T09:39:18");
        shipment.setQuantity(1);

        item.setShipments(Collections.singletonList(shipment));
        mockItems.add(item);
        mockOrder.setItems(mockItems);
        mockOrder.setFulfillmentStatus("Partially Fulfilled");

        OrderListResponse result = orderTransformer.transformToOrderListResponse(mockOrder);

        OrderSearchResponse orderDetails = result.getOrders().get(0);
        assertEquals(1, orderDetails.getShipToAddressShipments().size());
        assertEquals("01-06-2026", orderDetails.getShipToAddressShipments().get(0).getShippedDate());
        assertEquals("UPS Ground", orderDetails.getShipToAddressShipments().get(0).getCarrier());
        assertEquals("TRACK-123", orderDetails.getShipToAddressShipments().get(0).getTrackingNumber());
        assertEquals("https://ups.com/track/TRACK-123", orderDetails.getShipToAddressShipments().get(0).getTrackUrl());
        assertEquals("01-10-2026", orderDetails.getShipToAddressShipments().get(0).getAnticipatedArrivalDate());
        assertEquals("01-11-2026", orderDetails.getShipToAddressShipments().get(0).getActualDeliveryDate());
        assertEquals("Shipped", orderDetails.getShipToAddressShipments().get(0).getStatus());
    }

    // ========================================
    // Tests for transformToOrderListResponseFromList() method
    // ========================================

    @Test
    @DisplayName("transformToOrderListResponseFromList - Should transform empty order list")
    void transformToOrderListResponseFromList_EmptyList_ReturnsEmptyResponse() {
        // Given
        mockOrderList.setCount(0);
        mockOrderList.setData(Collections.emptyList());

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponseFromList(mockOrderList);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrders());
        assertEquals(0, result.getOrders().size());
    }

    @Test
    @DisplayName("transformToOrderListResponseFromList - Should transform single order in list")
    void transformToOrderListResponseFromList_SingleOrder_ReturnsOrderResponse() {
        // Given
        OrderListDataInner orderData = new OrderListDataInner();
        orderData.setId("ORD-2026-001");
        orderData.setCheckoutDate("2026-01-05");
        orderData.setBillAttention("John Doe");
        orderData.setBillPhone("816-123-4567");
        orderData.setBillEmail("john.doe@example.com");
        
        List<OrderListDataInner> dataList = new ArrayList<>();
        dataList.add(orderData);
        
        mockOrderList.setCount(1);
        mockOrderList.setData(dataList);

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponseFromList(mockOrderList);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrders());
        assertEquals(1, result.getOrders().size());
        
        OrderSearchResponse order = result.getOrders().get(0);
        assertEquals("ORD-2026-001", order.getOrderNumber());
        assertEquals("816-123-4567", order.getOrderBillToPhone());
        assertEquals("john.doe@example.com", order.getOrderBillToEmail());
    }

    @Test
    @DisplayName("transformToOrderListResponseFromList - Should transform multiple orders in list")
    void transformToOrderListResponseFromList_MultipleOrders_ReturnsOrderResponse() {
        // Given
        OrderListDataInner order1 = new OrderListDataInner();
        order1.setId("ORD-001");
        order1.setBillEmail("user1@example.com");
        
        OrderListDataInner order2 = new OrderListDataInner();
        order2.setId("ORD-002");
        order2.setBillEmail("user2@example.com");
        
        List<OrderListDataInner> dataList = new ArrayList<>();
        dataList.add(order1);
        dataList.add(order2);
        
        mockOrderList.setCount(2);
        mockOrderList.setData(dataList);

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponseFromList(mockOrderList);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrders());
        assertEquals(2, result.getOrders().size());
        assertEquals("ORD-001", result.getOrders().get(0).getOrderNumber());
        assertEquals("ORD-002", result.getOrders().get(1).getOrderNumber());
    }

    @Test
    @DisplayName("transformToOrderListResponseFromList - Should handle null data list")
    void transformToOrderListResponseFromList_NullDataList_ReturnsEmptyResponse() {
        // Given
        mockOrderList.setData(null);

        // When
        OrderListResponse result = orderTransformer.transformToOrderListResponseFromList(mockOrderList);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrders());
        assertEquals(0, result.getOrders().size());
    }
}
