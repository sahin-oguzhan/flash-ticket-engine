package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.response.OrderResponse;
import com.oguzhan.flash_ticket_engine.exception.InsufficientStockException;
import com.oguzhan.flash_ticket_engine.exception.LockAcquisitionException;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketDistributedLockService {

    private final RedissonClient redissonClient;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    private static final String LOCK_KEY_PREFIX = "lock:ticket:";

    public OrderResponse buyTicket(Long ticketId, BuyTicketRequest request) {
        String lockKey = LOCK_KEY_PREFIX + ticketId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(30, -1, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("Kilit edinilemedi! Yoğun trafik altında istek zaman aşımına uğradı. TicketId: {}", ticketId);
                throw new LockAcquisitionException("Sistem şu anda çok yoğun, lütfen tekrar deneyiniz.");
            }

            return transactionTemplate.execute(status -> {
                Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new RuntimeException("Bilet bulunamadı: " + ticketId));

                if (ticket.getAvailableCapacity() < request.quantity()) {
                    throw new InsufficientStockException(ticket.getAvailableCapacity());
                }

                ticket.setAvailableCapacity(ticket.getAvailableCapacity() - request.quantity());
                ticketRepository.save(ticket);

                Order order = Order.builder()
                        .ticket(ticket)
                        .userId(request.userId())
                        .quantity(request.quantity())
                        .totalPrice(ticket.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                        .status(OrderStatus.CONFIRMED)
                        .build();

                Order savedOrder = orderRepository.save(order);

                return new OrderResponse(
                        savedOrder.getId(),
                        ticket.getId(),
                        savedOrder.getUserId(),
                        savedOrder.getQuantity(),
                        savedOrder.getTotalPrice(),
                        savedOrder.getStatus(),
                        savedOrder.getCreatedAt()
                );
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Kilit bekleme sırasında kesintiye uğradı");
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
