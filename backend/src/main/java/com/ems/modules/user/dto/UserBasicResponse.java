package com.ems.modules.user.dto;

public record UserBasicResponse(
        Long id,
        String realName,
        String role,
        String status
) {
}
