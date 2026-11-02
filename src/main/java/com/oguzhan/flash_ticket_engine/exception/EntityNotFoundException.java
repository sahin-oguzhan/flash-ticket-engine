package com.oguzhan.flash_ticket_engine.exception;

import jakarta.persistence.Entity;

public class EntityNotFoundException extends RuntimeException{
    public EntityNotFoundException(String message) {
        super(message);
    }
}
