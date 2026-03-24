package com.ems.modules.user.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String realName,
        String role,
        String status,
        LocalDateTime createdAt
) {
}

