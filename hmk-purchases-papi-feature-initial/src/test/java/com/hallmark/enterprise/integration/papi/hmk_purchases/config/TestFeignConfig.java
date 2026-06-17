package com.hallmark.enterprise.integration.papi.hmk_purchases.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.hallmark.integration.common.security.client.FeignOAuth2RequestInterceptorFactory;

import feign.RequestInterceptor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TestFeignConfig - Test configuration for Feign clients
 *
 * <p>
 * This configuration provides mock implementations of Feign-related beans
 * that are required by the application context but are not needed for testing.
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-07
 * @version 1.0.0
 */
@TestConfiguration
public class TestFeignConfig {

    /**
     * Provides a mock FeignOAuth2RequestInterceptorFactory for testing.
     * This bean is marked as @Primary to override the production configuration.
     *
     * @return a mock FeignOAuth2RequestInterceptorFactory
     */
    @Bean
    @Primary
    public FeignOAuth2RequestInterceptorFactory mockFeignOAuth2RequestInterceptorFactory() {
        FeignOAuth2RequestInterceptorFactory factory = mock(FeignOAuth2RequestInterceptorFactory.class);
        RequestInterceptor mockInterceptor = mock(RequestInterceptor.class);
        when(factory.createRequestInterceptor(anyString(), anyString())).thenReturn(mockInterceptor);
        return factory;
    }
}
