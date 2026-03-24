package com.ems.modules.device.service;

import com.ems.modules.device.dto.DeviceResponse;
import com.ems.modules.device.entity.DeviceEntity;
import com.ems.modules.device.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<DeviceResponse> listAll(Long labId) {
        List<DeviceEntity> list = labId == null ? deviceRepository.findAll() : deviceRepository.findByLabId(labId);
        return list.stream().map(this::toResponse).toList();
    }

    public DeviceResponse getById(Long id) {
        DeviceEntity entity = deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        return toResponse(entity);
    }

    public boolean existsById(Long id) {
        return deviceRepository.existsById(id);
    }

    private DeviceResponse toResponse(DeviceEntity e) {
        return new DeviceResponse(
                e.getId(),
                e.getLabId(),
                e.getDeviceCode(),
                e.getDeviceName(),
                e.getCategory(),
                e.getLocation(),
                e.getStatus()
        );
    }
}

