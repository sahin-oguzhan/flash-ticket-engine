package com.oguzhan.flash_ticket_engine.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuyTicketRequest(
        @NotBlank(message = "userId boş bırakılamaz")
        String userId,

        @NotNull(message = "quantity boş bırakılamaz")
        @Min(value = 1, message = "En az 1 bilet satın alınmalıdır")
        Integer quantity
) {
}
