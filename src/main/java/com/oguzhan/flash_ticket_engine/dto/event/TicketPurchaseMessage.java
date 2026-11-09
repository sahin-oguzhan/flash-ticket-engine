package com.oguzhan.flash_ticket_engine.dto.event;

import java.io.Serializable;

public record TicketPurchaseMessage(
        Long ticketId,
        String userId,
        String idempotencyKey
) implements Serializable {
}
