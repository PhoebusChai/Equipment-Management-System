package com.ems.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserStatusUpdateRequest(
        @NotBlank String status
) {
}

