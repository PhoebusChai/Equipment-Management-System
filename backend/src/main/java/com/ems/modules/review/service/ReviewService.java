package com.ems.modules.review.service;

import com.ems.modules.apply.entity.LabApplyFormEntity;
import com.ems.modules.apply.repository.LabApplyFormRepository;
import com.ems.modules.booking.entity.BookingEntity;
import com.ems.modules.booking.repository.BookingRepository;
import com.ems.modules.device.entity.DeviceEntity;
import com.ems.modules.device.repository.DeviceRepository;
import com.ems.modules.lab.entity.LabEntity;
import com.ems.modules.lab.repository.LabRepository;
import com.ems.modules.review.dto.ReviewResponse;
import com.ems.modules.review.dto.ReviewUpsertRequest;
import com.ems.modules.review.entity.DeviceReviewEntity;
import com.ems.modules.review.entity.LabReviewEntity;
import com.ems.modules.review.repository.DeviceReviewRepository;
import com.ems.modules.review.repository.LabReviewRepository;
import com.ems.modules.user.entity.UserEntity;
import com.ems.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReviewService {

    private final BookingRepository bookingRepository;
    private final LabReviewRepository labReviewRepository;
    private final DeviceReviewRepository deviceReviewRepository;
    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final DeviceRepository deviceRepository;
    private final LabApplyFormRepository labApplyFormRepository;

    public ReviewService(
            BookingRepository bookingRepository,
            LabReviewRepository labReviewRepository,
            DeviceReviewRepository deviceReviewRepository,
            UserRepository userRepository,
            LabRepository labRepository,
            DeviceRepository deviceRepository,
            LabApplyFormRepository labApplyFormRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.labReviewRepository = labReviewRepository;
        this.deviceReviewRepository = deviceReviewRepository;
        this.userRepository = userRepository;
        this.labRepository = labRepository;
        this.deviceRepository = deviceRepository;
        this.labApplyFormRepository = labApplyFormRepository;
    }

    @Transactional
    public ReviewResponse upsert(ReviewUpsertRequest request) {
        BookingEntity booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("预约不存在"));
        if (!booking.getUserId().equals(request.userId())) {
            throw new IllegalArgumentException("仅可评价自己的预约");
        }
        if (!"FINISHED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("仅已完成预约可评价");
        }

        String type = booking.getResourceType() == null ? "" : booking.getResourceType().toUpperCase();
        if ("LAB".equals(type)) {
            LabReviewEntity e = labReviewRepository.findByBookingId(booking.getId()).orElseGet(LabReviewEntity::new);
            e.setBookingId(booking.getId());
            e.setLabId(booking.getResourceId());
            e.setUserId(request.userId());
            e.setRating(request.rating());
            e.setContent(request.content() == null ? "" : request.content().trim());
            LabReviewEntity saved = labReviewRepository.save(e);
            return toLabResponse(saved);
        }
        if ("DEVICE".equals(type)) {
            DeviceReviewEntity e = deviceReviewRepository.findByBookingId(booking.getId()).orElseGet(DeviceReviewEntity::new);
            e.setBookingId(booking.getId());
            e.setDeviceId(booking.getResourceId());
            e.setUserId(request.userId());
            e.setRating(request.rating());
            e.setContent(request.content() == null ? "" : request.content().trim());
            DeviceReviewEntity saved = deviceReviewRepository.save(e);
            return toDeviceResponse(saved);
        }
        throw new IllegalArgumentException("仅支持实验室或设备预约评价");
    }

    public List<ReviewResponse> listByResource(String resourceType, Long resourceId) {
        String type = resourceType == null ? "" : resourceType.toUpperCase();
        if ("LAB".equals(type)) {
            return labReviewRepository.findByLabIdOrderByCreatedAtDesc(resourceId).stream().map(this::toLabResponse).toList();
        }
        if ("DEVICE".equals(type)) {
            return deviceReviewRepository.findByDeviceIdOrderByCreatedAtDesc(resourceId).stream().map(this::toDeviceResponse).toList();
        }
        throw new IllegalArgumentException("resourceType 仅支持 LAB 或 DEVICE");
    }

    public List<ReviewResponse> listTeacherReviews(Long teacherId, String resourceType) {
        if (teacherId == null) throw new IllegalArgumentException("teacherId 不能为空");
        Set<Long> managedLabIds = resolveManagedLabIdsByTeacher(teacherId);
        String type = resourceType == null ? "ALL" : resourceType.toUpperCase();
        List<ReviewResponse> result = new ArrayList<>();
        if (!managedLabIds.isEmpty() && ("ALL".equals(type) || "LAB".equals(type))) {
            result.addAll(labReviewRepository.findByLabIdInOrderByCreatedAtDesc(new ArrayList<>(managedLabIds)).stream().map(this::toLabResponse).toList());
        }
        if (!managedLabIds.isEmpty() && ("ALL".equals(type) || "DEVICE".equals(type))) {
            List<DeviceEntity> devices = deviceRepository.findByLabIdIn(new ArrayList<>(managedLabIds));
            List<Long> ids = devices.stream().map(DeviceEntity::getId).toList();
            if (!ids.isEmpty()) {
                result.addAll(deviceReviewRepository.findByDeviceIdInOrderByCreatedAtDesc(ids).stream().map(this::toDeviceResponse).toList());
            }
        }
        return result.stream().sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).toList();
    }

    public List<ReviewResponse> listAdminReviews(String resourceType, Integer rating) {
        String type = resourceType == null ? "ALL" : resourceType.toUpperCase();
        List<ReviewResponse> result = new ArrayList<>();
        if ("ALL".equals(type) || "LAB".equals(type)) {
            result.addAll(labReviewRepository.findAll().stream().map(this::toLabResponse).toList());
        }
        if ("ALL".equals(type) || "DEVICE".equals(type)) {
            result.addAll(deviceReviewRepository.findAll().stream().map(this::toDeviceResponse).toList());
        }
        return result.stream()
                .filter(x -> rating == null || x.rating().equals(rating))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    public ReviewResponse getByBookingId(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return null;
        String type = booking.getResourceType() == null ? "" : booking.getResourceType().toUpperCase();
        if ("LAB".equals(type)) return labReviewRepository.findByBookingId(bookingId).map(this::toLabResponse).orElse(null);
        if ("DEVICE".equals(type)) return deviceReviewRepository.findByBookingId(bookingId).map(this::toDeviceResponse).orElse(null);
        return null;
    }

    private Set<Long> resolveManagedLabIdsByTeacher(Long teacherId) {
        Set<Long> ids = new HashSet<>();
        List<LabApplyFormEntity> approved = labApplyFormRepository.findByStatusAndApplicantId("APPROVED", teacherId);
        for (LabApplyFormEntity x : approved) {
            Long id = extractLabIdFromReason(x.getReason());
            if (id != null) {
                ids.add(id);
                continue;
            }
            LabEntity lab = labRepository.findFirstByLabNameIgnoreCase(x.getLabName());
            if (lab != null) ids.add(lab.getId());
        }
        return ids;
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

    private ReviewResponse toLabResponse(LabReviewEntity e) {
        LabEntity lab = labRepository.findById(e.getLabId()).orElse(null);
        UserEntity user = userRepository.findById(e.getUserId()).orElse(null);
        return new ReviewResponse(
                e.getId(),
                e.getBookingId(),
                "LAB",
                e.getLabId(),
                lab == null ? ("实验室#" + e.getLabId()) : lab.getLabName(),
                e.getUserId(),
                user == null ? ("用户#" + e.getUserId()) : user.getRealName(),
                e.getRating(),
                e.getContent(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private ReviewResponse toDeviceResponse(DeviceReviewEntity e) {
        DeviceEntity device = deviceRepository.findById(e.getDeviceId()).orElse(null);
        UserEntity user = userRepository.findById(e.getUserId()).orElse(null);
        return new ReviewResponse(
                e.getId(),
                e.getBookingId(),
                "DEVICE",
                e.getDeviceId(),
                device == null ? ("设备#" + e.getDeviceId()) : device.getDeviceName(),
                e.getUserId(),
                user == null ? ("用户#" + e.getUserId()) : user.getRealName(),
                e.getRating(),
                e.getContent(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
