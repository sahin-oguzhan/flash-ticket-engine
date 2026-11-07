package com.oguzhan.flash_ticket_engine.controller;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.response.OrderResponse;
import com.oguzhan.flash_ticket_engine.service.TicketDistributedLockService;
import com.oguzhan.flash_ticket_engine.service.TicketNaiveService;
import com.oguzhan.flash_ticket_engine.service.TicketOptimisticService;
import com.oguzhan.flash_ticket_engine.service.TicketPessimisticService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketNaiveService ticketNaiveService;
    private final TicketPessimisticService  ticketPessimisticService;
    private final TicketOptimisticService ticketOptimisticService;
    private final TicketDistributedLockService  ticketDistributedLockService;

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
