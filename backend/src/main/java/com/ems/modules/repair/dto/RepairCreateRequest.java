package com.ems.modules.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RepairCreateRequest(
        @NotNull Long reporterId,
        @NotBlank String resourceType,
        @NotNull Long resourceId,
        @NotBlank String faultDesc,
        @NotBlank String urgencyLevel
) {
}

