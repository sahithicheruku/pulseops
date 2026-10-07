package com.pulseops.transaction_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "loadTest", matches = "true")
class TransactionCreationLoadTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pulseops")
            .withUsername("pulseops")
            .withPassword("pulseops_dev");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> false);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.rate-limit.transactions", () -> 1_000_000);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void measuresTransactionCreation() throws Exception {
        int requests = Integer.getInteger("loadTest.requests", 100);
        int concurrency = Integer.getInteger("loadTest.concurrency", 10);
        assertThat(requests).isPositive();
        assertThat(concurrency).isPositive();

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<Long>> futures = new ArrayList<>(requests);
        try {
            long loadStarted = System.nanoTime();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(this::createTransaction));
            }
            List<Long> latencies = new ArrayList<>(requests);
            for (Future<Long> future : futures) {
                latencies.add(future.get());
            }
            Collections.sort(latencies);

            double throughput = requests / ((System.nanoTime() - loadStarted) / 1_000_000_000.0);
            System.out.printf(
                    "LOAD_RESULT requests=%d concurrency=%d throughput=%.2f req/s p50=%d ms p95=%d ms p99=%d ms%n",
                    requests, concurrency, throughput,
                    percentile(latencies, 0.50), percentile(latencies, 0.95), percentile(latencies, 0.99));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private long createTransaction() {
        long started = System.nanoTime();
        try {
            mockMvc.perform(post("/api/v1/transactions")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .content("{\"amount\":12.50,\"currency\":\"USD\"}"))
                    .andExpect(status().isCreated());
            return System.nanoTime() - started;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static long percentile(List<Long> sortedNanos, double percentile) {
        int index = (int) Math.ceil(percentile * sortedNanos.size()) - 1;
        return Duration.ofNanos(sortedNanos.get(Math.max(0, index))).toMillis();
    }
}
