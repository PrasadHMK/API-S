package com.hallmark.enterprise.integration.papi.hmk_purchases.service.transformer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.ShippingMethodLabelFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.Order;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderBillTo;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderBillToAddress;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderList;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderListDataInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderItemsInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderItemsInnerShipmentsInner;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.ShippingMethod;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.storeinfo.StoreInfoFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.AllItemList;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.AllItemListCancelledItems;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.AllItemListPickedupItems;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.AllItemListProcessingItems;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.AllItemListReadyForPickupItems;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.BuyOnlinePickUpInStoreItems;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.Item;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderListResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderSearchResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.ShipToAddressShipments;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.StoreItems;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.Stores;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Transformer
 *
 * <p>
 * Transforms order data from System APIs directly into OpenAPI spec models.
 * Eliminates intermediate/internal models for cleaner architecture.
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@Component
@AllArgsConstructor
@Slf4j
public class OrderTransformer {

    private final StoreInfoFeignClient storeInfoClient;
    private final ShippingMethodLabelFeignClient shippingMethodLabelClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final DateTimeFormatter TIME_INPUT_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter TIME_OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String NOT_AVAILABLE = "Not Available";

    /**
     * Transform single order data to OrderListResponse
     *
     * @param order The Order object from the spec
     * @return OrderListResponse with single order
     */
    public OrderListResponse transformToOrderListResponse(Order order) {
        List<OrderItemsInner> items = order.getItems();
        
        if (items == null) {
            items = new ArrayList<>();
        }

        List<OrderItemsInner> bopisItems = items.stream()
                .filter(item -> "PickUpAtStore".equals(item.getDeliveryMethod()))
                .collect(Collectors.toList());
                
        List<OrderItemsInner> shipToAddressItems = items.stream()
                .filter(item -> "ShipToAddress".equals(item.getDeliveryMethod()))
                .collect(Collectors.toList());

        OrderSearchResponse orderDetails = buildOrderSearchResponse(order, shipToAddressItems, bopisItems);
        
        return new OrderListResponse().orders(Collections.singletonList(orderDetails));
    }

    /**
     * Transform order list data to OrderListResponse
     *
     * @param orderList OrderList object from the spec
     * @return OrderListResponse with multiple orders
     */
    public OrderListResponse transformToOrderListResponseFromList(OrderList orderList) {
        List<OrderListDataInner> orders = orderList.getData();
        log.debug("OrderList Data Inner: {}", orders);
        if (orders == null) {
            orders = new ArrayList<>();
        }

        List<OrderSearchResponse> orderSearchResponses = orders.stream()
                .map(this::buildOrderSearchResponseSummary)
                .collect(Collectors.toList());

        return new OrderListResponse().orders(orderSearchResponses);
    }

    /**
     * Build OrderSearchResponse with full details (for single order search)
     */
    private OrderSearchResponse buildOrderSearchResponse(Order order,
                                                          List<OrderItemsInner> shipToAddressItems,
                                                          List<OrderItemsInner> bopisItemsList) {
        OrderBillTo billTo = order.getBillTo();
        OrderBillToAddress address = billTo != null ? billTo.getAddress() : null;

        OrderSearchResponse response = new OrderSearchResponse();
        response.setOrderNumber(order.getOrderNumber() != null ? order.getOrderNumber() : NOT_AVAILABLE);
        response.setOrderDate(formatDate(order.getOrderDate()));
        response.setOrderBillToPhone(billTo != null && billTo.getPhone() != null ? billTo.getPhone() : NOT_AVAILABLE);
        response.setOrderBillToEmail(billTo != null && billTo.getEmail() != null ? billTo.getEmail() : NOT_AVAILABLE);
        response.setOrderBillToAttention(address != null && address.getAttention() != null ? address.getAttention() : NOT_AVAILABLE);
        response.setOrderBillToAddress(address != null && address.getAddress() != null ? address.getAddress() : NOT_AVAILABLE);
        response.setOrderBillToCity(address != null && address.getCity() != null ? address.getCity() : NOT_AVAILABLE);
        response.setOrderBillToPostalCode(address != null && address.getPostalCode() != null ? address.getPostalCode() : NOT_AVAILABLE);
        response.setOrderBillToRegion(address != null && address.getCountryRegion() != null ? address.getCountryRegion() : NOT_AVAILABLE);
        response.setOrderBillToCountry(address != null && address.getCountry() != null ? address.getCountry() : NOT_AVAILABLE);
        
        if (!shipToAddressItems.isEmpty()) {
            response.setShipToAddressShipments(buildShipToAddressShipmentsList(order, shipToAddressItems));
        }
        
        if (!bopisItemsList.isEmpty()) {
            response.setBoPISItems(buildBOPISItems(bopisItemsList));
        }

        return response;
    }

