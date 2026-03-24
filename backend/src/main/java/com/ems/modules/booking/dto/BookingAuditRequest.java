package com.ems.modules.booking.dto;

import jakarta.validation.constraints.NotNull;

public record BookingAuditRequest(
        @NotNull Long approverId,
        String reason
) {
}

