package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
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
public class TicketOptimisticService {

    private final TicketRepository  ticketRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void buyTicket(Long ticketId, BuyTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Bilet bulunamadı: " + ticketId));

        if (ticket.getAvailableCapacity() < request.quantity()) {
            throw new RuntimeException("Yetersiz stok! Mevcut: " + ticket.getAvailableCapacity());
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

        orderRepository.save(order);
    }
}
