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
public class TicketDistributedLockServiceTest {

    @Autowired
    private TicketDistributedLockService ticketDistributedLockService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository  orderRepository;

    private Long savedTicketId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        ticketRepository.deleteAll();

        Ticket ticket = Ticket.builder()
                .eventName("Manifest İzmir 2026 - Redis Distributed Lock")
                .totalCapacity(100)
                .availableCapacity(100)
                .price(new BigDecimal("2500.00"))
                .build();

        savedTicketId = ticketRepository.save(ticket).getId();
    }

    @Test
    @DisplayName("Redisson Distributed Lock altında 200 eşzamanlı istek tam 100 bilet satmalı, 0 over-selling ve 0 versiyon çakışması üretmelidir.")
    void shouldDemonstratedDistributedLockingWithoutOverSelling() throws InterruptedException {
        int numberOfThreads = 200;
        int poolSize = 32;
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        CountDownLatch readyLatch = new CountDownLatch(poolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger outOfStockFailures = new AtomicInteger(0);
        AtomicInteger lockTimeoutOrOtherFailures = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user-" + i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    BuyTicketRequest request = new BuyTicketRequest(userId, 1);
                    ticketDistributedLockService.buyTicket(savedTicketId, request);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Yetersiz stok")) {
                        outOfStockFailures.incrementAndGet();
                    }
                } catch (Exception e) {
                    lockTimeoutOrOtherFailures.incrementAndGet();
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
        long confirmedOrdersCount = orderRepository.sumQuantityByTicketIdAndStatus(savedTicketId, OrderStatus.CONFIRMED);

        System.out.println("==================================================");
        System.out.println("REDIS DISTRIBUTED LOCK METRİKLERİ");
        System.out.println("Başlangıç Stoğu               : 100");
        System.out.println("Toplam İstek Sayısı           : " + numberOfThreads);
        System.out.println("Başarılı Satış                : " + successCount.get());
        System.out.println("Yetersiz Stok Hatası          : " + outOfStockFailures.get());
        System.out.println("Kilit / Diğer Hatalar         : " + lockTimeoutOrOtherFailures.get());
        System.out.println("DB Onaylı Sipariş Sayısı      : " + confirmedOrdersCount);
        System.out.println("DB Toplam Satılan Bilet       : " + totalSoldTickets);
        System.out.println("DB'de Kalan Son Stok          : " + finalTicket.getAvailableCapacity());
        System.out.println("DB Son Version Değeri         : " + finalTicket.getVersion());
        System.out.println("==================================================");

        assertThat(totalSoldTickets).isEqualTo(100);
        assertThat(successCount.get()).isEqualTo(100);

        assertThat(finalTicket.getAvailableCapacity()).isEqualTo(0);

        assertThat(finalTicket.getVersion()).isEqualTo(100L);

        assertThat(outOfStockFailures.get()).isEqualTo(100);
        assertThat(lockTimeoutOrOtherFailures.get()).isEqualTo(0);
    }
}
