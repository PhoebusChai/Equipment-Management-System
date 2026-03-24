package com.ems.modules.lab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LabCreateRequest(
        @NotBlank @Size(max = 64) String labCode,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Pattern(regexp = "COMPUTER|BIOLOGY") String type,
        @NotBlank @Size(max = 64) String building,
        @Size(max = 128) String college,
        @Min(1) @Max(10000) Integer capacity,
        @Size(max = 2000) String description
) {
}

