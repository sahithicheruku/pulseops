package com.pulseops.transaction_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.pulseops.transaction_service.controller.TransactionController;
import com.pulseops.transaction_service.service.TransactionService;
import com.pulseops.transaction_service.service.TransactionRateLimiter;

@WebMvcTest(TransactionController.class)
class TransactionServiceApplicationTests {

	@MockitoBean
	private TransactionService service;

	@MockitoBean
	private TransactionRateLimiter rateLimiter;

	@Test
	void contextLoads() {
	}

}
