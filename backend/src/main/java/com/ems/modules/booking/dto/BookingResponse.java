package com.ems.modules.booking.dto;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String bookingNo,
        Long userId,
        String resourceType,
        Long resourceId,
        String resourceName,
        String bookingScope,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        String status,
        Long approverId,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

