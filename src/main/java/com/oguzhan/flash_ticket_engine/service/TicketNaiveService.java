package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.request.OrderResponse;
import com.oguzhan.flash_ticket_engine.exception.EntityNotFoundException;
import com.oguzhan.flash_ticket_engine.exception.InsufficientTicketCapacityException;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketNaiveService {
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse buyTicket(Long ticketId, BuyTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Bilet bulunamadı: ID = " + ticketId));

        if (ticket.getAvailableCapacity() < request.quantity()) {
            log.warn("Yetersiz stok! Mevcut: {}, İstenen: {}, User: {}",
                    ticket.getAvailableCapacity(), request.quantity(), request.userId());
            throw new InsufficientTicketCapacityException("Yetersiz bilet stoğu");
        }

        ticket.setAvailableCapacity(ticket.getAvailableCapacity() - request.quantity());
        ticketRepository.save(ticket);

        BigDecimal totalPrice = ticket.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        Order order = Order.builder()
                .ticket(ticket)
                .userId(request.userId())
                .quantity(request.quantity())
                .totalPrice(totalPrice)
                .status(OrderStatus.CONFIRMED)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Sipariş onaylandı: OrderID = {}, Kalan Stok = {}", savedOrder.getId(), ticket.getAvailableCapacity());

        return OrderResponse.from(savedOrder);
    }
}
