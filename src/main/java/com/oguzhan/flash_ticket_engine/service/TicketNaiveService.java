package com.oguzhan.flash_ticket_engine.service;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.entity.OrderNaive;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.domain.entity.TicketNaive;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.request.OrderResponse;
import com.oguzhan.flash_ticket_engine.exception.EntityNotFoundException;
import com.oguzhan.flash_ticket_engine.exception.InsufficientTicketCapacityException;
import com.oguzhan.flash_ticket_engine.repository.OrderNaiveRepository;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketNaiveRepository;
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
    private final TicketNaiveRepository  ticketNaiveRepository;
    private final OrderNaiveRepository  orderNaiveRepository;

    @Transactional
    public OrderResponse buyTicket(Long ticketId, BuyTicketRequest request) {
        TicketNaive ticket = ticketNaiveRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Bilet bulunamadı: ID = " + ticketId));

        if (ticket.getAvailableCapacity() < request.quantity()) {
            log.warn("Yetersiz stok! Mevcut: {}, İstenen: {}, User: {}",
                    ticket.getAvailableCapacity(), request.quantity(), request.userId());
            throw new InsufficientTicketCapacityException("Yetersiz bilet stoğu");
        }

        ticket.setAvailableCapacity(ticket.getAvailableCapacity() - request.quantity());
        ticketNaiveRepository.save(ticket);

        BigDecimal totalPrice = ticket.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

        OrderNaive order = OrderNaive.builder()
                .ticket(ticket)
                .userId(request.userId())
                .quantity(request.quantity())
                .totalPrice(totalPrice)
                .status(OrderStatus.CONFIRMED)
                .build();

        OrderNaive savedOrder = orderNaiveRepository.save(order);
        log.info("Sipariş onaylandı: OrderID = {}, Kalan Stok = {}", savedOrder.getId(), ticket.getAvailableCapacity());

        return new OrderResponse(
                savedOrder.getId(),
                ticketId,
                savedOrder.getUserId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt()
        );
    }
}
