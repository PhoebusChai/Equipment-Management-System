package com.ems.modules.apply.service;

import com.ems.modules.apply.dto.*;
import com.ems.modules.apply.entity.DeviceApplyFormEntity;
import com.ems.modules.apply.entity.LabApplyFormEntity;
import com.ems.modules.apply.entity.ScrapApplyFormEntity;
import com.ems.modules.apply.repository.DeviceApplyFormRepository;
import com.ems.modules.apply.repository.LabApplyFormRepository;
import com.ems.modules.apply.repository.ScrapApplyFormRepository;
import com.ems.modules.common.enums.ApplyStatus;
import com.ems.modules.lab.service.LabService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ApplicationService {

    private final LabApplyFormRepository labApplyFormRepository;
    private final DeviceApplyFormRepository deviceApplyFormRepository;
    private final ScrapApplyFormRepository scrapApplyFormRepository;
    private final LabService labService;

    public ApplicationService(
            LabApplyFormRepository labApplyFormRepository,
            DeviceApplyFormRepository deviceApplyFormRepository,
            ScrapApplyFormRepository scrapApplyFormRepository,
            LabService labService
    ) {
        this.labApplyFormRepository = labApplyFormRepository;
        this.deviceApplyFormRepository = deviceApplyFormRepository;
        this.scrapApplyFormRepository = scrapApplyFormRepository;
        this.labService = labService;
    }

    public List<ApplicationResponse> listAll() {
        List<ApplicationResponse> all = new ArrayList<>();
        all.addAll(labApplyFormRepository.findAll().stream().map(this::toLabApplyResponse).toList());
        all.addAll(deviceApplyFormRepository.findAll().stream().map(this::toDeviceApplyResponse).toList());
        all.addAll(scrapApplyFormRepository.findAll().stream().map(this::toScrapApplyResponse).toList());
        all.sort(Comparator.comparing(ApplicationResponse::id).reversed());
        return all;
    }

    @Transactional
    public ApplicationResponse createLabApply(LabApplyCreateRequest request) {
        LabApplyFormEntity e = new LabApplyFormEntity();
        e.setApplicantId(request.applicantId());
        e.setLabName(request.labName());
        e.setLabType(request.labType().toUpperCase());
        e.setReason(request.reason());
        e.setOpenDateStart(request.openDateStart());
        e.setOpenDateEnd(request.openDateEnd());
        e.setDailyStartTime(request.dailyStartTime());
        e.setDailyEndTime(request.dailyEndTime());
        e.setSlotPreset(request.slotPreset() == null ? null : request.slotPreset().trim());
        e.setStatus(ApplyStatus.SUBMITTED.name());
        return toLabApplyResponse(labApplyFormRepository.save(e));
    }

    @Transactional
    public ApplicationResponse createDeviceApply(DeviceApplyCreateRequest request) {
        DeviceApplyFormEntity e = new DeviceApplyFormEntity();
        e.setApplicantId(request.applicantId());
        e.setDeviceName(request.deviceName());
        e.setCategory(request.category());
        e.setQuantity(request.quantity());
        e.setExpectedBudget(request.expectedBudget());
        e.setReason(request.reason());
        e.setStatus(ApplyStatus.SUBMITTED.name());
        return toDeviceApplyResponse(deviceApplyFormRepository.save(e));
    }

    @Transactional
    public ApplicationResponse createScrapApply(ScrapApplyCreateRequest request) {
        ScrapApplyFormEntity e = new ScrapApplyFormEntity();
        e.setDeviceId(request.deviceId());
        e.setApplicantId(request.applicantId());
        e.setReason(request.reason());
        e.setStatus(ApplyStatus.SUBMITTED.name());
        return toScrapApplyResponse(scrapApplyFormRepository.save(e));
    }

    @Transactional
    public ApplicationResponse approve(String type, Long id, ApplyAuditRequest request) {
        return audit(type, id, request, ApplyStatus.APPROVED.name());
    }

    @Transactional
    public ApplicationResponse reject(String type, Long id, ApplyAuditRequest request) {
        return audit(type, id, request, ApplyStatus.REJECTED.name());
    }

    private ApplicationResponse audit(String type, Long id, ApplyAuditRequest request, String targetStatus) {
        String normalized = type == null ? "" : type.toLowerCase();
        if ("lab".equals(normalized)) {
            LabApplyFormEntity e = labApplyFormRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("实验室申请不存在"));
            e.setStatus(targetStatus);
            e.setReviewerId(request.reviewerId());
            e.setReviewedAt(LocalDateTime.now());
            e.setReviewComment(request.reviewComment());
            LabApplyFormEntity saved = labApplyFormRepository.save(e);
            if (ApplyStatus.APPROVED.name().equals(targetStatus)) {
                Long labId = extractLabIdFromReason(saved.getReason());
                if (labId != null) {
                    labService.updateStatusById(labId, "IN_USE");
                } else {
                    labService.markInUseByName(saved.getLabName());
                }
            }
            return toLabApplyResponse(saved);
        }
        if ("device".equals(normalized)) {
            DeviceApplyFormEntity e = deviceApplyFormRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("设备申请不存在"));
            e.setStatus(targetStatus);
            e.setReviewerId(request.reviewerId());
            e.setReviewedAt(LocalDateTime.now());
            e.setReviewComment(request.reviewComment());
            return toDeviceApplyResponse(deviceApplyFormRepository.save(e));
        }
        if ("scrap".equals(normalized)) {
            ScrapApplyFormEntity e = scrapApplyFormRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("报废申请不存在"));
            e.setStatus(targetStatus);
            e.setReviewerId(request.reviewerId());
            e.setReviewedAt(LocalDateTime.now());
            e.setReviewComment(request.reviewComment());
            return toScrapApplyResponse(scrapApplyFormRepository.save(e));
        }
        throw new IllegalArgumentException("type 仅支持 lab/device/scrap");
    }

    private Long extractLabIdFromReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String[] lines = reason.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim().toLowerCase();
            if (trimmed.startsWith("labid:") || trimmed.startsWith("labid：")) {
                String idText = trimmed.replace("labid:", "").replace("labid：", "").trim();
                try {
                    return Long.parseLong(idText);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private ApplicationResponse toLabApplyResponse(LabApplyFormEntity e) {
        return new ApplicationResponse(
                "LAB_APPLY",
                e.getId(),
                e.getApplicantId(),
                e.getLabName(),
                e.getReason(),
                e.getOpenDateStart(),
                e.getOpenDateEnd(),
                e.getDailyStartTime(),
                e.getDailyEndTime(),
                e.getSlotPreset(),
                e.getStatus(),
                e.getReviewerId(),
                e.getReviewedAt(),
                e.getReviewComment(),
                e.getCreatedAt()
        );
    }

    private ApplicationResponse toDeviceApplyResponse(DeviceApplyFormEntity e) {
        return new ApplicationResponse(
                "DEVICE_APPLY",
                e.getId(),
                e.getApplicantId(),
                e.getDeviceName(),
                e.getReason(),
                null,
                null,
                null,
                null,
                null,
                e.getStatus(),
                e.getReviewerId(),
                e.getReviewedAt(),
                e.getReviewComment(),
                null
        );
    }

    private ApplicationResponse toScrapApplyResponse(ScrapApplyFormEntity e) {
        return new ApplicationResponse(
                "SCRAP_APPLY",
                e.getId(),
                e.getApplicantId(),
                "设备ID:" + e.getDeviceId(),
                e.getReason(),
                null,
                null,
                null,
                null,
                null,
                e.getStatus(),
                e.getReviewerId(),
                e.getReviewedAt(),
                e.getReviewComment(),
                null
        );
    }
}

