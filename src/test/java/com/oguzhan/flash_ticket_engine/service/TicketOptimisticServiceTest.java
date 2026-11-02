package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=40",
        "spring.datasource.hikari.connection-timeout=20000"
})
public class TicketOptimisticServiceTest {

    @Autowired
    private TicketOptimisticService ticketOptimisticService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long savedTicketId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        ticketRepository.deleteAll();

        Ticket ticket = Ticket.builder()
                .eventName("Manifest İzmir 2026 - Optimistic Lcok")
                .totalCapacity(100)
                .availableCapacity(100)
                .price(new BigDecimal("2500.00"))
                .build();

        savedTicketId = ticketRepository.save(ticket).getId();
    }

    @Test
    @DisplayName("Optimistic Lock altında eşzamanlı istekler versiyon çakışması yaşamalı ve over-selling gerçekleşmemelidir")
    void shouldDemonstrateOptimisticLockingAndVersionConflicts() throws InterruptedException {
        int numberOfThreads = 200;
        int poolSize = 32;
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        CountDownLatch readyLatch = new CountDownLatch(poolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockFailures = new AtomicInteger(0);
        AtomicInteger otherFailures = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user-" + i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    BuyTicketRequest request = new BuyTicketRequest(userId, 1);
                    ticketOptimisticService.buyTicket(savedTicketId, request);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    optimisticLockFailures.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof ObjectOptimisticLockingFailureException ||
                            (e.getCause() != null && e.getCause().getCause() instanceof ObjectOptimisticLockingFailureException)) {
                        optimisticLockFailures.incrementAndGet();
                    } else {
                        otherFailures.incrementAndGet();
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();

        doneLatch.await();
        executorService.shutdown();

        Ticket finalTicket = ticketRepository.findById(savedTicketId).orElseThrow();
        int totalSoldTickets = orderRepository.sumQuantityByTicketIdAndStatus(savedTicketId, OrderStatus.CONFIRMED);
        long confirmedOrdersCount = orderRepository.countByTicketIdAndStatus(savedTicketId, OrderStatus.CONFIRMED);

        System.out.println("==================================================");
        System.out.println("OPTIMISTIC LOCK METRİKLERİ");
        System.out.println("Başlangıç Stoğu               : 100");
        System.out.println("Toplam İstek Sayısı           : " + numberOfThreads);
        System.out.println("Başarılı Çağrı                : " + successCount.get());
        System.out.println("Optimistic Lock Hatası        : " + optimisticLockFailures.get());
        System.out.println("Diğer Hatalar                 : " + otherFailures.get());
        System.out.println("DB Onaylı Sipariş Sayısı      : " + confirmedOrdersCount);
        System.out.println("DB Toplam Satılan Bilet       : " + totalSoldTickets);
        System.out.println("DB'de Kalan Son Stok          : " + finalTicket.getAvailableCapacity());
        System.out.println("DB Son Version Değeri         : " + finalTicket.getVersion());
        System.out.println("==================================================");

        assertThat(totalSoldTickets).isLessThanOrEqualTo(100);
        assertThat(totalSoldTickets).isEqualTo(successCount.get());
        assertThat(optimisticLockFailures.get()).isGreaterThan(0);
        assertThat(finalTicket.getVersion()).isEqualTo((long) successCount.get());
        assertThat(finalTicket.getAvailableCapacity() + totalSoldTickets).isEqualTo(100);
    }
}
