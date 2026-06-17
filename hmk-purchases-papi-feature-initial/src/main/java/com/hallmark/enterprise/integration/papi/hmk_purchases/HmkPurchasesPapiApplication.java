package com.hallmark.enterprise.integration.papi.hmk_purchases;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import lombok.extern.slf4j.Slf4j;

/**
 * Main Spring Boot application class for Purchases Process API
 *
 * <p>
 * This application provides process API endpoints for order searches and integrates
 * with Orders, Shipping Tracking, and Store Info system APIs.
 * </p>
 *
 * © 2026 Hallmark. All rights reserved.
 *
 * @author Xtivia Team
 * @since 2026-01-06
 * @version 1.0.0
 */
@SpringBootApplication
@EnableFeignClients
@Slf4j
public class HmkPurchasesPapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HmkPurchasesPapiApplication.class, args);
	}

}
