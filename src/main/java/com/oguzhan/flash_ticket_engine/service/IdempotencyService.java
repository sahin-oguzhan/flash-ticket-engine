package com.oguzhan.flash_ticket_engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:ticket:";
    private static final Duration LOCK_EXPIRATION = Duration.ofMinutes(10);

    public boolean lockOrAcquire(String key) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", LOCK_EXPIRATION);
        return Boolean.TRUE.equals(success);
    }

    public void markCompleted(String key) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        stringRedisTemplate.opsForValue().set(redisKey, "COMPLETED", Duration.ofDays(1));
    }

    public void release(String key) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        stringRedisTemplate.delete(redisKey);
    }
}
