package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.FlashTicketEngineApplication;
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

@SpringBootTest(properties = {"spring.datasource.hikari.maximum-pool-size=40",
        "spring.datasource.hikari.connection-timeout=20000"})
public class TicketNaiveServiceTest {

    @Autowired
    private TicketNaiveService ticketNaiveService;

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
                .eventName("Manifest İzmir 2026")
                .totalCapacity(100)
                .availableCapacity(100)
                .price(new BigDecimal("2500.00"))
                .build();

        savedTicketId = ticketRepository.save(ticket).getId();
    }

    @Test
    @DisplayName("Eşzamanlı 200 istek altında kilitsiz naive servis over-selling yaşamalıdır.")
    void shouldDemonstrateRaceConditionAndOverSelling() throws InterruptedException {
        int numberOfThreads = 200;
        int poolSize = 32;
        ExecutorService  executorService = Executors.newFixedThreadPool(poolSize);

        CountDownLatch readyLatch = new CountDownLatch(poolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount =  new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String userId = "user-" + i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    BuyTicketRequest request = new BuyTicketRequest(userId, 1);
                    ticketNaiveService.buyTicket(savedTicketId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
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
        System.out.println("RACE CONDITION (BEFORE) METRİKLERİ");
        System.out.println("Başlangıç Stoğu         : 100");
        System.out.println("Toplam İstek Sayısı     : " + numberOfThreads);
        System.out.println("Başarılı Çağrı (Java)   : " + successCount.get());
        System.out.println("Hata Alan Çağrı (Java)  : " + failCount.get());
        System.out.println("DB Onaylı Sipariş Sayısı: " + confirmedOrdersCount);
        System.out.println("DB Toplam Satılan Bilet : " + totalSoldTickets);
        System.out.println("DB'de Kalan Son Stok    : " + finalTicket.getAvailableCapacity());
        System.out.println("==================================================");

        assertThat(totalSoldTickets).isGreaterThan(100);
    }
}
