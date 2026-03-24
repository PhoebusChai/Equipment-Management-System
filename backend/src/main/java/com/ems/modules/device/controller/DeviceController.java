package com.ems.modules.device.controller;

import com.ems.common.api.ApiResponse;
import com.ems.modules.device.dto.DeviceResponse;
import com.ems.modules.device.service.DeviceService;
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
}

