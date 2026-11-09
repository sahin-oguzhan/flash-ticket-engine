package com.oguzhan.flash_ticket_engine.consumer;

import com.oguzhan.flash_ticket_engine.config.RabbitMQConfig;
import com.oguzhan.flash_ticket_engine.dto.event.OrderMessage;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.exception.BusinessException;
import com.oguzhan.flash_ticket_engine.service.IdempotencyService;
import com.oguzhan.flash_ticket_engine.service.TicketDistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketOrderConsumer {

    private final TicketDistributedLockService ticketDistributedLockService;
    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processOrder(OrderMessage orderMessage) {
        String key = orderMessage.orderCorrelationId();

        if (!idempotencyService.lockOrAcquire(key)) {
            log.warn("Mükerrer sipariş mesajı tespit edildi! İşlem atlanıyor. CorrelationId: {}, TicketId: {}", key, orderMessage.ticketId() );
            return;
        }

        log.info("Kuyruktan sipariş mesajı alındı ve işleniyor. CorrelationId: {}, TicketId: {}",
                key, orderMessage.ticketId());

        try {
            BuyTicketRequest request = new BuyTicketRequest(orderMessage.userId(), orderMessage.quantity());
            ticketDistributedLockService.buyTicket(orderMessage.ticketId(), request);

            idempotencyService.markCompleted(key);

            log.info("Sipariş başarıyla tamamlandı. CorrelationId: {}", key);
        } catch (BusinessException e) {
            log.warn("Sipariş tamamlanamadı (CorrelationId: {}): {}", key, e.getMessage());
            idempotencyService.markCompleted(key);

        } catch (Exception e) {

            log.error("Sistem hatası (CorrelationId: {}): {}", key, e.getMessage());
            idempotencyService.release(key);
            throw e;
            }
        }
    }

