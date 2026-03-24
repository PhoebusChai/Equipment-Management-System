package com.ems.modules.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotNull Long labId,
        @NotBlank @Size(max = 64) String deviceCode,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String category,
        @Size(max = 128) String location,
        @Pattern(regexp = "AVAILABLE|IN_USE|MAINTENANCE") String status
) {
}
