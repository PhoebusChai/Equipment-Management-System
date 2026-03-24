package com.ems.modules.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookingId,
        String resourceType,
        Long resourceId,
        String resourceName,
        Long userId,
        String userName,
        Integer rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
