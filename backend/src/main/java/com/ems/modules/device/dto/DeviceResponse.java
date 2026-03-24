package com.ems.modules.device.dto;

public record DeviceResponse(
        Long id,
        Long labId,
        String deviceCode,
        String name,
        String category,
        String location,
        String status
) {
}

