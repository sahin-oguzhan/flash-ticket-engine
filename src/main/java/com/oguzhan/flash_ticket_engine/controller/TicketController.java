package com.oguzhan.flash_ticket_engine.controller;

import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.request.OrderResponse;
import com.oguzhan.flash_ticket_engine.service.TicketNaiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketNaiveService ticketNaiveService;

    @PostMapping("/{ticketId}/buy-naive")
    public ResponseEntity<OrderResponse> buyTicketNaive(
            @PathVariable Long ticketId,
            @Valid @RequestBody BuyTicketRequest request
            ) {
        OrderResponse response = ticketNaiveService.buyTicket(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
