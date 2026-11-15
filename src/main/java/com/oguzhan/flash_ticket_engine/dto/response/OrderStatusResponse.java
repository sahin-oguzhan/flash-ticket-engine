package com.oguzhan.flash_ticket_engine.dto.response;

import com.oguzhan.flash_ticket_engine.domain.entity.Order;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OrderStatusResponse(
        String trackingId,
        Long orderId,
        Long ticketId,
        String userId,
        Integer quantity,
        BigDecimal totalPrice,
        String status,
        String message,
        LocalDateTime createdAt
) {
    public static OrderStatusResponse from(Order order) {
        return OrderStatusResponse.builder()
                .trackingId(order.getTrackingId())
                .orderId(order.getId())
                .ticketId(order.getTicket().getId())
                .userId(order.getUserId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .message("Sipariş başarıyla tamamlandı.")
                .createdAt(order.getCreatedAt())
                .build();
    }
}
