package com.ems.modules.booking.dto;

import jakarta.validation.constraints.NotNull;

public record BookingCancelRequest(
        @NotNull Long userId,
        String reason
) {
}

