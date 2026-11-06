package com.oguzhan.flash_ticket_engine.controller;

import com.oguzhan.flash_ticket_engine.dto.event.OrderMessage;
import com.oguzhan.flash_ticket_engine.dto.request.BuyTicketRequest;
import com.oguzhan.flash_ticket_engine.dto.response.AsyncOrderResponse;
import com.oguzhan.flash_ticket_engine.producer.TicketOrderProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/tickets")
@RequiredArgsConstructor
public class TicketAsyncController {

    private final TicketOrderProducer  ticketOrderProducer;

    @PostMapping("/{ticketId}/buy-async")
    public ResponseEntity<AsyncOrderResponse> buyTicketAsync(
            @PathVariable Long ticketId,
            @RequestBody BuyTicketRequest request
    ) {
        String trackingId = UUID.randomUUID().toString();

        OrderMessage orderMessage = new OrderMessage(
                trackingId,
                ticketId,
                request.userId(),
                request.quantity(),
                LocalDateTime.now()
        );

        ticketOrderProducer.sendOrderMessage(orderMessage);

        AsyncOrderResponse response = new AsyncOrderResponse(
                trackingId,
                "ACCEPTED",
                "Bilet alma talebiniz sıraya alındı, arka planda işleniyor."
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
