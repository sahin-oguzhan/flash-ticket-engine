package com.oguzhan.flash_ticket_engine.exception;

public class InsufficientTicketCapacityException extends RuntimeException
{
    public InsufficientTicketCapacityException(String message)
    {
        super(message);
    }
}
