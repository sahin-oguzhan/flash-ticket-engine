package com.oguzhan.flash_ticket_engine.consumer;

import com.oguzhan.flash_ticket_engine.config.RabbitMQConfig;
import com.oguzhan.flash_ticket_engine.dto.event.OrderMessage;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processOrder(OrderMessage orderMessage) {
        log.info("Kuyruktan sipariş mesajı alındı ve işleniyor. CorrelationId: {}, TicketId: {}",
                orderMessage.orderCorrelationId(), orderMessage.ticketId());

        try {
            BuyTicketRequest request = new BuyTicketRequest(orderMessage.userId(), orderMessage.quantity());
            ticketDistributedLockService.buyTicket(orderMessage.ticketId(), request);

            log.info("Sipariş başarıyla tamamlandı. CorrelationId: {}",
                    orderMessage.orderCorrelationId());
        } catch (RuntimeException e) {
            log.warn("Sipariş işlenirken hata oluştu (CorrelationId: {}): {}",
                    orderMessage.orderCorrelationId(), e.getMessage());
        }
    }
}
