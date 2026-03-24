package com.ems.modules.repair.service;

import com.ems.modules.common.enums.RepairStatus;
import com.ems.modules.repair.dto.RepairAuditRequest;
import com.ems.modules.repair.dto.RepairCreateRequest;
import com.ems.modules.repair.dto.RepairResponse;
import com.ems.modules.repair.entity.RepairRequestEntity;
import com.ems.modules.repair.repository.RepairRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class RepairService {

    private final RepairRequestRepository repairRequestRepository;

    public RepairService(RepairRequestRepository repairRequestRepository) {
        this.repairRequestRepository = repairRequestRepository;
    }

    public List<RepairResponse> list(Long handlerId) {
        List<RepairRequestEntity> list = handlerId == null
                ? repairRequestRepository.findAll()
                : repairRequestRepository.findByConfirmerIdOrderByCreatedAtDesc(handlerId);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public RepairResponse create(RepairCreateRequest request) {
        RepairRequestEntity e = new RepairRequestEntity();
        e.setRequestNo(generateRequestNo());
        e.setReporterId(request.reporterId());
        e.setResourceType(request.resourceType().toUpperCase());
        e.setResourceId(request.resourceId());
        e.setFaultDesc(request.faultDesc());
        e.setUrgencyLevel(request.urgencyLevel().toUpperCase());
        e.setStatus(RepairStatus.SUBMITTED.name());
        return toResponse(repairRequestRepository.save(e));
    }

    @Transactional
    public RepairResponse confirm(Long id, RepairAuditRequest request) {
        RepairRequestEntity e = getById(id);
        if (!RepairStatus.SUBMITTED.name().equals(e.getStatus())) {
            throw new IllegalArgumentException("仅待确认工单可执行确认");
        }
        e.setStatus(RepairStatus.CONFIRMED.name());
        e.setConfirmerId(request.confirmerId());
        e.setConfirmedAt(LocalDateTime.now());
        return toResponse(repairRequestRepository.save(e));
    }

    @Transactional
    public RepairResponse start(Long id, RepairAuditRequest request) {
        RepairRequestEntity e = getById(id);
        if (!RepairStatus.CONFIRMED.name().equals(e.getStatus())) {
            throw new IllegalArgumentException("仅已确认工单可开始维修");
        }
        e.setStatus(RepairStatus.IN_REPAIR.name());
        e.setConfirmerId(request.confirmerId());
        return toResponse(repairRequestRepository.save(e));
    }

    @Transactional
    public RepairResponse finish(Long id, RepairAuditRequest request) {
        RepairRequestEntity e = getById(id);
        if (!RepairStatus.IN_REPAIR.name().equals(e.getStatus())) {
            throw new IllegalArgumentException("仅维修中的工单可完成");
        }
        e.setStatus(RepairStatus.FINISHED.name());
        e.setConfirmerId(request.confirmerId());
        e.setFinishedAt(LocalDateTime.now());
        return toResponse(repairRequestRepository.save(e));
    }

    private RepairRequestEntity getById(Long id) {
        return repairRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("工单不存在"));
    }

    private RepairResponse toResponse(RepairRequestEntity e) {
        return new RepairResponse(
                e.getId(),
                e.getRequestNo(),
                e.getReporterId(),
                e.getResourceType(),
                e.getResourceId(),
                e.getFaultDesc(),
                e.getUrgencyLevel(),
                e.getStatus(),
                e.getConfirmerId(),
                e.getConfirmedAt(),
                e.getFinishedAt(),
                e.getCreatedAt()
        );
    }

    private String generateRequestNo() {
        return "RP" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}

