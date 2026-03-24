package com.ems.modules.device.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.device.dto.DeviceCreateRequest;
import com.ems.modules.device.dto.DeviceResponse;
import com.ems.modules.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<List<DeviceResponse>> list(@RequestParam(required = false) Long labId) {
        return ApiResponse.ok(deviceService.listAll(labId));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.getById(id));
    }

    @PostMapping
    public ApiResponse<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.ok("创建成功", deviceService.create(request));
    }
}

