package com.oguzhan.flash_ticket_engine.dto.response;

public record AsyncOrderResponse(
        String trackingId,
        String status,
        String message
) {
}
