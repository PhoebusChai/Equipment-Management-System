package com.ems.modules.lab.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.lab.dto.LabCreateRequest;
import com.ems.modules.lab.dto.LabCreateWithImagesRequest;
import com.ems.modules.lab.dto.LabResponse;
import com.ems.modules.lab.service.LabService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.Collections;

import java.util.List;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping
    public ApiResponse<List<LabResponse>> list() {
        return ApiResponse.ok(labService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<LabResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(labService.getById(id));
    }

    @PostMapping
    public ApiResponse<LabResponse> create(@Valid @RequestBody LabCreateRequest request) {
        return ApiResponse.ok("创建成功", labService.create(request));
    }

    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LabResponse> createWithImages(@Valid @ModelAttribute LabCreateWithImagesRequest request) {
        LabCreateRequest create = new LabCreateRequest(
                request.getLabCode(),
                request.getName(),
                request.getType(),
                request.getBuilding(),
                request.getCollege(),
                request.getCapacity(),
                request.getDescription()
        );
        return ApiResponse.ok(
                "创建成功",
                labService.createWithImages(create, request.getImages() == null ? Collections.emptyList() : Arrays.asList(request.getImages()))
        );
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        Resource resource = labService.loadLabImage(filename);
        return ResponseEntity.ok()
                .contentType(labService.detectMediaType(filename))
                .body(resource);
    }
}

