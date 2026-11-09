package com.oguzhan.flash_ticket_engine.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private static final String TEST_KEY = "test-order-key-123";
    private static final String REDIS_FULL_KEY = "idempotency:ticket:" + TEST_KEY;

    @Test
    @DisplayName("İlk istek geldiğinde SETNX başarılı olmalı ve true dönmelidir")
    void shouldAcquireLockWhenKeyDoesNotExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(REDIS_FULL_KEY), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        boolean acquired = idempotencyService.lockOrAcquire(TEST_KEY);

        assertThat(acquired).isTrue();
        verify(valueOperations).setIfAbsent(eq(REDIS_FULL_KEY), eq("PROCESSING"), any(Duration.class));
    }

    @Test
    @DisplayName("Mükerrer istek geldiğinde SETNX false dönmeli ve kilit alınamamalıdır")
    void shouldNotAcquireLockWhenKeyAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(REDIS_FULL_KEY), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        boolean acquired = idempotencyService.lockOrAcquire(TEST_KEY);

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("Sipariş tamamlandığında COMPLETED durumuna çekilmeli ve TTL uzatılmalıdır")
    void shouldMarkAsCompleted() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        idempotencyService.markCompleted(TEST_KEY);

        verify(valueOperations).set(eq(REDIS_FULL_KEY), eq("COMPLETED"), any(Duration.class));
    }

    @Test
    @DisplayName("Beklenmeyen hata anında release çağrıldığında anahtar Redis'ten silinmelidir")
    void shouldReleaseKey() {
        idempotencyService.release(TEST_KEY);

        verify(redisTemplate).delete(REDIS_FULL_KEY);
    }
}
