package com.ems.modules.apply.dto;

import jakarta.validation.constraints.NotNull;

public record ApplyAuditRequest(
        @NotNull Long reviewerId,
        String reviewComment
) {
}

