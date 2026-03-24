package com.ems.modules.booking.service;

import com.ems.modules.booking.dto.*;
import com.ems.modules.booking.entity.BookingEntity;
import com.ems.modules.booking.repository.BookingRepository;
import com.ems.modules.apply.entity.LabApplyFormEntity;
import com.ems.modules.apply.repository.LabApplyFormRepository;
import com.ems.modules.common.enums.BookingStatus;
import com.ems.modules.device.service.DeviceService;
import com.ems.modules.lab.service.LabService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final LabService labService;
    private final DeviceService deviceService;
    private final LabApplyFormRepository labApplyFormRepository;

    public BookingService(
            BookingRepository bookingRepository,
            LabService labService,
            DeviceService deviceService,
            LabApplyFormRepository labApplyFormRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.labService = labService;
        this.deviceService = deviceService;
        this.labApplyFormRepository = labApplyFormRepository;
    }

    public BookingConflictCheckResponse checkConflict(BookingConflictCheckRequest request) {
        autoExpireBookings();
        validateResource(request.resourceType(), request.resourceId());
        validateTimeRange(request.startTime(), request.endTime());
        long count = conflictBookings(request.resourceType(), request.resourceId(), request.startTime(), request.endTime()).size();
        return new BookingConflictCheckResponse(count > 0, count);
    }

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        autoExpireBookings();
        validateResource(request.resourceType(), request.resourceId());
        validateTimeRange(request.startTime(), request.endTime());

        List<BookingEntity> conflicts = conflictBookings(
                request.resourceType(),
                request.resourceId(),
                request.startTime(),
                request.endTime()
        );
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("预约时间冲突，请选择其他时间段");
        }

        BookingEntity entity = new BookingEntity();
        entity.setBookingNo(generateBookingNo());
        entity.setUserId(request.userId());
        entity.setResourceType(request.resourceType().toUpperCase());
        entity.setResourceId(request.resourceId());
        entity.setBookingScope(request.bookingScope() == null ? "" : request.bookingScope().trim());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setDurationMinutes((int) Duration.between(request.startTime(), request.endTime()).toMinutes());
        entity.setStatus(BookingStatus.PENDING.name());

        BookingEntity saved = bookingRepository.save(entity);
        return toResponse(saved);
    }

    public List<BookingResponse> listBookings(Long userId, Long approverId) {
        autoExpireBookings();
        if (userId != null) {
            return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
        }
        if (approverId != null) {
            return bookingRepository.findByApproverIdOrderByCreatedAtDesc(approverId).stream().map(this::toResponse).toList();
        }
        return bookingRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse approve(Long bookingId, BookingAuditRequest request) {
        BookingEntity entity = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!BookingStatus.PENDING.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("仅待审核预约可以批准");
        }
        entity.setStatus(BookingStatus.APPROVED.name());
        entity.setApproverId(request.approverId());
        entity.setRejectReason(null);
        BookingEntity saved = bookingRepository.save(entity);
        syncLabAvailability(saved.getResourceType(), saved.getResourceId());
        // TODO: 后续可将审批结果写入用户通知表（若引入 user_notices）
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse reject(Long bookingId, BookingAuditRequest request) {
        BookingEntity entity = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!BookingStatus.PENDING.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("仅待审核预约可以驳回");
        }
        entity.setStatus(BookingStatus.REJECTED.name());
        entity.setApproverId(request.approverId());
        entity.setRejectReason(request.reason() == null ? "" : request.reason());
        BookingEntity saved = bookingRepository.save(entity);
        syncLabAvailability(saved.getResourceType(), saved.getResourceId());
        // TODO: 后续可将审批结果写入用户通知表（若引入 user_notices）
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse finish(Long bookingId, BookingAuditRequest request) {
        BookingEntity entity = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!BookingStatus.APPROVED.name().equals(entity.getStatus()) && !BookingStatus.IN_USE.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("仅已确认/使用中的预约可以完成");
        }
        entity.setStatus(BookingStatus.FINISHED.name());
        entity.setApproverId(request.approverId());
        BookingEntity saved = bookingRepository.save(entity);
        syncLabAvailability(saved.getResourceType(), saved.getResourceId());
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, BookingCancelRequest request) {
        BookingEntity entity = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!entity.getUserId().equals(request.userId())) {
            throw new IllegalArgumentException("无权限取消该预约");
        }
        if (BookingStatus.FINISHED.name().equals(entity.getStatus()) || BookingStatus.CANCELLED.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("该预约已结束或已取消");
        }
        entity.setStatus(BookingStatus.CANCELLED.name());
        entity.setRejectReason(request.reason() == null ? "用户取消" : request.reason().trim());
        BookingEntity saved = bookingRepository.save(entity);
        syncLabAvailability(saved.getResourceType(), saved.getResourceId());
        return toResponse(saved);
    }

    private void validateResource(String resourceType, Long resourceId) {
        String type = resourceType == null ? "" : resourceType.toUpperCase();
        if ("LAB".equals(type)) {
            if (!labService.existsById(resourceId)) throw new IllegalArgumentException("实验室不存在");
            if (!labService.isAvailable(resourceId)) throw new IllegalArgumentException("实验室当前占用中或不可用，无法预约");
            if (!isLabBookableByApprovedTeacherApply(resourceId)) {
                throw new IllegalArgumentException("该实验室尚未配置教师开放时段，当前不可预约");
            }
            return;
        }
        if ("DEVICE".equals(type)) {
            if (!deviceService.existsById(resourceId)) throw new IllegalArgumentException("设备不存在");
            return;
        }
        throw new IllegalArgumentException("resourceType 仅支持 LAB 或 DEVICE");
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("时间范围不合法");
        }
    }

    private List<BookingEntity> conflictBookings(String resourceType, Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        return bookingRepository.findByResourceTypeAndResourceIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                resourceType.toUpperCase(),
                resourceId,
                List.of(BookingStatus.PENDING.name(), BookingStatus.APPROVED.name(), BookingStatus.IN_USE.name()),
                endTime,
                startTime
        );
    }

    @Transactional
    protected void autoExpireBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<BookingEntity> expired = bookingRepository.findByStatusInAndEndTimeBefore(
                List.of(BookingStatus.PENDING.name(), BookingStatus.APPROVED.name(), BookingStatus.IN_USE.name()),
                now
        );
        for (BookingEntity entity : expired) {
            if (BookingStatus.PENDING.name().equals(entity.getStatus())) {
                entity.setStatus(BookingStatus.CANCELLED.name());
                entity.setRejectReason("已过预约时段，系统自动取消");
            } else {
                entity.setStatus(BookingStatus.FINISHED.name());
            }
            BookingEntity saved = bookingRepository.save(entity);
            syncLabAvailability(saved.getResourceType(), saved.getResourceId());
        }
    }

    private void syncLabAvailability(String resourceType, Long resourceId) {
        if (!"LAB".equalsIgnoreCase(resourceType)) return;
        long activeCount = bookingRepository.countByResourceTypeAndResourceIdAndStatusInAndEndTimeAfter(
                "LAB",
                resourceId,
                List.of(BookingStatus.APPROVED.name(), BookingStatus.IN_USE.name()),
                LocalDateTime.now()
        );
        labService.updateStatusById(resourceId, activeCount > 0 ? "IN_USE" : "AVAILABLE");
    }

    private BookingResponse toResponse(BookingEntity e) {
        return new BookingResponse(
                e.getId(),
                e.getBookingNo(),
                e.getUserId(),
                e.getResourceType(),
                e.getResourceId(),
                resolveResourceName(e.getResourceType(), e.getResourceId()),
                e.getBookingScope(),
                e.getStartTime(),
                e.getEndTime(),
                e.getDurationMinutes(),
                e.getStatus(),
                e.getApproverId(),
                e.getRejectReason(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private String resolveResourceName(String resourceType, Long resourceId) {
        if ("LAB".equalsIgnoreCase(resourceType)) {
            try {
                return labService.getById(resourceId).name();
            } catch (Exception ignored) {
                return "实验室#" + resourceId;
            }
        }
        if ("DEVICE".equalsIgnoreCase(resourceType)) {
            try {
                return deviceService.getById(resourceId).name();
            } catch (Exception ignored) {
                return "设备#" + resourceId;
            }
        }
        return "资源#" + resourceId;
    }

    private boolean isLabBookableByApprovedTeacherApply(Long labId) {
        try {
            String labName = labService.getById(labId).name();
            List<LabApplyFormEntity> approved = labApplyFormRepository.findByStatusAndLabNameIgnoreCase("APPROVED", labName);
            if (approved.isEmpty()) return false;
            LocalDateTime now = LocalDateTime.now();
            return approved.stream().anyMatch((x) -> isNowInApplyOpenWindow(x.getReason(), now));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNowInApplyOpenWindow(String reason, LocalDateTime now) {
        String openDateStart = extractMeta(reason, "开放开始");
        String openDateEnd = extractMeta(reason, "开放结束");
        String dailyStartTime = extractMeta(reason, "每日开始");
        String dailyEndTime = extractMeta(reason, "每日结束");
        if (openDateStart == null || openDateEnd == null || dailyStartTime == null || dailyEndTime == null) return false;
        try {
            var date = now.toLocalDate();
            var startDate = java.time.LocalDate.parse(openDateStart);
            var endDate = java.time.LocalDate.parse(openDateEnd);
            if (date.isBefore(startDate) || date.isAfter(endDate)) return false;
            var time = now.toLocalTime();
            var startTime = java.time.LocalTime.parse(dailyStartTime);
            var endTime = java.time.LocalTime.parse(dailyEndTime);
            return !time.isBefore(startTime) && time.isBefore(endTime);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractMeta(String reason, String key) {
        if (reason == null || reason.isBlank()) return null;
        String prefixCn = key + "：";
        String prefixEn = key + ":";
        String[] lines = reason.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith(prefixCn)) return line.substring(prefixCn.length()).trim();
            if (line.startsWith(prefixEn)) return line.substring(prefixEn.length()).trim();
        }
        return null;
    }

    private String generateBookingNo() {
        return "BK" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}

