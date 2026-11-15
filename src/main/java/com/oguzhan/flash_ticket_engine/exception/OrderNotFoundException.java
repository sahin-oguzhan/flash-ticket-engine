package com.oguzhan.flash_ticket_engine.exception;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String trackingId) {
        super("Sipariş bulunamadı: " + trackingId);
    }
}
