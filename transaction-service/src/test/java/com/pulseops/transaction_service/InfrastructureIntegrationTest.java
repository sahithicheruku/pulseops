package com.pulseops.transaction_service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pulseops.transaction_service.event.TransactionCreatedEvent;
import com.pulseops.transaction_service.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "app.demo.username=test-user",
        "app.demo.password=test-password"
})
class InfrastructureIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pulseops")
            .withUsername("pulseops")
            .withPassword("pulseops_dev");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"));

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
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Autowired
    AnalyticsService analyticsService;

    @Test
    void connectsToPostgresRedisAndKafka() {
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);

        var transactionId = UUID.randomUUID();
        var createdAt = Instant.now().minusSeconds(30);
        var completedAt = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO transaction (id, amount, currency, description, status, created_at, updated_at,
                    failure_reason, retry_count, last_failure_at, trace_id, source)
                VALUES (?, 12.50, 'USD', 'Analytics integration', 3, ?, ?, NULL, 0, NULL, ?, 'USER')
                """, transactionId, Timestamp.from(createdAt), Timestamp.from(completedAt), "integration-trace-" + transactionId);
        jdbcTemplate.update("""
                INSERT INTO transaction_status_history (id, transaction_id, status, changed_at, failure_reason, trace_id)
                VALUES (?, ?, 'COMPLETED', ?, NULL, ?)
                """, UUID.randomUUID(), transactionId, Timestamp.from(completedAt), "integration-trace-" + transactionId);
        var analytics = analyticsService.getAnalytics("24h");
        assertThat(analytics.totalTransactions()).isEqualTo(1);
        assertThat(analytics.successfulTransactions()).isEqualTo(1);
        assertThat(analytics.currencyDistribution()).containsEntry("USD", 1L);
        assertThat(analytics.p95ProcessingLatencyMs()).isGreaterThan(0);

        var key = "integration:" + UUID.randomUUID();
        redisTemplate.opsForValue().set(key, "connected");
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("connected");

        var event = new TransactionCreatedEvent(
                UUID.randomUUID(), BigDecimal.TEN, "USD",
                com.pulseops.transaction_service.domain.TransactionStatus.RECEIVED,
                Instant.now());
        kafkaTemplate.send("transaction.created", event.transactionId().toString(), event).join();

        var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "integration-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        try (consumer) {
            consumer.subscribe(java.util.List.of("transaction.created"));
            var records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records).anyMatch(record -> record.key().equals(event.transactionId().toString()));
        }
    }
}
