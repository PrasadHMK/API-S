package com.hallmark.enterprise.integration.papi.hmk_purchases.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrderListByEmailFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.config.TestFeignConfig;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrderListByPhoneFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrdersOmniManhFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.ShippingMethodLabelFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.Order;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderBillTo;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderBillToAddress;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderItemsInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderItemsInnerShipmentsInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderList;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderListDataInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.ShippingMethod;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.storeinfo.StoreInfoFeignClient;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Purchases PAPI
 *
 * Tests end-to-end flows covering:
 * - Success scenarios for all search types (order number, phone, email)
 * - Failure scenarios (not found, validation errors, system errors)
 * - Request validation (missing parameters, invalid formats)
 * - Data transformation and external API integration
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestFeignConfig.class)
@DisplayName("Purchases PAPI Integration Tests")
class PurchasesPapiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdersOmniManhFeignClient ordersClient;

    @MockitoBean
    private OrderListByEmailFeignClient orderListByEmailClient;

    @MockitoBean
    private OrderListByPhoneFeignClient orderListByPhoneClient;

    @MockitoBean
    private StoreInfoFeignClient storeInfoClient;

    @MockitoBean
    private ShippingMethodLabelFeignClient shippingMethodLabelClient;

    private Order mockOrder;
    private OrderList mockOrderList;
    private OrderBillTo mockBillTo;
    private OrderBillToAddress mockAddress;

    @BeforeEach
    void setUp() {
        setupMockOrderData();
        setupMockOrderListData();
    }

    private void setupMockOrderData() {
        // Setup address
        mockAddress = new OrderBillToAddress();
        mockAddress.setAttention("John Doe");
        mockAddress.setAddress("123 Main Street");
        mockAddress.setCity("Kansas City");
        mockAddress.setPostalCode("64101");
        mockAddress.setCountryRegion("MO");
        mockAddress.setCountry("USA");

        // Setup billTo
        mockBillTo = new OrderBillTo();
        mockBillTo.setPhone("816-123-4567");
        mockBillTo.setEmail("john.doe@hallmark.com");
        mockBillTo.setAddress(mockAddress);

        // Setup order
        mockOrder = new Order();
        mockOrder.setOrderNumber("ORD-2026-12345");
        mockOrder.setOrderDate("2026-01-05");
        mockOrder.setFulfillmentStatus("Shipped");
        mockOrder.setBillTo(mockBillTo);
        mockOrder.setItems(new ArrayList<>());
    }

    private void setupMockOrderListData() {
        // Setup order list data
        OrderListDataInner orderData = new OrderListDataInner();
        orderData.setId("ORD-2026-12345");
        orderData.setCheckoutDate("2026-01-05");
        orderData.setBillAttention("John Doe");
        orderData.setBillPhone("816-123-4567");
        orderData.setBillEmail("john.doe@hallmark.com");
        
        List<OrderListDataInner> dataList = new ArrayList<>();
        dataList.add(orderData);
        
        mockOrderList = new OrderList();
        mockOrderList.setCount(1);
        mockOrderList.setData(dataList);
    }

    // ========================================
    // SUCCESS SCENARIOS
    // ========================================

    @Test
    @DisplayName("E2E: Search by order number - Success")
    void searchByOrderNumber_ValidOrder_ReturnsSuccess() throws Exception {
        // Given
        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORD-2026-12345"))
                .andExpect(jsonPath("$.orders[0].orderDate").value("01-05-2026"))
                .andExpect(jsonPath("$.orders[0].orderBillToEmail").value("john.doe@hallmark.com"))
                .andExpect(jsonPath("$.orders[0].orderBillToPhone").value("816-123-4567"));

        verify(ordersClient, times(1)).getOrderById("ORD-2026-12345");
        verify(orderListByPhoneClient, never()).getOrderListByPhone(anyString());
        verify(orderListByEmailClient, never()).getOrderListByEmail(anyString());
    }

    @Test
    @DisplayName("E2E: Search by phone number - Success")
    void searchByPhoneNumber_ValidPhone_ReturnsSuccess() throws Exception {
        // Given
        doReturn(ResponseEntity.ok(mockOrderList)).when(orderListByPhoneClient).getOrderListByPhone("816-123-4567");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("phoneNumber", "816-123-4567")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORD-2026-12345"));

        verify(orderListByPhoneClient, times(1)).getOrderListByPhone("816-123-4567");
        verify(ordersClient, never()).getOrderById(anyString());
        verify(orderListByEmailClient, never()).getOrderListByEmail(anyString());
    }

    @Test
    @DisplayName("E2E: Search by email - Success")
    void searchByEmail_ValidEmail_ReturnsSuccess() throws Exception {
        // Given
        doReturn(ResponseEntity.ok(mockOrderList)).when(orderListByEmailClient).getOrderListByEmail("john.doe@hallmark.com");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("emailAddress", "john.doe@hallmark.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORD-2026-12345"));

        verify(orderListByEmailClient, times(1)).getOrderListByEmail("john.doe@hallmark.com");
        verify(ordersClient, never()).getOrderById(anyString());
        verify(orderListByPhoneClient, never()).getOrderListByPhone(anyString());
    }

    @Test
    @DisplayName("E2E: Search with ship-to-address items maps shipment from order payload")
    void searchByOrderNumber_WithShipToAddressItems_MapsShipmentDetails() throws Exception {
        // Given - Add ship-to-address item
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("ShipToAddress");
        item.setDescription("Greeting Cards");
        item.setQuantityOrdered(2);
        item.setLatestDeliveryDate("2026-01-10");

        OrderItemsInnerShipmentsInner shipment = new OrderItemsInnerShipmentsInner();
        shipment.setTrackingNumber("1Z999AA10123456784");
        shipment.setShippingMethodLabel("UPS Ground");
        shipment.setQuantity(2);
        shipment.setShippedDate("2026-01-06");
        shipment.setTrackURL("https://ups.com/track/1Z999AA10123456784");
        
        List<OrderItemsInnerShipmentsInner> shipments = new ArrayList<>();
        shipments.add(shipment);
        item.setShipments(shipments);
        
        List<OrderItemsInner> items = new ArrayList<>();
        items.add(item);
        mockOrder.setItems(items);

        // Mock shipping method label
        ShippingMethod shippingMethod = new ShippingMethod();
        shippingMethod.setShippingMethodLabel("UPS Ground");
        when(shippingMethodLabelClient.getShippingMethodLabel(anyString(), any()))
                .thenReturn(ResponseEntity.ok(shippingMethod));

        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments").isArray())
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].tracking_number")
                        .value("1Z999AA10123456784"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].shipped_date")
                        .value("01-06-2026"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].carrier")
                        .value("UPS Ground"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].track_url")
                        .value("https://ups.com/track/1Z999AA10123456784"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].anticipated_arrival_date")
                        .value("01-10-2026"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].actual_delivery_date")
                        .value("Not Available"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].status")
                        .value("Shipped"));
    }

    @Test
    @DisplayName("E2E: Search with BOPIS items and store info API call")
    void searchByOrderNumber_WithBOPISItems_CallsStoreInfoAPI() throws Exception {
        // Given - Add BOPIS item
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("PickUpAtStore");
        item.setDescription("Gift Items");
        item.setQuantityOrdered(1);
        item.setStoreID("STORE-KC-001");
        item.setStoreName("Kansas City Plaza");
        item.setStorePhone("816-555-0100");
        item.setPickupStatus("ReadyForPickup");
        
        List<OrderItemsInner> items = new ArrayList<>();
        items.add(item);
        mockOrder.setItems(items);

        // Mock store info - Return Map as expected by the code
        Map<String, Object> storeInfo = new HashMap<>();
        storeInfo.put("address1", "4750 Country Club Plaza");
        storeInfo.put("address2", null);  // null instead of empty string to avoid extra comma
        storeInfo.put("city", "Kansas City");
        storeInfo.put("state_code", "MO");
        storeInfo.put("postal_code", "64112");
        doReturn(ResponseEntity.ok(storeInfo))
                .when(storeInfoClient).getStoreInfo("STORE-KC-001");

        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].BOPISItems").exists())
                .andExpect(jsonPath("$.orders[0].BOPISItems.pickupStatus").value("Ready For Pickup"))
                .andExpect(jsonPath("$.orders[0].BOPISItems.stores").isArray())
                .andExpect(jsonPath("$.orders[0].BOPISItems.stores[0].store_address")
                        .value("4750 Country Club Plaza, Kansas City, MO 64112"));

        verify(storeInfoClient, times(1)).getStoreInfo("STORE-KC-001");
    }

    // ========================================
    // FAILURE SCENARIOS
    // ========================================

    @Test
    @DisplayName("E2E: Search by order number - Order not found")
    void searchByOrderNumber_OrderNotFound_Returns404() throws Exception {
        // Given
        doReturn(ResponseEntity.ok(null)).when(ordersClient).getOrderById("ORD-INVALID");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(ordersClient, times(1)).getOrderById("ORD-INVALID");
    }

    @Test
    @DisplayName("E2E: Search by phone - No orders found")
    void searchByPhoneNumber_NoOrdersFound_Returns404() throws Exception {
        // Given
        OrderList emptyResponse = new OrderList();
        emptyResponse.setCount(0);
        emptyResponse.setData(new ArrayList<>());

        doReturn(ResponseEntity.ok(emptyResponse)).when(orderListByPhoneClient).getOrderListByPhone("555-0000");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("phoneNumber", "555-0000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: Search by email - No orders found")
    void searchByEmail_NoOrdersFound_Returns404() throws Exception {
        // Given
        OrderList emptyResponse = new OrderList();
        emptyResponse.setCount(0);

        doReturn(ResponseEntity.ok(emptyResponse)).when(orderListByEmailClient).getOrderListByEmail("nobody@example.com");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("emailAddress", "nobody@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: System error from orders API")
    void searchByOrderNumber_SystemError_Returns500() throws Exception {
        // Given
        when(ordersClient.getOrderById(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("E2E: Delivered shipment maps actual delivery date from order payload")
    void searchByOrderNumber_DeliveredShipment_ReturnsMappedDeliveryFields() throws Exception {
        // Given - Add ship-to-address item
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("ShipToAddress");
        item.setDescription("Product");
        item.setLatestDeliveryDate("2026-01-12");

        OrderItemsInnerShipmentsInner shipment = new OrderItemsInnerShipmentsInner();
        shipment.setTrackingNumber("TRACK123");
        shipment.setShippingMethodLabel("FEDEX Ground");
        shipment.setQuantity(1);
        
        List<OrderItemsInnerShipmentsInner> shipments = new ArrayList<>();
        shipments.add(shipment);
        item.setShipments(shipments);
        
        List<OrderItemsInner> items = new ArrayList<>();
        items.add(item);
        mockOrder.setItems(items);
        mockOrder.setFulfillmentStatus("Delivered");

        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].anticipated_arrival_date")
                        .value("01-12-2026"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].actual_delivery_date")
                        .value("01-12-2026"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].status")
                        .value("Delivered"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments[0].tracking_number")
                        .value("TRACK123"));
    }

    @Test
    @DisplayName("E2E: Store info API failure - Graceful degradation")
    void searchByOrderNumber_StoreInfoAPIFails_ReturnsPartialData() throws Exception {
        // Given - Add BOPIS item
        OrderItemsInner item = new OrderItemsInner();
        item.setDeliveryMethod("PickUpAtStore");
        item.setDescription("Product");
        item.setStoreID("STORE001");
        item.setStoreName("Test Store");
        item.setStorePhone("555-0100");
        item.setPickupStatus("Processing");
        item.setQuantityOrdered(1);
        
        List<OrderItemsInner> items = new ArrayList<>();
        items.add(item);
        mockOrder.setItems(items);

        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");
        doThrow(new RuntimeException("Store service unavailable")).when(storeInfoClient).getStoreInfo(anyString());

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].BOPISItems.stores[0].store_address")
                        .value("Not Available"))
                .andExpect(jsonPath("$.orders[0].BOPISItems.stores[0].store_info")
                        .value("Test Store"));
    }

    // ========================================
    // REQUEST VALIDATION SCENARIOS
    // ========================================

    @Test
    @DisplayName("E2E: No search parameters provided")
    void searchOrders_NoParameters_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ordersClient, orderListByPhoneClient, orderListByEmailClient);
    }

    @Test
    @DisplayName("E2E: Multiple parameters - Phone number takes priority")
    void searchOrders_MultipleParameters_PrioritizesPhoneNumber() throws Exception {
        // Given
        doReturn(ResponseEntity.ok(mockOrderList)).when(orderListByPhoneClient).getOrderListByPhone("816-123-4567");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .param("phoneNumber", "816-123-4567")
                        .param("email", "john.doe@hallmark.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderListByPhoneClient, times(1)).getOrderListByPhone("816-123-4567");
        verifyNoInteractions(ordersClient, orderListByEmailClient);
    }

    @Test
    @DisplayName("E2E: Order number and email - Order number takes priority")
    void searchOrders_OrderNumberAndEmail_PrioritizesOrderNumber() throws Exception {
        // Given
        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .param("email", "john.doe@hallmark.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ordersClient, times(1)).getOrderById("ORD-2026-12345");
        verifyNoInteractions(orderListByPhoneClient, orderListByEmailClient);
    }

    // ========================================
    // DATA TRANSFORMATION SCENARIOS
    // ========================================

    @Test
    @DisplayName("E2E: Date formatting transformation")
    void searchByOrderNumber_DateFormatting_TransformsCorrectly() throws Exception {
        // Given
        mockOrder.setOrderDate("2026-12-25");
        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderDate").value("12-25-2026"))
                .andReturn();
    }

    @Test
    @DisplayName("E2E: Missing billTo data - Returns defaults")
    void searchByOrderNumber_MissingBillTo_ReturnsDefaults() throws Exception {
        // Given
        mockOrder.setBillTo(null);
        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderBillToPhone").value("Not Available"))
                .andExpect(jsonPath("$.orders[0].orderBillToEmail").value("Not Available"));
    }

    @Test
    @DisplayName("E2E: Empty items array - Returns order without shipments")
    void searchByOrderNumber_EmptyItems_ReturnsOrderWithoutShipments() throws Exception {
        // Given
        mockOrder.setItems(new ArrayList<>());
        doReturn(ResponseEntity.ok(mockOrder)).when(ordersClient).getOrderById("ORD-2026-12345");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("orderNumber", "ORD-2026-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORD-2026-12345"))
                .andExpect(jsonPath("$.orders[0].shipToAddressShipments").doesNotExist())
                .andExpect(jsonPath("$.orders[0].BOPISItems").doesNotExist());
    }

    @Test
    @DisplayName("E2E: Multiple orders in list response")
    void searchByPhoneNumber_MultipleOrders_ReturnsAllOrders() throws Exception {
        // Given
        OrderListDataInner order2 = new OrderListDataInner();
        order2.setId("ORD-2026-67890");
        order2.setCheckoutDate("2026-01-10");
        order2.setBillPhone("816-123-4567");
        
        List<OrderListDataInner> dataList = new ArrayList<>();
        dataList.add(mockOrderList.getData().get(0));
        dataList.add(order2);
        
        OrderList multipleOrdersList = new OrderList();
        multipleOrdersList.setCount(2);
        multipleOrdersList.setData(dataList);

        doReturn(ResponseEntity.ok(multipleOrdersList)).when(orderListByPhoneClient).getOrderListByPhone("816-123-4567");

        // When & Then
        mockMvc.perform(get("/purchases-papi/v1/orders/search")
                        .param("phoneNumber", "816-123-4567")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORD-2026-12345"))
                .andExpect(jsonPath("$.orders[1].orderNumber").value("ORD-2026-67890"));
    }
}
