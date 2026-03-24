package com.ems.modules.auth.dto;

public record LoginResponse(
        Long userId,
        String email,
        String roleCode,
        String token
) {
}

