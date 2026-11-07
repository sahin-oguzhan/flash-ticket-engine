package com.oguzhan.flash_ticket_engine.exception;

import com.oguzhan.flash_ticket_engine.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (e.getMessage() != null && e.getMessage().contains("yoğun")) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (e.getMessage() != null && e.getMessage().contains("Yetersiz stok")) {
            status = HttpStatus.CONFLICT;
        }

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                e.getMessage()
        );

        return ResponseEntity.status(status).body(response);
    }
}
