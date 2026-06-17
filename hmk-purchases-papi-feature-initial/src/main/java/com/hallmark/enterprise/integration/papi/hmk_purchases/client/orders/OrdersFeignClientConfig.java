package com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders;

import org.springframework.context.annotation.Bean;

import com.hallmark.integration.common.security.client.FeignOAuth2RequestInterceptorFactory;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrdersFeignClientConfig - Configuration for Orders Omni MANH System API Feign Client
 *
 * <p>
 * This class is responsible for configuring the Orders Omni MANH Feign Client used to interact
 * with the Orders Omni MANH System API.
 * </p>
 *
 * <p>
 * It includes settings such as OAuth2 authentication, timeouts, and other client-specific
 * configurations.
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@Slf4j
public class OrdersFeignClientConfig {

    /**
     * Creates a RequestInterceptor for Orders Omni MANH System API using OAuth2.
     *
     * @param factory the FeignOAuth2RequestInterceptorFactory to create the interceptor
     * @return a RequestInterceptor configured for Orders Omni MANH System API
     */
    @Bean
    public RequestInterceptor ordersRequestInterceptor(FeignOAuth2RequestInterceptorFactory factory) {
        log.info("Creating OAuth2 Request Interceptor for orders-omni-manh-sapi");
        return factory.createRequestInterceptor("orders-omni-manh-sapi", "system");
    }
}
