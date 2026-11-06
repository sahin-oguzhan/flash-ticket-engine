package com.oguzhan.flash_ticket_engine.dto.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record OrderMessage(
        String orderCorrelationId,
        Long ticketId,
        String userId,
        Integer quantity,
        LocalDateTime requestedAt
) implements Serializable {
}
