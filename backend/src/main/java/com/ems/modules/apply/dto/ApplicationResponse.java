package com.ems.modules.apply.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

public record ApplicationResponse(
        String type,
        Long id,
        Long applicantId,
        String title,
        String detail,
        LocalDate openDateStart,
        LocalDate openDateEnd,
        LocalTime dailyStartTime,
        LocalTime dailyEndTime,
        String slotPreset,
        String status,
        Long reviewerId,
        LocalDateTime reviewedAt,
        String reviewComment,
        LocalDateTime createdAt
) {
}

