package com.oguzhan.flash_ticket_engine.consumer;

import com.oguzhan.flash_ticket_engine.dto.event.OrderMessage;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.exception.InsufficientStockException;
import com.oguzhan.flash_ticket_engine.service.IdempotencyService;
import com.oguzhan.flash_ticket_engine.service.TicketDistributedLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;


@ExtendWith(MockitoExtension.class)
public class TicketOrderConsumerTest {

    @Mock
    private TicketDistributedLockService ticketDistributedLockService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private TicketOrderConsumer ticketOrderConsumer;

    private final OrderMessage orderMessage = new OrderMessage("order-corr-1", 10L, "user-1", 1, LocalDateTime.now());

    @Test
    @DisplayName("Başarılı siparişte kilit alınmalı, bilet satılmalı ve işlem tamamlandı işaretlenmelidir")
    void shouldProcessOrderSuccessfully() {
        when(idempotencyService.lockOrAcquire("order-corr-1")).thenReturn(true);

        ticketOrderConsumer.processOrder(orderMessage);

        verify(ticketDistributedLockService).buyTicket(eq(10L), any(BuyTicketRequest.class));
        verify(idempotencyService).markCompleted("order-corr-1");
        verify(idempotencyService, never()).release(any());
    }

    @Test
    @DisplayName("Mükerrer istek geldiğinde sipariş işleme metodu hiç çağrılmamalıdır")
    void shouldSkipProcessingWhenDuplicateMessageReceived() {
        when(idempotencyService.lockOrAcquire("order-corr-1")).thenReturn(false);

        ticketOrderConsumer.processOrder(orderMessage);

        verifyNoInteractions(ticketDistributedLockService);
        verify(idempotencyService, never()).markCompleted(any());
        verify(idempotencyService, never()).release(any());
    }

    @Test
    @DisplayName("Stok yetersiz olduğunda (BusinessException) kilit tamamlandı işaretlenmeli ve retry edilmemelidir")
    void shouldNotRetryWhenBusinessExceptionOccurs() {
        when(idempotencyService.lockOrAcquire("order-corr-1")).thenReturn(true);
        doThrow(new InsufficientStockException(0))
                .when(ticketDistributedLockService).buyTicket(eq(10L), any(BuyTicketRequest.class));

        ticketOrderConsumer.processOrder(orderMessage);

        verify(idempotencyService).markCompleted("order-corr-1");
        verify(idempotencyService, never()).release(any());
    }

    @Test
    @DisplayName("Beklenmedik bir sistem hatasında kilit serbest bırakılmalı ve retry için exception fırlatılmalıdır")
    void shouldReleaseLockAndThrowExceptionOnUnexpectedFailure() {
        when(idempotencyService.lockOrAcquire("order-corr-1")).thenReturn(true);
        doThrow(new RuntimeException("Database connection timeout!"))
                .when(ticketDistributedLockService).buyTicket(eq(10L), any(BuyTicketRequest.class));

        assertThatThrownBy(() -> ticketOrderConsumer.processOrder(orderMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection timeout!");

        verify(idempotencyService).release("order-corr-1");
        verify(idempotencyService, never()).markCompleted(any());
    }
}
