package com.ems.modules.apply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceApplyCreateRequest(
        @NotNull Long applicantId,
        @NotBlank String deviceName,
        String category,
        @NotNull Integer quantity,
        Double expectedBudget,
        @NotBlank String reason
) {
}

