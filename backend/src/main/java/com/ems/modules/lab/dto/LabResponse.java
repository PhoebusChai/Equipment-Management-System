package com.ems.modules.lab.dto;

import java.util.List;

public record LabResponse(
        Long id,
        String labCode,
        String name,
        String type,
        String building,
        String college,
        Integer capacity,
        String status,
        String description,
        List<String> imageUrls
) {
}

