package com.ems.modules.repair.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.repair.dto.RepairAuditRequest;
import com.ems.modules.repair.dto.RepairCreateRequest;
import com.ems.modules.repair.dto.RepairResponse;
import com.ems.modules.repair.service.RepairService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repairs")
public class RepairController {

    private final RepairService repairService;

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @GetMapping
    public ApiResponse<List<RepairResponse>> list(@RequestParam(required = false) Long handlerId) {
        return ApiResponse.ok(repairService.list(handlerId));
    }

    @PostMapping
    public ApiResponse<RepairResponse> create(@Valid @RequestBody RepairCreateRequest request) {
        return ApiResponse.ok("创建成功", repairService.create(request));
    }

    @PatchMapping("/{id}/confirm")
    public ApiResponse<RepairResponse> confirm(@PathVariable Long id, @Valid @RequestBody RepairAuditRequest request) {
        return ApiResponse.ok("已确认", repairService.confirm(id, request));
    }

    @PatchMapping("/{id}/start")
    public ApiResponse<RepairResponse> start(@PathVariable Long id, @Valid @RequestBody RepairAuditRequest request) {
        return ApiResponse.ok("维修中", repairService.start(id, request));
    }

    @PatchMapping("/{id}/finish")
    public ApiResponse<RepairResponse> finish(@PathVariable Long id, @Valid @RequestBody RepairAuditRequest request) {
        return ApiResponse.ok("已完成", repairService.finish(id, request));
    }
}

