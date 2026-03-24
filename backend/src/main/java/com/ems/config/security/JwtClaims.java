package com.ems.config.security;

public record JwtClaims(
        Long userId,
        String roleCode,
        String email
) {
}

