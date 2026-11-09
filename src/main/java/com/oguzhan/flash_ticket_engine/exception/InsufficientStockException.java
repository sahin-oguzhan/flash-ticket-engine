package com.oguzhan.flash_ticket_engine.exception;

public class InsufficientStockException extends BusinessException{
    public InsufficientStockException(int available) {
        super(String.format("Yetersiz stok! Mevcut: " + available));
    }
}
