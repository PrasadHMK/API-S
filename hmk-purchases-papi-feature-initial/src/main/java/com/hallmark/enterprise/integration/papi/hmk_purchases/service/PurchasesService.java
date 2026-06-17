package com.hallmark.enterprise.integration.papi.hmk_purchases.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrderListByEmailFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrderListByPhoneFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrdersOmniManhFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.Order;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders_omni_manh_sapi.spec.model.OrderList;
import com.hallmark.enterprise.integration.papi.hmk_purchases.exception.ErrorCodes;
import com.hallmark.enterprise.integration.papi.hmk_purchases.server.spec.model.OrderListResponse;
import com.hallmark.enterprise.integration.papi.hmk_purchases.service.transformer.OrderTransformer;
import com.hallmark.integration.common.exception.IntegrationException;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PurchasesService – Service class for managing purchase order operations.
 *
 * <p>
 * This service provides methods to retrieve orders by order number, phone number, or email.
 * It delegates to Feign clients for data retrieval and OrderTransformer for response transformation.
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@Service
@AllArgsConstructor
@Slf4j
@Validated
public class PurchasesService {

    private final OrdersOmniManhFeignClient ordersClient;
    private final OrderListByEmailFeignClient orderListByEmailClient;
    private final OrderListByPhoneFeignClient orderListByPhoneClient;
    private final OrderTransformer orderTransformer;

    /**
     * Search for order by order number
     *
     * @param orderNumber The order number to search for
     * @return OrderListResponse object with OpenAPI spec models
     */
    public OrderListResponse searchByOrderNumber(@NotBlank String orderNumber) {
        log.info("Searching for order by order number: {}", orderNumber);
        
        Order response = ordersClient.getOrderById(orderNumber).getBody();
        
        if (response == null || response.getOrderNumber() == null) {
            log.warn("No order found for order number: {}", orderNumber);
            throw new IntegrationException(ErrorCodes.ORDER_NOT_FOUND, "service", "No order is found");
        }

        log.info("Successfully retrieved order for order number: {}", orderNumber);
        return orderTransformer.transformToOrderListResponse(response);
    }

    /**
     * Search for orders by phone number
     *
     * @param phoneNumber The phone number to search for
     * @return OrderListResponse object with OpenAPI spec models
     */
    public OrderListResponse searchByPhoneNumber(@NotBlank String phoneNumber) {
        log.info("Searching for orders by phone number");
        
        OrderList response = orderListByPhoneClient.getOrderListByPhone(phoneNumber).getBody();
       
        if (response == null || response.getCount() == null || response.getCount() == 0) {
            log.warn("No orders found for phone number");
            throw new IntegrationException(ErrorCodes.ORDER_NOT_FOUND, "service", "No order is found");
        }

        log.info("Successfully retrieved orders for phone number");
        return orderTransformer.transformToOrderListResponseFromList(response);
    }

    /**
     * Search for orders by email address
     *
     * @param email The email address to search for
     * @return OrderListResponse object with OpenAPI spec models
     */
    public OrderListResponse searchByEmail(@NotBlank String email) {
        log.info("Searching for orders by email");
        
        OrderList response = orderListByEmailClient.getOrderListByEmail(email).getBody();
        
        if (response == null || response.getCount() == null || response.getCount() == 0) {
            log.warn("No orders found for email");
            throw new IntegrationException(ErrorCodes.ORDER_NOT_FOUND, "service", "No order is found");
        }

        log.info("Successfully retrieved orders for email");
        return orderTransformer.transformToOrderListResponseFromList(response);
    }
}
