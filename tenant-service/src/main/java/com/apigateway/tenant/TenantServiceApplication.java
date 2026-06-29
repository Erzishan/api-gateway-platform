package com.apigateway.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TenantServiceApplication {

	public static void main(String[] args) {

		// Force UTC timezone before Spring starts
		// This prevents Windows "Asia/Calcutta" timezone conflict with PostgreSQL
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(TenantServiceApplication.class, args);
	}
}
