package com.ems.modules.repair.dto;

import java.time.LocalDateTime;

public record RepairResponse(
        Long id,
        String requestNo,
        Long reporterId,
        String resourceType,
        Long resourceId,
        String faultDesc,
        String urgencyLevel,
        String status,
        Long confirmerId,
        LocalDateTime confirmedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
}

