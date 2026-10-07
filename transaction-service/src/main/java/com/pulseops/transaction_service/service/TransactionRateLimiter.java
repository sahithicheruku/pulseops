package com.pulseops.transaction_service.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final int limit;
    private final Duration window;

    public TransactionRateLimiter(StringRedisTemplate redisTemplate,
                                  @Value("${app.rate-limit.transactions:10}") int limit,
                                  @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean allow(String clientId) {
        var key = "transaction:rate-limit:" + clientId;
        var count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= limit;
    }
}
