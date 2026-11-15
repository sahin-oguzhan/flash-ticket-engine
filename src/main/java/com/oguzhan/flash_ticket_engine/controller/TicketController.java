package com.oguzhan.flash_ticket_engine.controller;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.response.OrderResponse;
import com.oguzhan.flash_ticket_engine.dto.response.OrderStatusResponse;
import com.oguzhan.flash_ticket_engine.dto.response.TicketResponse;
import com.oguzhan.flash_ticket_engine.exception.OrderNotFoundException;
import com.oguzhan.flash_ticket_engine.exception.TicketNotFoundException;
import com.oguzhan.flash_ticket_engine.repository.OrderRepository;
import com.oguzhan.flash_ticket_engine.repository.TicketRepository;
import com.oguzhan.flash_ticket_engine.service.IdempotencyService;
import com.oguzhan.flash_ticket_engine.service.TicketDistributedLockService;
import com.oguzhan.flash_ticket_engine.service.TicketNaiveService;
import com.oguzhan.flash_ticket_engine.service.TicketOptimisticService;
import com.oguzhan.flash_ticket_engine.service.TicketPessimisticService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketNaiveService ticketNaiveService;
    private final TicketPessimisticService  ticketPessimisticService;
    private final TicketOptimisticService ticketOptimisticService;
    private final TicketDistributedLockService  ticketDistributedLockService;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @GetMapping("/orders/{trackingId}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable String trackingId) {
        return orderRepository.findByTrackingId(trackingId)
                .map(order -> ResponseEntity.ok(OrderStatusResponse.from(order)))
                .orElseGet(() -> {
                    String idempotencyStatus = idempotencyService.getStatus(trackingId);
                    if ("PROCESSING".equals(idempotencyStatus)) {
                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                                OrderStatusResponse.builder()
                                        .trackingId(trackingId)
                                        .status("PROCESSING")
                                        .message("Siparişiniz şu anda işleniyor.")
                                        .build()
                        );
                    }
                    if ("COMPLETED".equals(idempotencyStatus)) {
                        return ResponseEntity.ok(
                                OrderStatusResponse.builder()
                                        .trackingId(trackingId)
                                        .status("FAILED")
                                        .message("Sipariş tamamlanamadı (yetersiz stok veya geçersiz bilet).")
                                        .build()
                        );
                    }
                    throw new OrderNotFoundException(trackingId);
                });
    }

    @PostMapping("/{ticketId}/buy-naive")
    public ResponseEntity<OrderResponse> buyTicketNaive(
            @PathVariable Long ticketId,
            @Valid @RequestBody BuyTicketRequest request
            ) {
        OrderResponse response = ticketNaiveService.buyTicket(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{ticketId}/buy-pessimistic")
    public ResponseEntity<OrderResponse> buyTicketPessimistic(
            @PathVariable Long ticketId,
            @Valid @RequestBody BuyTicketRequest request
    ) {
        OrderResponse response = ticketPessimisticService.buyTicket(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{ticketId}/buy-optimistic")
    public ResponseEntity<OrderResponse> buyTicketOptimistic(
            @PathVariable Long ticketId,
            @Valid @RequestBody BuyTicketRequest request
    ) {
        OrderResponse response = ticketOptimisticService.buyTicket(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{ticketId}/buy-redis-lock")
    public ResponseEntity<OrderResponse> buyTicketRedisLock(
            @PathVariable Long ticketId,
            @Valid @RequestBody BuyTicketRequest request
    ) {
        OrderResponse response = ticketDistributedLockService.buyTicket(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
