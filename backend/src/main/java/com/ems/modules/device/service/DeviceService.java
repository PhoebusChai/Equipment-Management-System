package com.ems.modules.device.service;

import com.ems.modules.device.dto.DeviceCreateRequest;
import com.ems.modules.device.dto.DeviceResponse;
import com.ems.modules.device.entity.DeviceEntity;
import com.ems.modules.device.repository.DeviceRepository;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public DeviceResponse create(DeviceCreateRequest request) {
        String code = request.deviceCode().trim().toUpperCase();
        if (deviceRepository.existsByDeviceCode(code)) {
            throw new IllegalArgumentException("设备编码已存在");
        }
        DeviceEntity entity = new DeviceEntity();
        entity.setLabId(request.labId());
        entity.setDeviceCode(code);
        entity.setDeviceName(request.name().trim());
        entity.setCategory(request.category() == null ? "" : request.category().trim());
        entity.setLocation(request.location() == null ? "" : request.location().trim());
        entity.setStatus(request.status() == null ? "AVAILABLE" : request.status().trim().toUpperCase());
        return toResponse(deviceRepository.save(entity));
    }

    public boolean existsById(Long id) {
        return deviceRepository.existsById(id);
    }

    @Transactional
    public void updateStatusById(Long id, String status) {
        DeviceEntity entity = deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        entity.setStatus(status == null ? "AVAILABLE" : status.toUpperCase());
        deviceRepository.save(entity);
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

