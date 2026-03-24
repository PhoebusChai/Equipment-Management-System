package com.ems.modules.repair.dto;

import jakarta.validation.constraints.NotNull;

public record RepairAuditRequest(
        @NotNull Long confirmerId
) {
}

