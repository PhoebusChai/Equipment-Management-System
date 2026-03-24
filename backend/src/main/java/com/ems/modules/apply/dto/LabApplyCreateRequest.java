package com.ems.modules.apply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record LabApplyCreateRequest(
        @NotNull Long applicantId,
        @NotBlank String labName,
        @NotBlank String labType,
        @NotBlank String reason,
        @NotNull LocalDate openDateStart,
        @NotNull LocalDate openDateEnd,
        @NotNull LocalTime dailyStartTime,
        @NotNull LocalTime dailyEndTime,
        String slotPreset
) {
}

