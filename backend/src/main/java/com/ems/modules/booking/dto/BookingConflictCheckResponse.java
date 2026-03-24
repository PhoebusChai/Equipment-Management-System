package com.ems.modules.booking.dto;

public record BookingConflictCheckResponse(
        boolean hasConflict,
        long conflictCount
) {
}

