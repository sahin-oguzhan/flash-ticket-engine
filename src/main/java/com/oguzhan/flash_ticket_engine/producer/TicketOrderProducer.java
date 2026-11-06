package com.oguzhan.flash_ticket_engine.producer;

import com.oguzhan.flash_ticket_engine.config.RabbitMQConfig;
import com.oguzhan.flash_ticket_engine.dto.event.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendOrderMessage(OrderMessage orderMessage) {
        log.info("Sipariş talebi kuyruğa gönderiliyor. CorrelationId: {}, TicketId: {}, UserId: {}",
                orderMessage.orderCorrelationId(), orderMessage.ticketId(), orderMessage.userId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                orderMessage
        );
    }
}
