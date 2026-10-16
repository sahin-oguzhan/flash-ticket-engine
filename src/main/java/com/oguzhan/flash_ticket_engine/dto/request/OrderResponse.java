package com.oguzhan.flash_ticket_engine.dto.request;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import com.oguzhan.flash_ticket_engine.domain.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OrderResponse(
        Long orderId,
        Long ticketId,
        String userId,
        Integer quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .ticketId(order.getTicket().getId())
                .userId(order.getUserId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
