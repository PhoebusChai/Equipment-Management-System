package com.ems.modules.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingCreateRequest(
        @NotNull Long userId,
        @NotBlank String resourceType,
        @NotNull Long resourceId,
        @NotBlank String bookingScope,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime
) {
}