    /**
     * Build OrderSearchResponse summary from OrderListDataInner (for phone/email search)
     */
    private OrderSearchResponse buildOrderSearchResponseSummary(OrderListDataInner order) {
        OrderSearchResponse response = new OrderSearchResponse();
        log.debug("Order List Data Inner: {}", order);
        response.setOrderNumber(order.getId() != null ? order.getId() : NOT_AVAILABLE);
        response.setOrderDate(formatDate(order.getCheckoutDate()));
        response.setOrderStatus(order.getStatus() != null ? order.getStatus() : NOT_AVAILABLE);
        response.setOrderBillToPhone(order.getBillPhone() != null ? order.getBillPhone() : NOT_AVAILABLE);
        response.setOrderBillToEmail(order.getBillEmail() != null ? order.getBillEmail() : NOT_AVAILABLE);
        response.setOrderBillToAttention(order.getBillAttention() != null ? order.getBillAttention() : NOT_AVAILABLE);
        response.setOrderBillToAddress(order.getBillAddress() != null ? order.getBillAddress() : NOT_AVAILABLE);
        response.setOrderBillToCity(order.getBillCity() != null ? order.getBillCity() : NOT_AVAILABLE);
        response.setOrderBillToPostalCode(order.getBillPostalCode() != null ? order.getBillPostalCode() : NOT_AVAILABLE);
        response.setOrderBillToRegion(order.getBillCountryRegionCd() != null ? order.getBillCountryRegionCd() : NOT_AVAILABLE);
        response.setOrderBillToCountry(order.getBillCountryCd() != null ? order.getBillCountryCd() : NOT_AVAILABLE);
        
        return response;
    }

    /**
     * Build list of ShipToAddressShipments (array format per OpenAPI spec)
     */
    private List<ShipToAddressShipments> buildShipToAddressShipmentsList(Order order, List<OrderItemsInner> shipToAddressItems) {
        List<ShipToAddressShipments> shipments = new ArrayList<>();

        Set<String> trackingNumbers = shipToAddressItems.stream()
                .flatMap(item -> {
                    List<OrderItemsInnerShipmentsInner> itemShipments = item.getShipments();
                    return itemShipments != null ? itemShipments.stream() : java.util.stream.Stream.empty();
                })
                .map(shipment -> {
                    String trackingNumber = shipment.getTrackingNumber();
                    return (trackingNumber == null || trackingNumber.trim().isEmpty()) ? NOT_AVAILABLE : trackingNumber;
                })
                .collect(Collectors.toSet());

        for (String trackingNumber : trackingNumbers) {
            ShipToAddressShipments shipment = buildShipmentForTrackingNumber(order, trackingNumber, shipToAddressItems);
            if (shipment != null) {
                shipments.add(shipment);
            }
        }

        ShipToAddressShipments cancelledShipment = buildCancelledShipment(shipToAddressItems);
        if (cancelledShipment != null) {
            shipments.add(cancelledShipment);
        }

        ShipToAddressShipments processingShipment = buildProcessingShipment(shipToAddressItems);
        if (processingShipment != null) {
            shipments.add(processingShipment);
        }

        return shipments;
    }

