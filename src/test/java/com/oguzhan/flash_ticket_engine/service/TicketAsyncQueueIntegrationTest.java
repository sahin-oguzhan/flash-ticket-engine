package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.config.RabbitMQConfig;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.event.OrderMessage;
import com.oguzhan.flash_ticket_engine.producer.TicketOrderProducer;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TicketAsyncQueueIntegrationTest {

    @Autowired
    private TicketOrderProducer ticketOrderProducer;

    @Autowired
    private TicketRepository  ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    private Long savedTicketId;

    @BeforeEach
    void setUp() throws InterruptedException {
        rabbitAdmin.purgeQueue(RabbitMQConfig.QUEUE_NAME);
        rabbitAdmin.purgeQueue(RabbitMQConfig.DLQ_QUEUE_NAME);

        TimeUnit.MILLISECONDS.sleep(500);


        orderRepository.deleteAll();
        ticketRepository.deleteAll();

        Ticket ticket = Ticket.builder()
                .eventName("Manifest İzmir 2026 - RabbitMq Event-Driven")
                .totalCapacity(100)
                .availableCapacity(100)
                .price(new BigDecimal("2500.00"))
                .build();

        savedTicketId = ticketRepository.save(ticket).getId();
    }

    @Test
    @DisplayName("RabbitMQ kuyruğuna atılan 200 istek asenkron worker tarafından tam 100 bilet satacak şekilde işlenmeli, 0 over-selling üretmelidir.")
    void shouldProcessOrdersAsynchronouslyWithoutOverSelling() throws InterruptedException {
        int totalRequests = 200;

        for (int i = 0; i < totalRequests; i++) {
            OrderMessage message = new OrderMessage(
                    UUID.randomUUID().toString(),
                    savedTicketId,
                    "async-user-" + i,
                    1,
                    LocalDateTime.now()
            );
            ticketOrderProducer.sendOrderMessage(message);
        }

        long maxWaitTimeMs = 25000;
        long pollIntervalMs = 300;
        long elapsed = 0;
        int totalSold = 0;

        while (elapsed < maxWaitTimeMs) {
            totalSold = orderRepository.sumQuantityByTicketIdAndStatus(savedTicketId, OrderStatus.CONFIRMED);
            if (totalSold >= 100) {TimeUnit.MILLISECONDS.sleep(2500);
                break;
            }
            TimeUnit.MILLISECONDS.sleep(pollIntervalMs);
            elapsed += pollIntervalMs;
        }

        Ticket finalTicket = ticketRepository.findById(savedTicketId).orElseThrow();
        int finalTotalSoldTickets = orderRepository.sumQuantityByTicketIdAndStatus(savedTicketId, OrderStatus.CONFIRMED);
        long confirmedOrdersCount = orderRepository.countByTicketIdAndStatus(savedTicketId, OrderStatus.CONFIRMED);

        System.out.println("==================================================");
        System.out.println("RABBITMQ ASYNC QUEUE METRİKLERİ");
        System.out.println("Başlangıç Stoğu               : 100");
        System.out.println("Kuyruğa Gönderilen Mesaj      : " + totalRequests);
        System.out.println("DB Onaylı Sipariş Sayısı      : " + confirmedOrdersCount);
        System.out.println("DB Toplam Satılan Bilet       : " + finalTotalSoldTickets);
        System.out.println("DB'de Kalan Son Stok          : " + finalTicket.getAvailableCapacity());
        System.out.println("DB Son Version Değeri         : " + finalTicket.getVersion());
        System.out.println("==================================================");

        assertThat(finalTotalSoldTickets).isEqualTo(100);
        assertThat(finalTicket.getAvailableCapacity()).isEqualTo(0);
        assertThat(confirmedOrdersCount).isEqualTo(100);
    }
}
