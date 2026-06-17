package com.hallmark.enterprise.integration.papi.hmk_purchases.client.storeinfo;

import org.springframework.context.annotation.Bean;

import com.hallmark.integration.common.security.client.FeignOAuth2RequestInterceptorFactory;

import feign.Logger;
import feign.RequestInterceptor;

/**
 * StoreInfoFeignClientConfig - Configuration for Store Info System API Feign Client
 *
 * <p>
 * This class is responsible for configuring the Store Info Feign Client used to interact with the
 * Store Info System API.
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
public class StoreInfoFeignClientConfig {

    /**
     * Configures Feign Logger level to FULL for detailed request/response logging.
     *
     * @return Logger.Level.FULL for complete logging
     */
    @Bean
    public Logger.Level feignStoreLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Creates a RequestInterceptor for Store Info System API using OAuth2.
     *
     * @param factory the FeignOAuth2RequestInterceptorFactory to create the interceptor
     * @return a RequestInterceptor configured for Store Info System API
     */
    @Bean
    public RequestInterceptor storeInfoRequestInterceptor(FeignOAuth2RequestInterceptorFactory factory) {
        return factory.createRequestInterceptor("store-info-sapi", "system");
    }
}
