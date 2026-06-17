package com.hallmark.enterprise.integration.papi.hmk_purchases.config;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;

import com.hallmark.enterprise.integration.papi.hmk_purchases.client.orders.OrdersOmniManhFeignClient;
import com.hallmark.enterprise.integration.papi.hmk_purchases.client.storeinfo.StoreInfoFeignClient;

/**
 * Native runtime configuration for the application.
 * 
 * <p>
 * This class configures runtime hints for native image generation using GraalVM,
 * ensuring that necessary classes and resources are available at runtime.
 * This includes hints for Feign clients, model classes, and configuration properties.
 * </p>
 * 
 * © 2026 Hallmark. All rights reserved.
 * 
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@Configuration
@ImportRuntimeHints({ NativeRuntimeHints.class, FeignRuntimeHints.class })
public class NativeRuntimeConfig {
}

/**
 * Runtime hints for application-specific classes and resources
 */
class NativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register application properties for reflection
        hints.resources().registerPattern("application*.properties");
        hints.resources().registerPattern("application*.yml");
        
        // Register OpenAPI resources
        hints.resources().registerPattern("openapi/**/*.yaml");
        hints.resources().registerPattern("openapi/**/*.json");
    }
}

/**
 * Runtime hints for Feign clients to enable native compilation
 */
class FeignRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register proxies for Feign clients
        hints.proxies().registerJdkProxy(
                OrdersOmniManhFeignClient.class,
                SpringProxy.class,
                Advised.class,
                DecoratingProxy.class
        );
        
        hints.proxies().registerJdkProxy(
                StoreInfoFeignClient.class,
                SpringProxy.class,
                Advised.class,
                DecoratingProxy.class
        );

        // Register reflection for Feign client interfaces
        hints.reflection().registerType(
                OrdersOmniManhFeignClient.class,
                MemberCategory.INVOKE_PUBLIC_METHODS
        );
        
        hints.reflection().registerType(
                StoreInfoFeignClient.class,
                MemberCategory.INVOKE_PUBLIC_METHODS
        );
    }
}
