package com.oguzhan.flash_ticket_engine.dto.response;

import com.oguzhan.flash_ticket_engine.domain.entity.Ticket;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TicketResponse(
        Long id,
        String eventName,
        Integer totalCapacity,
        Integer availableCapacity,
        BigDecimal price,
        LocalDateTime createdAt
) {
    public static TicketResponse from(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .eventName(ticket.getEventName())
                .totalCapacity(ticket.getTotalCapacity())
                .availableCapacity(ticket.getAvailableCapacity())
                .price(ticket.getPrice())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
