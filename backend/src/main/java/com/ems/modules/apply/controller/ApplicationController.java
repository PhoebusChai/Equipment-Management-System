package com.ems.modules.apply.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.apply.dto.*;
import com.ems.modules.apply.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ApiResponse<List<ApplicationResponse>> list() {
        return ApiResponse.ok(applicationService.listAll());
    }

    @PostMapping("/lab")
    public ApiResponse<ApplicationResponse> createLab(@Valid @RequestBody LabApplyCreateRequest request) {
        return ApiResponse.ok("提交成功", applicationService.createLabApply(request));
    }

    @PostMapping("/device")
    public ApiResponse<ApplicationResponse> createDevice(@Valid @RequestBody DeviceApplyCreateRequest request) {
        return ApiResponse.ok("提交成功", applicationService.createDeviceApply(request));
    }

    @PostMapping("/scrap")
    public ApiResponse<ApplicationResponse> createScrap(@Valid @RequestBody ScrapApplyCreateRequest request) {
        return ApiResponse.ok("提交成功", applicationService.createScrapApply(request));
    }

    @PatchMapping("/{type}/{id}/approve")
    public ApiResponse<ApplicationResponse> approve(
            @PathVariable String type,
            @PathVariable Long id,
            @Valid @RequestBody ApplyAuditRequest request
    ) {
        return ApiResponse.ok("审批通过", applicationService.approve(type, id, request));
    }

    @PatchMapping("/{type}/{id}/reject")
    public ApiResponse<ApplicationResponse> reject(
            @PathVariable String type,
            @PathVariable Long id,
            @Valid @RequestBody ApplyAuditRequest request
    ) {
        return ApiResponse.ok("审批驳回", applicationService.reject(type, id, request));
    }
}

