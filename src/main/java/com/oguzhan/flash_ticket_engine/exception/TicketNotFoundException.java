package com.oguzhan.flash_ticket_engine.exception;

public class TicketNotFoundException extends BusinessException{
    public TicketNotFoundException(Long ticketId) {
        super("Bilet bulunamadı: " + ticketId);
    }
}
