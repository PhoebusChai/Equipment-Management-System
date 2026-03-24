package com.ems.modules.apply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScrapApplyCreateRequest(
        @NotNull Long deviceId,
        @NotNull Long applicantId,
        @NotBlank String reason
) {
}