    /**
     * Build shipment information for a specific tracking number
     */
    private ShipToAddressShipments buildShipmentForTrackingNumber(Order order, String trackingNumber,
                                                                  List<OrderItemsInner> shipToAddressItems) {
        List<Item> items = new ArrayList<>();
        OrderItemsInnerShipmentsInner shipmentInfo = null;
        List<OrderItemsInner> matchedItems = new ArrayList<>();

        for (OrderItemsInner item : shipToAddressItems) {
            List<OrderItemsInnerShipmentsInner> itemShipments = item.getShipments();
            if (itemShipments != null) {
                for (OrderItemsInnerShipmentsInner itemShipment : itemShipments) {
                    String shipmentTrackingNumber = itemShipment.getTrackingNumber();
                    String normalizedTrackingNumber = (shipmentTrackingNumber == null || shipmentTrackingNumber.trim().isEmpty()) 
                        ? NOT_AVAILABLE 
                        : shipmentTrackingNumber;
                    
                    if (trackingNumber.equals(normalizedTrackingNumber)) {
                        items.add(buildItem(item, itemShipment.getQuantity()));
                        matchedItems.add(item);
                        
                        if (shipmentInfo == null) {
                            shipmentInfo = itemShipment;
                        }
                    }
                }
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        ShipToAddressShipments shipment = new ShipToAddressShipments();
        shipment.setItems(items);
        
        if (shipmentInfo != null) {
            String shippingMethodLabel = getShippingMethodLabel(null, 
                shipmentInfo.getShippingMethodLabel());
            
            shipment.setShippedDate(formatDate(shipmentInfo.getShippedDate()));
            shipment.setCarrier(shippingMethodLabel);
            shipment.setTrackingNumber(shipmentInfo.getTrackingNumber() != null ? shipmentInfo.getTrackingNumber() : NOT_AVAILABLE);
            
            String trackUrl = shipmentInfo.getTrackURL();
            if (trackUrl == null || trackUrl.contains("null")) {
                shipment.setTrackUrl(NOT_AVAILABLE);
            } else {
                shipment.setTrackUrl(trackUrl);
            }

            shipment.setAnticipatedArrivalDate(getAnticipatedArrivalDate(matchedItems));
            shipment.setActualDeliveryDate(formatDate(shipmentInfo.getDeliveryDate()));
            shipment.setStatus(getShipmentStatus(order, shipmentInfo));
        }

        return shipment;
    }

    /**
     * Build cancelled shipment information
     */
    private ShipToAddressShipments buildCancelledShipment(List<OrderItemsInner> shipToAddressItems) {
        List<Item> cancelledItems = new ArrayList<>();

        for (OrderItemsInner item : shipToAddressItems) {
            Integer quantityCancelled = item.getQuantityCancelled();
            if (quantityCancelled != null && quantityCancelled > 0) {
                cancelledItems.add(buildItem(item, quantityCancelled));
            }
        }

        if (cancelledItems.isEmpty()) {
            return null;
        }

        ShipToAddressShipments shipment = new ShipToAddressShipments();
        shipment.setItems(cancelledItems);
        shipment.setStatus("Cancelled");
        
        return shipment;
    }

    /**
     * Build processing (yet to ship) shipment information
     */
    private ShipToAddressShipments buildProcessingShipment(List<OrderItemsInner> shipToAddressItems) {
        List<Item> processingItems = new ArrayList<>();

        for (OrderItemsInner item : shipToAddressItems) {
            Integer quantityOrdered = item.getQuantityOrdered();
            Integer quantityCancelled = item.getQuantityCancelled();
            
            List<OrderItemsInnerShipmentsInner> shipments = item.getShipments();
            int quantityShipped = 0;
            if (shipments != null) {
                quantityShipped = shipments.stream()
                        .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                        .sum();
            }

            int yetToShip = (quantityOrdered != null ? quantityOrdered : 0) 
                - (quantityCancelled != null ? quantityCancelled : 0) 
                - quantityShipped;

            if (yetToShip > 0) {
                Item processingItem = buildItem(item, yetToShip);
                processingItem.setEarliestDeliveryDate(formatDate(item.getEarliestDeliveryDate()));
                processingItem.setLatestDeliveryDate(formatDate(item.getLatestDeliveryDate()));
                processingItems.add(processingItem);
            }
        }

        if (processingItems.isEmpty()) {
            return null;
        }

        ShipToAddressShipments shipment = new ShipToAddressShipments();
        shipment.setItems(processingItems);
        shipment.setStatus("Processing");
        shipment.setShippedDate("Pending");
        shipment.setCarrier(NOT_AVAILABLE);
        shipment.setTrackingNumber(NOT_AVAILABLE);
        
        return shipment;
    }

    /**
     * Build BOPIS (Buy Online Pick Up In Store) items information
     */
    private BuyOnlinePickUpInStoreItems buildBOPISItems(List<OrderItemsInner> bopisItemsList) {
        String pickupStatus = calculatePickupStatus(bopisItemsList);
        
        Map<String, List<OrderItemsInner>> itemsByStore = bopisItemsList.stream()
                .collect(Collectors.groupingBy(item -> item.getStoreID() != null ? item.getStoreID() : "UNKNOWN"));
        
        List<Stores> stores = new ArrayList<>();
        for (Map.Entry<String, List<OrderItemsInner>> entry : itemsByStore.entrySet()) {
            Stores store = buildStore(entry.getKey(), entry.getValue());
            if (store != null) {
                stores.add(store);
            }
        }
        
        BuyOnlinePickUpInStoreItems bopisItems = new BuyOnlinePickUpInStoreItems();
        bopisItems.setPickupStatus(pickupStatus);
        bopisItems.setStores(stores);
        
        return bopisItems;
    }

    /**
     * Build Store information for BOPIS items
     */
    private Stores buildStore(String storeId, List<OrderItemsInner> items) {
        Stores store = new Stores();
        
        AllItemList itemList = new AllItemList();
        
        List<StoreItems> pickedUpItems = buildStoreItemsByStatus(items, "PickedUp");
        if (!pickedUpItems.isEmpty()) {
            AllItemListPickedupItems pickedUp = new AllItemListPickedupItems();
            pickedUp.setItems(pickedUpItems);
            itemList.setPickedupItems(pickedUp);
        }
        
        List<StoreItems> processingItems = buildStoreItemsByStatus(items, "Processing");
        if (!processingItems.isEmpty()) {
            AllItemListProcessingItems processing = new AllItemListProcessingItems();
            processing.setItems(processingItems);
            itemList.setProcessingItems(processing);
        }
        
        List<StoreItems> cancelledItems = buildStoreItemsByStatus(items, "Cancelled");
        if (!cancelledItems.isEmpty()) {
            AllItemListCancelledItems cancelled = new AllItemListCancelledItems();
            cancelled.setItems(cancelledItems);
            itemList.setCancelledItems(cancelled);
        }
        
        List<StoreItems> readyForPickupItems = buildStoreItemsByStatus(items, "ReadyForPickup");
        if (!readyForPickupItems.isEmpty()) {
            AllItemListReadyForPickupItems readyForPickup = new AllItemListReadyForPickupItems();
            readyForPickup.setItems(readyForPickupItems);
            itemList.setReadyForPickupItems(readyForPickup);
        }
        
        store.setItemList(itemList);
        
        if (!processingItems.isEmpty()) {
            OrderItemsInner firstItem = items.stream()
                    .filter(item -> "Processing".equals(item.getPickupStatus()))
                    .findFirst()
                    .orElse(null);
            
            if (firstItem != null) {
                store.setPromisedDate(formatDate(firstItem.getPickConfirmDate()));
                store.setPromisedTime(formatTime(firstItem.getPickupTime()));
            }
        }
        
        if (!items.isEmpty()) {
            OrderItemsInner firstItem = items.get(0);
            store.setStoreInfo(firstItem.getStoreName() != null ? firstItem.getStoreName() : NOT_AVAILABLE);
            store.setStoreAddress(getStoreAddress(storeId));
            store.setStorePhone(firstItem.getStorePhone() != null ? firstItem.getStorePhone() : NOT_AVAILABLE);
        }
        
        return store;
    }

    /**
     * Build store items list for a specific pickup status
     */
    private List<StoreItems> buildStoreItemsByStatus(List<OrderItemsInner> items, String status) {
        return items.stream()
                .filter(item -> status.equals(item.getPickupStatus()))
                .map(item -> {
                    StoreItems storeItem = new StoreItems();
                    storeItem.setDescription(item.getDescription() != null ? item.getDescription() : NOT_AVAILABLE);
                    Integer quantity = item.getQuantityOrdered();
                    if (quantity != null) {
                        storeItem.setQuantity(new BigDecimal(quantity.toString()));
                    }
                    return storeItem;
                })
                .collect(Collectors.toList());
    }

    /**
     * Build Item from order data
     */
    private Item buildItem(OrderItemsInner itemData, Object quantity) {
        Item item = new Item();
        item.setDescription(itemData.getDescription() != null ? itemData.getDescription() : NOT_AVAILABLE);
        
        if (quantity instanceof Number) {
            item.setQuantity(new BigDecimal(quantity.toString()));
        } else if (quantity != null) {
            try {
                item.setQuantity(new BigDecimal(quantity.toString()));
            } catch (NumberFormatException e) {
                log.warn("Could not convert quantity to BigDecimal: {}", quantity);
            }
        }
        
        return item;
    }

    /**
     * Calculate overall pickup status for BOPIS items
     */
    private String calculatePickupStatus(List<OrderItemsInner> bopisItemsList) {
        Set<String> statuses = bopisItemsList.stream()
                .map(item -> item.getPickupStatus())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (statuses.isEmpty()) {
            return NOT_AVAILABLE;
        }

        if (statuses.size() == 1) {
            String status = statuses.iterator().next();
            return formatPickupStatus(status);
        }

        List<String> statusParts = new ArrayList<>();

        for (String status : Arrays.asList("PickedUp", "ReadyForPickup", "Processing", "Cancelled")) {
            if (statuses.contains(status)) {
                statusParts.add("Partially " + formatPickupStatus(status));
            }
        }

        if (statusParts.size() > 1) {
            String lastPart = statusParts.remove(statusParts.size() - 1);
            return String.join(", ", statusParts) + " and " + lastPart;
        }

        return statusParts.isEmpty() ? NOT_AVAILABLE : statusParts.get(0);
    }

    /**
     * Format pickup status to match Mule format (add spaces)
     */
    private String formatPickupStatus(String status) {
        if (status == null) {
            return NOT_AVAILABLE;
        }
        switch (status) {
            case "PickedUp":
                return "Picked Up";
            case "ReadyForPickup":
                return "Ready For Pickup";
            default:
                return status;
        }
    }

    /**
     * Get shipping method label from API.
     * 
     * Gracefully handles errors (404, 500, 503, network issues, etc.) by returning
     * the default label. This ensures order processing continues even when the
     * shipping method label service is unavailable.
     * 
     * @param shippingMethodId The shipping method ID to look up (may be null)
     * @param defaultLabel The default label to use if service fails or returns null
     * @return The shipping method label or default label/"Not Available"
     */
    private String getShippingMethodLabel(String shippingMethodId, String defaultLabel) {
        if (shippingMethodId == null) {
            return defaultLabel != null ? defaultLabel : NOT_AVAILABLE;
        }
        
        try {
            ResponseEntity<ShippingMethod> response = shippingMethodLabelClient.getShippingMethodLabel(shippingMethodId, null);
            if (response != null && response.getBody() != null) {
                ShippingMethod shippingMethod = response.getBody();
                String label = shippingMethod.getShippingMethodLabel();
                return label != null ? label : (defaultLabel != null ? defaultLabel : NOT_AVAILABLE);
            }
        } catch (Exception e) {
            // Gracefully handle all errors (404, 500, 503, network issues, etc.)
            // Log the error but continue processing - matches Mule on-error-continue behavior
            log.info("Error occurred in fetching shipment method label for ID {}: {}. Using default label.", 
                shippingMethodId, e.getMessage());
        }
        
        return defaultLabel != null ? defaultLabel : NOT_AVAILABLE;
    }

    private String getAnticipatedArrivalDate(List<OrderItemsInner> matchedItems) {
        return matchedItems.stream()
                .map(OrderItemsInner::getLatestDeliveryDate)
                .filter(Objects::nonNull)
                .map(this::formatDate)
                .filter(date -> !NOT_AVAILABLE.equals(date))
                .findFirst()
                .orElse(NOT_AVAILABLE);
    }

    private String getShipmentStatus(Order order, OrderItemsInnerShipmentsInner shipmentInfo) {
        if (isDelivered(order)) {
            return "Delivered";
        }

        // If the order payload already contains a concrete shipment segment, treat it as shipped.
        // Cancelled and processing are handled in their own synthetic shipment builders.
        if (shipmentInfo != null) {
            return "Shipped";
        }

        String fulfillmentStatus = order != null ? order.getFulfillmentStatus() : null;
        if (fulfillmentStatus == null || fulfillmentStatus.trim().isEmpty()) {
            return NOT_AVAILABLE;
        }

        if ("Partially Fulfilled".equalsIgnoreCase(fulfillmentStatus)
                || "Fulfilled".equalsIgnoreCase(fulfillmentStatus)) {
            return "Shipped";
        }

        return fulfillmentStatus;
    }

    private boolean isDelivered(Order order) {
        String fulfillmentStatus = order != null ? order.getFulfillmentStatus() : null;
        return fulfillmentStatus != null && fulfillmentStatus.toLowerCase().contains("deliver");
    }

    /**
     * Get formatted store address from Store Info API.
     * 
     * Gracefully handles errors (404 Not Found, 500, 503, network issues, etc.) by 
     * returning "Not Available". This ensures order processing continues even when 
     * the store info service is unavailable.
     * 
     * Constructs address in format: "address1, address2, city, state_code postal_code"
     * 
     * @param storeId The store ID to look up
     * @return Formatted address string, or "Not Available" if service fails or all 
     *         address fields are blank/null
     */
    @SuppressWarnings("unchecked")
    private String getStoreAddress(String storeId) {
        try {
            ResponseEntity<Object> response = storeInfoClient.getStoreInfo(storeId);
            if (response != null && response.getBody() != null) {
                Map<String, Object> storeInfo = (Map<String, Object>) response.getBody();
                
                // Get all address fields
                String address1 = getStringOrDefault(storeInfo, "address1");
                String address2 = getStringOrDefault(storeInfo, "address2");
                String city = getStringOrDefault(storeInfo, "city");
                String stateCode = getStringOrDefault(storeInfo, "state_code");
                String postalCode = getStringOrDefault(storeInfo, "postal_code");
                
                // Check if all fields are blank - return "Not Available" only if ALL are blank
                boolean allBlank = NOT_AVAILABLE.equals(address1) && 
                                  NOT_AVAILABLE.equals(address2) && 
                                  NOT_AVAILABLE.equals(city) && 
                                  NOT_AVAILABLE.equals(stateCode) && 
                                  NOT_AVAILABLE.equals(postalCode);
                
                if (allBlank) {
                    return NOT_AVAILABLE;
                }
                
                // Build address from available fields
                StringBuilder address = new StringBuilder();
                
                if (!NOT_AVAILABLE.equals(address1)) {
                    address.append(address1);
                }
                
                if (!NOT_AVAILABLE.equals(address2)) {
                    if (address.length() > 0) {
                        address.append(", ");
                    }
                    address.append(address2);
                }
                
                if (!NOT_AVAILABLE.equals(city)) {
                    if (address.length() > 0) {
                        address.append(", ");
                    }
                    address.append(city);
                }
                
                if (!NOT_AVAILABLE.equals(stateCode)) {
                    if (address.length() > 0) {
                        address.append(", ");
                    }
                    address.append(stateCode);
                }
                
                if (!NOT_AVAILABLE.equals(postalCode)) {
                    if (address.length() > 0) {
                        address.append(" ");
                    }
                    address.append(postalCode);
                }
                
                return address.toString();
            }
        } catch (Exception e) {
            // Gracefully handle all errors (404 Not Found, 500, 503, network issues, etc.)
            // Log the error but continue processing - matches Mule on-error-continue behavior
            log.info("Store info not found for store ID {}: {}. Using default value.", 
                storeId, e.getMessage());
        }
        
        return NOT_AVAILABLE;
    }

    /**
     * Format date to MM-dd-yyyy format
     */
    private String formatDate(Object date) {
        if (date == null) {
            return NOT_AVAILABLE;
        }
        
        if (date instanceof LocalDate) {
            return ((LocalDate) date).format(DATE_FORMATTER);
        } else if (date instanceof String) {
            String dateStr = (String) date;
            
            // Check if already a "Not Available" text value (handles typos from external systems)
            if (dateStr.equalsIgnoreCase("Not Available") || 
                dateStr.equalsIgnoreCase("Not Avalaible") ||
                dateStr.trim().isEmpty()) {
                return NOT_AVAILABLE;
            }
            
            if (dateStr.contains("T")) {
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(dateStr);
                return dateTime.toLocalDate().format(DATE_FORMATTER);
            } else {
                LocalDate localDate = LocalDate.parse(dateStr);
                return localDate.format(DATE_FORMATTER);
            }
        }
        
        return NOT_AVAILABLE;
    }

    /**
     * Format time to hh:mm a format
     */
    private String formatTime(Object time) {
        if (time == null) {
            return NOT_AVAILABLE;
        }
        
        if (time instanceof LocalTime) {
            return ((LocalTime) time).format(TIME_OUTPUT_FORMATTER);
        } else if (time instanceof String) {
            String timeStr = (String) time;
            LocalTime localTime = LocalTime.parse(timeStr, TIME_INPUT_FORMATTER);
            return localTime.format(TIME_OUTPUT_FORMATTER);
        }
        
        return NOT_AVAILABLE;
    }

    /**
     * Get value from map with default
     */
    private String getStringOrDefault(Map<String, Object> map, String key) {
        if (map == null) {
            return NOT_AVAILABLE;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : NOT_AVAILABLE;
    }
}
