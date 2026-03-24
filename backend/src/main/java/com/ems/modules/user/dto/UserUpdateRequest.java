package com.ems.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String realName,
        @NotBlank String role
) {
}

